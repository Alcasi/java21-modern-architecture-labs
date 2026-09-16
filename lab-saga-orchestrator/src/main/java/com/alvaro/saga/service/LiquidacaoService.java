package com.alvaro.saga.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LiquidacaoService {

    private static final Logger log = LoggerFactory.getLogger(LiquidacaoService.class);

    public String creditarDestino(String contaDestino, BigDecimal valor) {
        if ("CONTA-BLOQUEADA".equals(contaDestino)) {
            throw new IllegalArgumentException("Conta destino bloqueada ou encerrada no core bancário!");
        }
        log.info("🏦 Crédito de R$ {} liquidado com sucesso na conta destino {}", valor, contaDestino);
        return "LIQ-OK-" + UUID.randomUUID();
    }
}
