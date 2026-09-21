output "sqs_queue_url" {
  description = "URL da fila SQS principal"
  value       = aws_sqs_queue.transacoes_queue.id
}

output "sqs_queue_arn" {
  description = "ARN da fila SQS principal"
  value       = aws_sqs_queue.transacoes_queue.arn
}

output "sqs_dlq_url" {
  description = "URL da fila DLQ"
  value       = aws_sqs_queue.transacoes_dlq.id
}

output "dynamodb_table_name" {
  description = "Nome da tabela DynamoDB"
  value       = aws_dynamodb_table.banking_ledger.name
}

output "dynamodb_table_arn" {
  description = "ARN da tabela DynamoDB"
  value       = aws_dynamodb_table.banking_ledger.arn
}
