package com.alvaro.outbox.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alvaro.outbox.domain.OutboxEvento;
import com.alvaro.outbox.domain.Pagamento;
import com.alvaro.outbox.event.PagamentoCriadoEvento;
import com.alvaro.outbox.repository.OutboxEventoRepository;
import com.alvaro.outbox.repository.PagamentoRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class PagamentoService {

    private static final Logger log = LoggerFactory.getLogger(PagamentoService.class);

    private final PagamentoRepository pagamentoRepository;
    private final OutboxEventoRepository outboxEventoRepository;
    private final ObjectMapper objectMapper;

    public PagamentoService(PagamentoRepository pagamentoRepository,
            OutboxEventoRepository outboxEventoRepository,
            ObjectMapper objectMapper) {
        this.pagamentoRepository = pagamentoRepository;
        this.outboxEventoRepository = outboxEventoRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Pagamento processarPagamento(String contaOrigem, String contaDestino, BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor do pagamento deve ser estritamente positivo: " + valor);
        }

        // 1. Salva a entidade de negócio
        Pagamento pagamento = new Pagamento(contaOrigem, contaDestino, valor);
        pagamentoRepository.save(pagamento);
        log.info("💳 Pagamento salvo no PostgreSQL: {}", pagamento.getId());

        // 2. Constrói o Record imutável do evento
        PagamentoCriadoEvento evento = new PagamentoCriadoEvento(
                UUID.randomUUID(),
                pagamento.getId(),
                pagamento.getContaOrigem(),
                pagamento.getContaDestino(),
                pagamento.getValor(),
                pagamento.getCreateAt());

        // 3. Serializa para JSON e grava na OUTBOX na mesma transação ACID
        try {
            String payloadJson = objectMapper.writeValueAsString(evento);
            OutboxEvento outboxEvento = new OutboxEvento(
                    "PAGAMENTO",
                    pagamento.getId().toString(),
                    "PAGAMENTO_CRIADO",
                    payloadJson);
            outboxEventoRepository.save(outboxEvento);
            log.info("📦 Evento gravado atomicamente na tabela outbox_eventos: {}", outboxEvento.getId());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Erro ao serializar payload do evento", e);
        }

        return pagamento;
    }
}
