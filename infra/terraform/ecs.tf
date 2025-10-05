# ECS on Fargate: the Spring Boot API plus the five Next.js zones, each an independently deployable
# service. Service Connect gives every service a stable in-VPC name (backend:8080, catalog:3001, ...),
# the same names docker-compose uses, so the images need no environment-specific build.

locals {
  zones = {
    shell   = 3000
    catalog = 3001
    learn   = 3002
    studio  = 3003
    admin   = 3004
  }
  cookie_secure = var.certificate_arn != "" ? "true" : "false"
}

resource "aws_service_discovery_http_namespace" "this" {
  name = "${var.name}.internal"
}

resource "aws_ecs_cluster" "this" {
  name = var.name

  setting {
    name  = "containerInsights"
    value = "enabled"
  }

  service_connect_defaults {
    namespace = aws_service_discovery_http_namespace.this.arn
  }
}

resource "aws_cloudwatch_log_group" "svc" {
  for_each          = toset(local.images)
  name              = "/ecs/${var.name}/${each.key}"
  retention_in_days = 30
}

# ---------- IAM ----------
data "aws_iam_policy_document" "ecs_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "execution" {
  name               = "${var.name}-ecs-execution"
  assume_role_policy = data.aws_iam_policy_document.ecs_assume.json
}

resource "aws_iam_role_policy_attachment" "execution" {
  role       = aws_iam_role.execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

data "aws_iam_policy_document" "read_secrets" {
  statement {
    actions   = ["secretsmanager:GetSecretValue"]
    resources = [aws_secretsmanager_secret.jwt.arn, aws_db_instance.mysql.master_user_secret[0].secret_arn]
  }
}

resource "aws_iam_role_policy" "read_secrets" {
  name   = "read-app-secrets"
  role   = aws_iam_role.execution.id
  policy = data.aws_iam_policy_document.read_secrets.json
}

resource "aws_iam_role" "task" {
  name               = "${var.name}-ecs-task"
  assume_role_policy = data.aws_iam_policy_document.ecs_assume.json
}

# ---------- API ----------
resource "aws_ecs_task_definition" "backend" {
  family                   = "${var.name}-backend"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 512
  memory                   = 1024
  execution_role_arn       = aws_iam_role.execution.arn
  task_role_arn            = aws_iam_role.task.arn

  container_definitions = jsonencode([{
    name         = "backend"
    image        = "${aws_ecr_repository.this["backend"].repository_url}:${var.image_tag}"
    essential    = true
    portMappings = [{ name = "http", containerPort = 8080, appProtocol = "http" }]
    environment = [
      { name = "DB_URL", value = "jdbc:mysql://${aws_db_instance.mysql.address}:3306/ocs?sslMode=REQUIRED&connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true" },
      { name = "DB_USER", value = "ocs" },
      { name = "OCS_REDIS_ENABLED", value = "true" },
      { name = "REDIS_HOST", value = aws_elasticache_replication_group.redis.primary_endpoint_address },
      { name = "REDIS_SSL", value = "true" },
      { name = "OCS_COOKIE_SECURE", value = local.cookie_secure },
      { name = "OCS_CORS_ORIGINS", value = local.site_url },
      { name = "FORWARD_HEADERS_STRATEGY", value = "framework" },
      # Browser traffic arrives via the shell proxy and direct API clients via API Gateway, which
      # enforces the limits; a per-IP limiter here would only see proxy addresses.
      { name = "OCS_RATE_LIMIT_ENABLED", value = "false" },
      { name = "LOGGING_STRUCTURED_FORMAT_CONSOLE", value = "ecs" },
    ]
    secrets = [
      { name = "DB_PASSWORD", valueFrom = "${aws_db_instance.mysql.master_user_secret[0].secret_arn}:password::" },
      { name = "OCS_JWT_SECRET", valueFrom = aws_secretsmanager_secret.jwt.arn },
    ]
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        awslogs-group         = aws_cloudwatch_log_group.svc["backend"].name
        awslogs-region        = var.region
        awslogs-stream-prefix = "backend"
      }
    }
  }])
}

