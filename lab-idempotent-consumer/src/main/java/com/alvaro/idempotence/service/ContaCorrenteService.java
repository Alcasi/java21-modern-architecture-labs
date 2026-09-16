package com.alvaro.idempotence.service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alvaro.idempotence.event.PagamentoEvento;

@Service
public class ContaCorrenteService {

    private static final Logger log = LoggerFactory.getLogger(ContaCorrenteService.class);

    private final AtomicInteger totalDebitosExecutados = new AtomicInteger(0);
    private final List<String> transacoesProcessadas = new CopyOnWriteArrayList<>();

    public void processarDebito(PagamentoEvento evento) {
        totalDebitosExecutados.incrementAndGet();
        transacoesProcessadas.add(evento.transacaoId());
        log.info("💸 [CORE BANKING] Débito de R$ {} efetuado com sucesso na conta {}. TransacaoId: {}",
                evento.valor(), evento.contaOrigem(), evento.transacaoId());
    }

    public int getTotalDebitosExecutados() {
        return totalDebitosExecutados.get();
    }

    public List<String> getTransacoesProcessadas() {
        return transacoesProcessadas;
    }
}
