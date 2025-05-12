variable "region" {
  description = "AWS region"
  type        = string
  default     = "us-west-2"
}

variable "name" {
  description = "Name prefix for resources"
  type        = string
  default     = "ocs"
}

variable "stage_name" {
  description = "API Gateway stage"
  type        = string
  default     = "prod"
}

variable "backend_base_url" {
  description = "Public base URL of the Spring Boot service (e.g. the ALB in front of ECS), without trailing slash"
  type        = string
}

variable "throttle_rate_limit" {
  description = "Steady-state requests per second allowed across the stage"
  type        = number
  default     = 50
}

variable "throttle_burst_limit" {
  description = "Maximum burst (token bucket size) across the stage"
  type        = number
  default     = 100
}

variable "auth_rate_limit" {
  description = "Stricter per-second limit for the /api/auth/* endpoints (brute-force protection)"
  type        = number
  default     = 5
}

variable "auth_burst_limit" {
  description = "Burst limit for the /api/auth/* endpoints"
  type        = number
  default     = 10
}

variable "partner_quota_per_day" {
  description = "Daily request quota for API-key clients (integrations such as an HRIS sync)"
  type        = number
  default     = 10000
}
