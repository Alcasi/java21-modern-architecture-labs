package com.alvaro.resilience.model;

public record ChavePixValidacao(
        String chave,
        String titular,
        String banco,
        String status, // ATIVA, INATIVA, MODO_DEGRADADO
        String mensagem) {
}
