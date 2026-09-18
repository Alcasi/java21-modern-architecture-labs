package com.alvaro.lambda.model;

import java.math.BigDecimal;
import java.time.Instant;

public record TransacaoMensagem(
        String transacaoId,
        String contaId,
        BigDecimal valor,
        String tipo, // CREDITO ou DEBITO
        Instant timestamp) {
}
