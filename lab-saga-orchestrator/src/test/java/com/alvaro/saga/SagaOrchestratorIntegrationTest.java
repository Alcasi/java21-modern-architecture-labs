package com.alvaro.saga;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.alvaro.saga.domain.SagaInstancia;
import com.alvaro.saga.model.SagaEstado;
import com.alvaro.saga.orchestrator.TransferenciaSagaOrchestrator;
import com.alvaro.saga.repository.SagaInstanciaRepository;
import com.alvaro.saga.service.ContaService;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class SagaOrchestratorIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TransferenciaSagaOrchestrator orchestrator;

    @Autowired
    private SagaInstanciaRepository sagaRepository;

    @Autowired
    private ContaService contaService;

    @Test
    @DisplayName("Cenário 1: Deve executar os 3 passos com sucesso e concluir a Saga")
    void deveExecutarSagaComSucesso() {
        BigDecimal valor = new BigDecimal("500.00");
        SagaEstado resultado = orchestrator.executar("CONTA-ORIGEM-10", "CONTA-DESTINO-20", valor);

        // 1. Validação do Tipo Imutável com Pattern Matching do Java 21
        assertThat(resultado).isInstanceOf(SagaEstado.Concluida.class);
        if (resultado instanceof SagaEstado.Concluida concluida) {
            assertThat(concluida.protocoloLiquidacao()).startsWith("LIQ-OK-");
        }

        // 2. Validação no PostgreSQL: Estado final CONCLUIDA
        List<SagaInstancia> sagas = sagaRepository.findAll();
        assertThat(sagas).isNotEmpty();
        SagaInstancia saga = sagas.getLast(); // Sequenced Collections do Java 21!
        assertThat(saga.getEstadoAtual()).isEqualTo("CONCLUIDA");
        assertThat(saga.getIdReserva()).isNotNull();

        // 3. Nenhuma compensação deve ter sido acionada
        assertThat(contaService.getReservasEstornadas()).doesNotContain(saga.getIdReserva());
    }

    @Test
    @DisplayName("Cenário 2: Falha no Antifraude deve disparar compensação semântica (Estorno de Saldo)")
    void deveCompensarSaldoQuandoAntifraudeRejeitar() {
        BigDecimal valor = new BigDecimal("1500.00");
        // CONTA-SUSPEITA forçará a rejeição no Passo 2
        SagaEstado resultado = orchestrator.executar("CONTA-SUSPEITA", "CONTA-DESTINO-20", valor);

        // 1. Validação do Estado: FALHA_COMPENSADA
        assertThat(resultado).isInstanceOf(SagaEstado.FalhaCompensada.class);
        if (resultado instanceof SagaEstado.FalhaCompensada falha) {
            assertThat(falha.motivoFalha()).contains("suspeita de fraude");
        }

        // 2. Validação da Persistência
        SagaInstancia saga = sagaRepository.findAll().getLast();
        assertThat(saga.getEstadoAtual()).isEqualTo("FALHA_COMPENSADA");
        assertThat(saga.getMotivoFalha()).contains("suspeita de fraude");

        // 3. PROVA DO ROLLBACK DISTRIBUÍDO: A reserva do Passo 1 foi estornada!
        assertThat(contaService.getReservasEstornadas()).contains(saga.getIdReserva());
    }

    @Test
    @DisplayName("Cenário 3: Falha na Liquidação (Passo 3) deve disparar compensação da reserva (Passo 1)")
    void deveCompensarSaldoQuandoLiquidacaoFalharNoDestino() {
        BigDecimal valor = new BigDecimal("200.00");
        // CONTA-BLOQUEADA forçará a rejeição no Passo 3
        SagaEstado resultado = orchestrator.executar("CONTA-ORIGEM-10", "CONTA-BLOQUEADA", valor);

        // 1. Validação do Estado
        assertThat(resultado).isInstanceOf(SagaEstado.FalhaCompensada.class);

        // 2. Validação do Saga Log
        SagaInstancia saga = sagaRepository.findAll().getLast();
        assertThat(saga.getEstadoAtual()).isEqualTo("FALHA_COMPENSADA");
        assertThat(saga.getMotivoFalha()).contains("bloqueada ou encerrada");

        // 3. PROVA DA COMPENSAÇÃO SEMÂNTICA
        assertThat(contaService.getReservasEstornadas()).contains(saga.getIdReserva());
    }
}
