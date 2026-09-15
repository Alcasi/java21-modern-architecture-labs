package com.alvaro.kafka.event;

import java.math.BigDecimal;
import java.time.Instant;

public record TransacaoEvento(
    String transacaoId,
    String contaId,
    BigDecimal valor,
    String tipo, // DEPOSITO, SAQUE, PIX
    Instant timestamp
) {}
