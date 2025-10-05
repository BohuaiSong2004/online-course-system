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
  description = "Override for the API origin behind API Gateway. Empty = the ALB created by this stack."
  type        = string
  default     = ""
}

variable "image_tag" {
  description = "Container image tag to deploy (pushed to the ECR repositories by the deploy workflow)"
  type        = string
  default     = "latest"
}

variable "certificate_arn" {
  description = "ACM certificate for HTTPS on the load balancer. Empty = HTTP only (demo)."
  type        = string
  default     = ""
}

variable "site_url" {
  description = "Public URL of the site (canonical links, sitemap). Empty = the load balancer's address."
  type        = string
  default     = ""
}

variable "backend_desired_count" {
  description = "API instances. More than one requires Redis (enabled by this stack) for shared cache and rate limits."
  type        = number
  default     = 2
}

variable "backend_max_count" {
  description = "Upper bound for API autoscaling (CPU target tracking)"
  type        = number
  default     = 6
}

variable "db_instance_class" {
  type    = string
  default = "db.t4g.micro"
}

variable "db_multi_az" {
  description = "Standby replica in a second AZ (recommended for production)"
  type        = bool
  default     = false
}

variable "redis_node_type" {
  type    = string
  default = "cache.t4g.micro"
}

variable "deletion_protection" {
  description = "Protect the database from accidental deletion (set false to tear down a demo)"
  type        = bool
  default     = true
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
