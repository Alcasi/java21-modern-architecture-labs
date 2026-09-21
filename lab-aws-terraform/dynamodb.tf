resource "aws_dynamodb_table" "banking_ledger" {
  name         = "banking_ledger_${var.environment}"
  billing_mode = "PAY_PER_REQUEST" # On-Demand (escala de 0 a 100k TPS sem provisionar servidores)
  hash_key     = "pk"
  range_key    = "sk"

  attribute {
    name = "pk"
    type = "S"
  }

  attribute {
    name = "sk"
    type = "S"
  }

  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}
