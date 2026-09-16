package com.alvaro.outbox.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PagamentoCriadoEvento(
        UUID eventoId,
        UUID pagamentoId,
        String contaOrigem,
        String contaDestino,
        BigDecimal valor,
        Instant dataHora) {
}
