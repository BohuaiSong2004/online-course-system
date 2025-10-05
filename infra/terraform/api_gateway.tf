# API Gateway (REST, v1) in front of the Spring Boot API.
#
# - /api/{proxy+} is passed through to the backend unchanged.
# - Stage-wide throttling (token bucket: rate + burst) protects the service from traffic spikes;
#   clients over the limit receive HTTP 429 before the request ever reaches the backend.
# - /api/auth/{proxy+} gets a much stricter limit to slow down credential stuffing.
# - A usage plan with API keys adds per-client throttling and a daily quota for integrations.
#
# The backend's in-process RateLimitFilter mirrors the stage limits for local development.

locals {
  backend_base_url = var.backend_base_url != "" ? var.backend_base_url : "http://${aws_lb.web.dns_name}"
}

resource "aws_api_gateway_rest_api" "api" {
  name        = "${var.name}-api"
  description = "Online Course Management System API"

  endpoint_configuration {
    types = ["REGIONAL"]
  }
}

# ---------- /api ----------
resource "aws_api_gateway_resource" "api" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  parent_id   = aws_api_gateway_rest_api.api.root_resource_id
  path_part   = "api"
}

# ---------- /api/{proxy+} ----------
resource "aws_api_gateway_resource" "proxy" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  parent_id   = aws_api_gateway_resource.api.id
  path_part   = "{proxy+}"
}

resource "aws_api_gateway_method" "proxy_any" {
  rest_api_id      = aws_api_gateway_rest_api.api.id
  resource_id      = aws_api_gateway_resource.proxy.id
  http_method      = "ANY"
  authorization    = "NONE" # JWT is validated by the service itself
  api_key_required = false

  request_parameters = {
    "method.request.path.proxy" = true
  }
}

resource "aws_api_gateway_integration" "proxy_any" {
  rest_api_id             = aws_api_gateway_rest_api.api.id
  resource_id             = aws_api_gateway_resource.proxy.id
  http_method             = aws_api_gateway_method.proxy_any.http_method
  type                    = "HTTP_PROXY"
  integration_http_method = "ANY"
  uri                     = "${local.backend_base_url}/api/{proxy}"
  timeout_milliseconds    = 10000

  request_parameters = {
    "integration.request.path.proxy" = "method.request.path.proxy"
  }
}

# ---------- /api/auth/{proxy+} (stricter limits) ----------
resource "aws_api_gateway_resource" "auth" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  parent_id   = aws_api_gateway_resource.api.id
  path_part   = "auth"
}

resource "aws_api_gateway_resource" "auth_proxy" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  parent_id   = aws_api_gateway_resource.auth.id
  path_part   = "{proxy+}"
}

resource "aws_api_gateway_method" "auth_any" {
  rest_api_id   = aws_api_gateway_rest_api.api.id
  resource_id   = aws_api_gateway_resource.auth_proxy.id
  http_method   = "ANY"
  authorization = "NONE"

  request_parameters = {
    "method.request.path.proxy" = true
  }
}

resource "aws_api_gateway_integration" "auth_any" {
  rest_api_id             = aws_api_gateway_rest_api.api.id
  resource_id             = aws_api_gateway_resource.auth_proxy.id
  http_method             = aws_api_gateway_method.auth_any.http_method
  type                    = "HTTP_PROXY"
  integration_http_method = "ANY"
  uri                     = "${local.backend_base_url}/api/auth/{proxy}"
  timeout_milliseconds    = 10000

  request_parameters = {
    "integration.request.path.proxy" = "method.request.path.proxy"
  }
}

# ---------- deployment & stage ----------
resource "aws_api_gateway_deployment" "this" {
  rest_api_id = aws_api_gateway_rest_api.api.id

  triggers = {
    redeploy = sha1(jsonencode([
      aws_api_gateway_resource.proxy.id,
      aws_api_gateway_method.proxy_any.id,
      aws_api_gateway_integration.proxy_any.id,
      aws_api_gateway_resource.auth_proxy.id,
      aws_api_gateway_method.auth_any.id,
      aws_api_gateway_integration.auth_any.id,
    ]))
  }

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_cloudwatch_log_group" "access" {
  name              = "/aws/apigateway/${var.name}-${var.stage_name}"
  retention_in_days = 30
}

resource "aws_api_gateway_stage" "this" {
  rest_api_id   = aws_api_gateway_rest_api.api.id
  deployment_id = aws_api_gateway_deployment.this.id
  stage_name    = var.stage_name

  access_log_settings {
    destination_arn = aws_cloudwatch_log_group.access.arn
    format = jsonencode({
      requestId = "$context.requestId"
      ip        = "$context.identity.sourceIp"
      method    = "$context.httpMethod"
      path      = "$context.path"
      status    = "$context.status"
      latencyMs = "$context.responseLatency"
    })
  }
}

# Default throttling for every method in the stage.
resource "aws_api_gateway_method_settings" "all" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  stage_name  = aws_api_gateway_stage.this.stage_name
  method_path = "*/*"

  settings {
    metrics_enabled        = true
    throttling_rate_limit  = var.throttle_rate_limit
    throttling_burst_limit = var.throttle_burst_limit
  }
}

# Stricter throttling for authentication endpoints.
resource "aws_api_gateway_method_settings" "auth" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  stage_name  = aws_api_gateway_stage.this.stage_name
  method_path = "api/auth/{proxy+}/ANY"

  settings {
    metrics_enabled        = true
    throttling_rate_limit  = var.auth_rate_limit
    throttling_burst_limit = var.auth_burst_limit
  }
}

# ---------- usage plan for API-key clients ----------
# API Gateway applies a usage plan only to methods with api_key_required = true. Browser traffic stays
# key-less (stage limits above); set api_key_required on any routes reserved for integrations.
resource "aws_api_gateway_usage_plan" "partners" {
  name        = "${var.name}-partners"
  description = "Per-client throttling and daily quota for server-to-server integrations"

  api_stages {
    api_id = aws_api_gateway_rest_api.api.id
    stage  = aws_api_gateway_stage.this.stage_name
  }

  throttle_settings {
    rate_limit  = 10
    burst_limit = 20
  }

  quota_settings {
    limit  = var.partner_quota_per_day
    period = "DAY"
  }
}

resource "aws_api_gateway_api_key" "hris_sync" {
  name    = "${var.name}-hris-sync"
  enabled = true
}

resource "aws_api_gateway_usage_plan_key" "hris_sync" {
  key_id        = aws_api_gateway_api_key.hris_sync.id
  key_type      = "API_KEY"
  usage_plan_id = aws_api_gateway_usage_plan.partners.id
}
