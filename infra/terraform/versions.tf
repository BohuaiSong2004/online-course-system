terraform {
  required_version = ">= 1.5"

  # State lives in S3 (partial config). Initialise with:
  #   terraform init -backend-config="bucket=<state-bucket>" -backend-config="key=ocs/terraform.tfstate" \
  #                  -backend-config="region=us-west-2"
  backend "s3" {}

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }
}

provider "aws" {
  region = var.region

  default_tags {
    tags = {
      Project   = "online-course-system"
      ManagedBy = "terraform"
    }
  }
}
