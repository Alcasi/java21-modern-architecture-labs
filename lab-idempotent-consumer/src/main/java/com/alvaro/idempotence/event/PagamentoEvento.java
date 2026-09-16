package com.alvaro.idempotence.event;

import java.math.BigDecimal;
import java.time.Instant;

public record PagamentoEvento(
        String transacaoId, // ◄◄ CHAVE DE IDEMPOTÊNCIA ÚNICA GLOBAL!
        String contaOrigem,
        String contaDestino,
        BigDecimal valor,
        Instant timestamp) {
}
