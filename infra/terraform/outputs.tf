output "invoke_url" {
  description = "Base URL clients should call (append /api/...)"
  value       = aws_api_gateway_stage.this.invoke_url
}

output "hris_sync_api_key" {
  description = "API key for the HRIS integration client"
  value       = aws_api_gateway_api_key.hris_sync.value
  sensitive   = true
}
