# Deploying to AWS

Everything in `terraform/` is one stack. `terraform validate` and `fmt` run in CI. The stack has **not** been applied to a real AWS account from this repository: that needs your own account and credentials.

## What it creates

```mermaid
flowchart LR
    U[Browser] -->|HTTP/HTTPS| ALB[Application Load Balancer]
    C[API clients] --> GW[API Gateway<br/>throttling · usage plans · API keys]
    GW -->|/api/*| ALB
    ALB -->|default| SH[shell ×2]
    ALB -->|/api/*| BE[backend ×2–6<br/>CPU autoscaling]
    subgraph ECS Fargate · private subnets · Service Connect
      SH --> CA[catalog ×2] & LE[learn] & ST[studio] & AD[admin]
      SH & CA & LE & ST & AD -->|backend:8080| BE
    end
    BE --> RDS[(RDS MySQL 8<br/>encrypted, backups)]
    BE --> R[(ElastiCache Redis<br/>TLS, failover)]
    BE -. secrets .-> SM[Secrets Manager]
```

| File | Resources |
|---|---|
| `network.tf` | VPC across 2 AZs (public, private and database subnets, NAT) and security groups. Data stores accept traffic only from the app security group |
| `data.tf` | RDS MySQL 8 (password generated and stored by RDS in Secrets Manager), ElastiCache Redis replication group (TLS, at-rest encryption, automatic failover), JWT signing secret |
| `ecr.tf` | One image repository per deployable (scan on push, keep 20 images) |
| `alb.tf` | Public ALB: `/api/*` → backend, everything else → shell. Optional ACM certificate turns on HTTPS and an HTTP→HTTPS redirect |
| `ecs.tf` | Fargate cluster, Service Connect namespace, task definitions and services for the API and five zones, CPU target-tracking autoscaling for the API, deployment circuit breaker with rollback, CloudWatch logs, least-privilege IAM |
| `api_gateway.tf` | REST API Gateway in front of the ALB: stage throttling (50 rps, burst 100), stricter `/api/auth/*` limits, usage plan with API key and daily quota |

Service Connect gives every service the same in-VPC hostname it has in `docker-compose.yml` (`backend:8080`, `catalog:3001`, …). The same images run locally and on AWS without environment-specific builds.

Two API instances run by default. That works because `OCS_REDIS_ENABLED=true` puts the catalog cache and rate-limit buckets in Redis, shared by every instance. Logs are structured JSON (ECS format) and carry the request ID.

## First deployment

1. **State bucket.** Create an S3 bucket for Terraform state, with versioning on.
2. **Deploy role.** Create an IAM role GitHub Actions can assume through OIDC (trust `token.actions.githubusercontent.com` for this repository). Give it permissions for the services above.
3. **Repository settings.** Add the secret `AWS_DEPLOY_ROLE_ARN` and the variables `TF_STATE_BUCKET` and `AWS_REGION` (optional, defaults to `us-west-2`). Create a `production` environment if you want manual approval.
4. **Run it.** Start **Actions → Deploy to AWS**. It creates the image repositories, builds and pushes all six images tagged with the commit SHA, then applies the stack. The job summary prints the web URL and the API Gateway URL.

To run it from your own machine:

```bash
cd infra/terraform
terraform init -backend-config="bucket=<state-bucket>" -backend-config="key=ocs/terraform.tfstate" -backend-config="region=us-west-2"
terraform apply -var image_tag=<tag>
```

Optional variables (see `variables.tf`): `certificate_arn` and `site_url` for HTTPS and a custom domain, `db_multi_az`, `backend_desired_count` / `backend_max_count`, `db_instance_class`, `redis_node_type`.

## Cost and teardown

This is a production-shaped layout, so it is not free. The main costs are the NAT gateway, the ALB, RDS, two Redis nodes and seven always-on Fargate tasks. Estimate your region with the AWS Pricing Calculator before applying. For a short demo, set `deletion_protection = false`. `terraform destroy` then removes everything, including the image repositories.

## Production notes

- The demo seed is a Flyway migration. For a real environment, set `OCS_FLYWAY_LOCATIONS=classpath:db/migration` on the backend task to leave it out.
- The web app reaches the API inside the VPC. API Gateway limits apply to direct API clients. To throttle web traffic at the edge too, put AWS WAF rate-based rules on the ALB.
