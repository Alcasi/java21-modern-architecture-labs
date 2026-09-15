package com.alvaro.kafka;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.alvaro.kafka.consumer.TransacaoConsumer;
import com.alvaro.kafka.event.TransacaoEvento;
import com.alvaro.kafka.producer.TransacaoProducer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Testcontainers
@SpringBootTest
class KafkaIntegrationTest {

    // O Testcontainers sobe o broker real do Apache Kafka no Docker
    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.1")
    );

    @Autowired
    private TransacaoProducer producer;

    @Autowired
    private TransacaoConsumer consumer;

    @Test
    @DisplayName("Deve publicar transacoes e garantir ordenacao estrita por chave de conta no Kafka")
    void devePublicarEConsumirComGarantiaDeOrdenacao() {
        String contaPrincipal = "CONTA-CORRENTE-1001";
        String contaSecundaria = "CONTA-POUPANCA-2002";

        // 1. Criamos eventos: dois para a mesma conta e um para outra
        TransacaoEvento evento1 = new TransacaoEvento(
                UUID.randomUUID().toString(),
                contaPrincipal,
                new BigDecimal("1000.00"),
                "DEPOSITO",
                Instant.now()
        );

        TransacaoEvento evento2 = new TransacaoEvento(
                UUID.randomUUID().toString(),
                contaSecundaria,
                new BigDecimal("500.00"),
                "DEPOSITO",
                Instant.now()
        );

        TransacaoEvento evento3 = new TransacaoEvento(
                UUID.randomUUID().toString(),
                contaPrincipal,
                new BigDecimal("250.00"),
                "SAQUE",
                Instant.now()
        );

        // 2. Enviamos os eventos pelo Producer (com a chave contaId)
        producer.enviar(evento1);
        producer.enviar(evento2);
        producer.enviar(evento3);

        // 3. Awaitility: Aguardamos o consumidor processar as 3 mensagens assincronamente
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            assertThat(consumer.getEventosRecebidos()).hasSize(3);
        });

        // 4. Prova de Ordenacao Estrita por Chave de Particao!
        List<TransacaoEvento> eventosContaPrincipal = consumer.getEventosRecebidos().stream()
                .filter(e -> e.contaId().equals(contaPrincipal))
                .toList();

        assertThat(eventosContaPrincipal).hasSize(2);
        // O Depósito veio estritamente antes do Saque
        assertThat(eventosContaPrincipal.getFirst().tipo()).isEqualTo("DEPOSITO");
        assertThat(eventosContaPrincipal.getLast().tipo()).isEqualTo("SAQUE");
    }
}
