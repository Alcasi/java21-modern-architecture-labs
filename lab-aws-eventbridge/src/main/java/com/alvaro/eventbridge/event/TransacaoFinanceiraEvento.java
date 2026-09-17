package com.alvaro.eventbridge.event;

import java.math.BigDecimal;
import java.time.Instant;

public record TransacaoFinanceiraEvento(
        String transacaoId,
        String contaOrigem,
        String contaDestino,
        BigDecimal valor,
        String moeda,
        String tipo, // ex: "PIX", "CAMBIO_INTERNACIONAL"
        Instant timestamp) {
}
