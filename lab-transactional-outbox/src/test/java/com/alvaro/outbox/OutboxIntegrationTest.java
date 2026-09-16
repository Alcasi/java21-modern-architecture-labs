package com.alvaro.outbox;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.alvaro.outbox.domain.OutboxEvento;
import com.alvaro.outbox.domain.Pagamento;
import com.alvaro.outbox.publisher.OutboxPublisher;
import com.alvaro.outbox.repository.OutboxEventoRepository;
import com.alvaro.outbox.repository.PagamentoRepository;
import com.alvaro.outbox.service.PagamentoService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
class OutboxIntegrationTest {

    // Sobe o PostgreSQL 16 Alpine com HikariCP auto-configurado pelo
    // @ServiceConnection
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    // Sobe o Apache Kafka 7.6
    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @Autowired
    private PagamentoService pagamentoService;

    @Autowired
    private PagamentoRepository pagamentoRepository;

    @Autowired
    private OutboxEventoRepository outboxEventoRepository;

    @Autowired
    private OutboxPublisher outboxPublisher;

    @Test
    @DisplayName("Cenário 1: Deve gravar pagamento e outbox atomicamente na mesma transação e despachar para o Kafka")
    void deveGravarPagamentoEOutboxNaMesmaTransacaoEDespachar() {
        // 1. Processa o pagamento via serviço transacional
        BigDecimal valor = new BigDecimal("350.00");
        Pagamento pagamento = pagamentoService.processarPagamento("CONTA-ORIGEM-10", "CONTA-DESTINO-20", valor);

        // 2. Validação no PostgreSQL: O pagamento existe e a outbox tem 1 evento
        // PENDING
        assertThat(pagamentoRepository.findById(pagamento.getId())).isPresent();

        List<OutboxEvento> pendentes = outboxEventoRepository.findByStatus("PENDING");
        assertThat(pendentes).hasSize(1);

        OutboxEvento evento = pendentes.getFirst();
        assertThat(evento.getAggregateId()).isEqualTo(pagamento.getId().toString());
        assertThat(evento.getPayload()).contains("350.00");

        // 3. Despachante: envia da Outbox para o cluster Kafka
        int enviados = outboxPublisher.publicarEventosPendentes();
        assertThat(enviados).isEqualTo(1);

        // 4. Validação pós-despacho: o status mudou para PROCESSED
        assertThat(outboxEventoRepository.findByStatus("PENDING")).isEmpty();
        assertThat(outboxEventoRepository.findByStatus("PROCESSED")).hasSize(1);
    }

    @Test
    @DisplayName("Cenário 2 (Prova Dual-Write): Erro na transação deve sofrer rollback total sem vazar registros na Outbox")
    void deveGarantirRollbackTotalQuandoTransacaoFalhar() {
        long totalPagamentosAntes = pagamentoRepository.count();
        long totalOutboxAntes = outboxEventoRepository.count();

        // Tentamos processar um pagamento com valor inválido (-100.00)
        assertThatThrownBy(() -> pagamentoService.processarPagamento("CONTA-ORIGEM-99", "CONTA-DESTINO-99",
                new BigDecimal("-100.00"))).isInstanceOf(IllegalArgumentException.class);

        // PROVA DA ATOMICIDADE: NADA foi salvo no banco e NADA vazou para a Outbox!
        assertThat(pagamentoRepository.count()).isEqualTo(totalPagamentosAntes);
        assertThat(outboxEventoRepository.count()).isEqualTo(totalOutboxAntes);
    }
}
