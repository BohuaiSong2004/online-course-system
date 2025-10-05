output "web_url" {
  description = "Public URL of the web app"
  value       = local.site_url
}

output "invoke_url" {
  description = "API Gateway base URL for server-to-server clients (append /api/...)"
  value       = aws_api_gateway_stage.this.invoke_url
}

output "ecr_repositories" {
  description = "Image repositories the deploy workflow pushes to"
  value       = { for k, r in aws_ecr_repository.this : k => r.repository_url }
}

output "ecs_cluster" {
  value = aws_ecs_cluster.this.name
}

output "hris_sync_api_key" {
  description = "API key for the HRIS integration client"
  value       = aws_api_gateway_api_key.hris_sync.value
  sensitive   = true
}
