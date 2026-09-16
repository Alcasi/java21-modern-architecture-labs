package com.alvaro.saga.orchestrator;

import java.math.BigDecimal;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.alvaro.saga.domain.SagaInstancia;
import com.alvaro.saga.model.SagaEstado;
import com.alvaro.saga.repository.SagaInstanciaRepository;
import com.alvaro.saga.service.AntifraudeService;
import com.alvaro.saga.service.ContaService;
import com.alvaro.saga.service.LiquidacaoService;

@Component
public class TransferenciaSagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(TransferenciaSagaOrchestrator.class);

    private final SagaInstanciaRepository sagaRepository;
    private final ContaService contaService;
    private final AntifraudeService antifraudeService;
    private final LiquidacaoService liquidacaoService;

    public TransferenciaSagaOrchestrator(SagaInstanciaRepository sagaRepository,
            ContaService contaService,
            AntifraudeService antifraudeService,
            LiquidacaoService liquidacaoService) {
        this.sagaRepository = sagaRepository;
        this.contaService = contaService;
        this.antifraudeService = antifraudeService;
        this.liquidacaoService = liquidacaoService;
    }

    @Transactional
    public SagaEstado executar(String contaOrigem, String contaDestino, BigDecimal valor) {
        SagaInstancia saga = new SagaInstancia(contaOrigem, contaDestino, valor);
        sagaRepository.save(saga);
        log.info("🎬 [SAGA {}] Iniciando transferência de R$ {} ({} -> {})",
                saga.getId(), valor, contaOrigem, contaDestino);

        String idReserva = null;

        try {
            // PASSO 1: Reservar Saldo na Conta de Origem
            idReserva = contaService.reservarSaldo(contaOrigem, valor);
            saga.transicionarEstado("SALDO_RESERVADO", idReserva);
            sagaRepository.save(saga);

            // PASSO 2: Avaliação de Risco Antifraude
            String protocoloFraude = antifraudeService.analisarRisco(contaOrigem, valor);
            saga.transicionarEstado("FRAUDE_APROVADA", idReserva);
            sagaRepository.save(saga);

            // PASSO 3: Liquidação e Crédito na Conta Destino
            String protocoloLiquidacao = liquidacaoService.creditarDestino(contaDestino, valor);
            saga.transicionarEstado("CONCLUIDA", idReserva);
            sagaRepository.save(saga);
            log.info("🎉 [SAGA {}] Concluída com sucesso!", saga.getId());

            return new SagaEstado.Concluida(protocoloLiquidacao, Instant.now());

        } catch (Exception ex) {
            log.error("💥 [SAGA {}] Erro durante a execução: {}. Acionando compensação...",
                    saga.getId(), ex.getMessage());

            // ⚠️ TRANSAÇÃO DE COMPENSAÇÃO SEMÂNTICA (ROLLBACK DISTRIBUÍDO)
            if (idReserva != null) {
                contaService.estornarReserva(contaOrigem, idReserva, valor);
            }

            saga.registrarFalhaCompensada(ex.getMessage());
            sagaRepository.save(saga);
            log.warn("↩️ [SAGA {}] Compensação semântica concluída. Estado final: FALHA_COMPENSADA", saga.getId());

            return new SagaEstado.FalhaCompensada(ex.getMessage(), Instant.now());
        }
    }
}
