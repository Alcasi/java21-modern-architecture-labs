package com.alvaro.saga.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AntifraudeService {

    private static final Logger log = LoggerFactory.getLogger(AntifraudeService.class);

    public String analisarRisco(String conta, BigDecimal valor) {
        if ("CONTA-SUSPEITA".equals(conta)) {
            throw new SecurityException("Transação bloqueada por suspeita de fraude!");
        }
        log.info("🛡️ Análise de fraude aprovada para a conta {} no valor de R$ {}", conta, valor);
        return "FRAUDE-OK-" + UUID.randomUUID();
    }
}
