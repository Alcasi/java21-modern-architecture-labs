variable "aws_region" {
  description = "Região da AWS"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Ambiente de deploy (dev, stg, prod)"
  type        = string
  default     = "dev"
}

variable "project_name" {
  description = "Nome base do projeto"
  type        = string
  default     = "banking-core"
}