resource "aws_ecs_service" "backend" {
  name            = "backend"
  cluster         = aws_ecs_cluster.this.id
  task_definition = aws_ecs_task_definition.backend.arn
  desired_count   = var.backend_desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets         = module.vpc.private_subnets
    security_groups = [aws_security_group.app.id]
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.backend.arn
    container_name   = "backend"
    container_port   = 8080
  }

  service_connect_configuration {
    enabled = true
    service {
      port_name      = "http"
      discovery_name = "backend"
      client_alias {
        port     = 8080
        dns_name = "backend"
      }
    }
  }

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  lifecycle {
    ignore_changes = [desired_count] # owned by autoscaling after creation
  }

  depends_on = [aws_lb_listener.http]
}

resource "aws_appautoscaling_target" "backend" {
  service_namespace  = "ecs"
  resource_id        = "service/${aws_ecs_cluster.this.name}/${aws_ecs_service.backend.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  min_capacity       = var.backend_desired_count
  max_capacity       = var.backend_max_count
}

resource "aws_appautoscaling_policy" "backend_cpu" {
  name               = "${var.name}-backend-cpu"
  policy_type        = "TargetTrackingScaling"
  service_namespace  = aws_appautoscaling_target.backend.service_namespace
  resource_id        = aws_appautoscaling_target.backend.resource_id
  scalable_dimension = aws_appautoscaling_target.backend.scalable_dimension

  target_tracking_scaling_policy_configuration {
    target_value = 60
    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }
  }
}

# ---------- web zones ----------
resource "aws_ecs_task_definition" "zone" {
  for_each                 = local.zones
  family                   = "${var.name}-${each.key}"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 256
  memory                   = 512
  execution_role_arn       = aws_iam_role.execution.arn
  task_role_arn            = aws_iam_role.task.arn

  container_definitions = jsonencode([{
    name         = each.key
    image        = "${aws_ecr_repository.this[each.key].repository_url}:${var.image_tag}"
    essential    = true
    portMappings = [{ name = "http", containerPort = each.value, appProtocol = "http" }]
    environment = [
      { name = "PORT", value = tostring(each.value) },
      { name = "BACKEND_URL", value = "http://backend:8080" },
      { name = "SITE_URL", value = local.site_url },
      { name = "OCS_COOKIE_SECURE", value = local.cookie_secure },
      { name = "CATALOG_URL", value = "http://catalog:3001" },
      { name = "LEARN_URL", value = "http://learn:3002" },
      { name = "STUDIO_URL", value = "http://studio:3003" },
      { name = "ADMIN_URL", value = "http://admin:3004" },
    ]
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        awslogs-group         = aws_cloudwatch_log_group.svc[each.key].name
        awslogs-region        = var.region
        awslogs-stream-prefix = each.key
      }
    }
  }])
}

resource "aws_ecs_service" "zone" {
  for_each        = local.zones
  name            = each.key
  cluster         = aws_ecs_cluster.this.id
  task_definition = aws_ecs_task_definition.zone[each.key].arn
  desired_count   = each.key == "shell" || each.key == "catalog" ? 2 : 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets         = module.vpc.private_subnets
    security_groups = [aws_security_group.app.id]
  }

  # Only the shell is public; the other zones are reached through it.
  dynamic "load_balancer" {
    for_each = each.key == "shell" ? [1] : []
    content {
      target_group_arn = aws_lb_target_group.shell.arn
      container_name   = "shell"
      container_port   = 3000
    }
  }

  service_connect_configuration {
    enabled = true
    service {
      port_name      = "http"
      discovery_name = each.key
      client_alias {
        port     = each.value
        dns_name = each.key
      }
    }
  }

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  depends_on = [aws_lb_listener.http, aws_ecs_service.backend]
}
