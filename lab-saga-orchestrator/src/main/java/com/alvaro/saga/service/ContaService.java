package com.alvaro.saga.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ContaService {

    private static final Logger log = LoggerFactory.getLogger(ContaService.class);
    // Guarda o histórico de estornos para podermos validar nos testes
    private final List<String> reservasEstornadas = new CopyOnWriteArrayList<>();

    // 1. Passo de Avanço: Reserva Saldo
    public String reservarSaldo(String conta, BigDecimal valor) {
        if ("CONTA-SEM-SALDO".equals(conta)) {
            throw new IllegalStateException("Saldo insuficiente na conta: " + conta);
        }
        String idReserva = "RES-" + UUID.randomUUID();
        log.info("💰 Saldo de R$ {} reservado com sucesso na conta {}. ID Reserva: {}", valor, conta, idReserva);
        return idReserva;
    }

    // 2. Transação de Compensação: Desfaz a reserva (Estorno Semântico)
    public void estornarReserva(String conta, String idReserva, BigDecimal valor) {
        log.warn("↩️ [COMPENSAÇÃO] Estornando reserva {} de R$ {} na conta {}", idReserva, valor, conta);
        reservasEstornadas.add(idReserva);
    }

    public List<String> getReservasEstornadas() {
        return reservasEstornadas;
    }
}
