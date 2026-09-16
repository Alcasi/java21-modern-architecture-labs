package com.alvaro.idempotence;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.alvaro.idempotence.event.PagamentoEvento;
import com.alvaro.idempotence.producer.PagamentoProducer;
import com.alvaro.idempotence.service.ContaCorrenteService;
import com.redis.testcontainers.RedisContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Testcontainers
@SpringBootTest
class IdempotencyIntegrationTest {

    // Sobe o broker Apache Kafka 7.6 real
    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    // Sobe o Redis 7.2 Alpine em memória
    @Container
    @ServiceConnection
    static RedisContainer redis = new RedisContainer(
            DockerImageName.parse("redis:7.2-alpine"));

    @Autowired
    private PagamentoProducer producer;

    @Autowired
    private ContaCorrenteService contaCorrenteService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    @DisplayName("Deve executar débito estritamente 1 vez mesmo com 3 mensagens duplicadas recebidas do Kafka")
    void deveProcessarApenasUmaVezMesmoComTresEnviosDuplicadosNoKafka() {
        String transacaoIdUnica = "TRX-IDEMPOTENTE-" + UUID.randomUUID();
        BigDecimal valor = new BigDecimal("750.00");

        PagamentoEvento evento = new PagamentoEvento(
                transacaoIdUnica,
                "CONTA-CLIENTE-100",
                "CONTA-DESTINO-200",
                valor,
                Instant.now());

        // 💥 DISPARO EM RAJADA: Enviamos a MESMA mensagem 3 vezes seguidas no Kafka!
        producer.enviar(evento);
        producer.enviar(evento);
        producer.enviar(evento);

        // Aguarda o consumidor processar e descartar as duplicatas
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            // PROVA CONTÁBIL: O débito foi executado ESTRITAMENTE 1 ÚNICA VEZ!
            assertThat(contaCorrenteService.getTotalDebitosExecutados()).isEqualTo(1);
            assertThat(contaCorrenteService.getTransacoesProcessadas()).hasSize(1);
            assertThat(contaCorrenteService.getTransacoesProcessadas().getFirst()).isEqualTo(transacaoIdUnica);
        });

        // PROVA NO REDIS: A chave foi gravada com status PROCESSED
        String statusNoRedis = redisTemplate.opsForValue().get("idempotency:transacao:" + transacaoIdUnica);
        assertThat(statusNoRedis).isEqualTo("PROCESSED");
    }

    @Test
    @DisplayName("Deve permitir e processar transações distintas normalmente")
    void deveProcessarTransacoesDistintasNormalmente() {
        int debitosIniciais = contaCorrenteService.getTotalDebitosExecutados();

        PagamentoEvento evento1 = new PagamentoEvento(
                "TRX-DISTINTA-" + UUID.randomUUID(),
                "CONTA-CLIENTE-100",
                "CONTA-DESTINO-300",
                new BigDecimal("100.00"),
                Instant.now());

        PagamentoEvento evento2 = new PagamentoEvento(
                "TRX-DISTINTA-" + UUID.randomUUID(),
                "CONTA-CLIENTE-100",
                "CONTA-DESTINO-400",
                new BigDecimal("200.00"),
                Instant.now());

        producer.enviar(evento1);
        producer.enviar(evento2);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            assertThat(contaCorrenteService.getTotalDebitosExecutados()).isEqualTo(debitosIniciais + 2);
        });
    }
}
