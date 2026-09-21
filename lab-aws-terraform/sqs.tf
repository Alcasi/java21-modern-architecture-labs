# 1. Fila de Cartas Mortas (DLQ)
resource "aws_sqs_queue" "transacoes_dlq" {
  name                      = "${var.project_name}-transacoes-dlq-${var.environment}"
  message_retention_seconds = 1209600 # 14 dias de retenção máxima

  tags = {
    Environment = var.environment
    Project     = var.project_name
    Type        = "DLQ"
  }
}

# 2. Fila Principal de Transações
resource "aws_sqs_queue" "transacoes_queue" {
  name                       = "${var.project_name}-transacoes-${var.environment}"
  visibility_timeout_seconds = 30
  receive_wait_time_seconds  = 20 # Long polling ativado (otimiza custo e latência)

  # Redrive Policy: envia para a DLQ após 3 falhas de processamento
  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.transacoes_dlq.arn
    maxReceiveCount     = 3
  })

  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}
