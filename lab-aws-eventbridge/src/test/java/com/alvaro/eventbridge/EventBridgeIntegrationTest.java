package com.alvaro.eventbridge;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.alvaro.eventbridge.event.TransacaoFinanceiraEvento;
import com.alvaro.eventbridge.publisher.EventBridgePublisher;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.CreateEventBusRequest;
import software.amazon.awssdk.services.eventbridge.model.PutRuleRequest;
import software.amazon.awssdk.services.eventbridge.model.PutTargetsRequest;
import software.amazon.awssdk.services.eventbridge.model.Target;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Testcontainers
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EventBridgeIntegrationTest {

    @Container
    static LocalStackContainer localstack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3.4.0"));

    static {
        localstack.start();
    }

    @TestConfiguration
    static class TestAwsConfig {

        @Bean
        @Primary
        public EventBridgeClient testEventBridgeClient() {
            return EventBridgeClient.builder()
                    .endpointOverride(localstack.getEndpoint()) // ◄◄ Apenas getEndpoint()
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())))
                    .region(Region.of(localstack.getRegion()))
                    .build();
        }

        @Bean
        public SqsClient testSqsClient() {
            return SqsClient.builder()
                    .endpointOverride(localstack.getEndpoint()) // ◄◄ Apenas getEndpoint()
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())))
                    .region(Region.of(localstack.getRegion()))
                    .build();
        }
    }

    @Autowired
    private EventBridgePublisher publisher;

    @Autowired
    private EventBridgeClient eventBridgeClient;

    @Autowired
    private SqsClient sqsClient;

    private String queueUrl;

    @BeforeAll
    void setupInfraestruturaAws() {
        String busName = "banking-event-bus";
        String queueName = "auditoria-alta-renda-queue";

        // 1. Cria o Barramento de Eventos Corporativo no EventBridge
        eventBridgeClient.createEventBus(CreateEventBusRequest.builder().name(busName).build());

        // 2. Cria a Fila SQS de Auditoria
        queueUrl = sqsClient.createQueue(CreateQueueRequest.builder().queueName(queueName).build()).queueUrl();
        String queueArn = sqsClient.getQueueAttributes(GetQueueAttributesRequest.builder()
                .queueUrl(queueUrl)
                .attributeNames(QueueAttributeName.QUEUE_ARN)
                .build()).attributes().get(QueueAttributeName.QUEUE_ARN);

        // 3. Cria a Regra no EventBridge: Filtra apenas transações com valor >= 10000
        String eventPatternJson = """
                {
                  "source": ["banking.pagamentos"],
                  "detail-type": ["TransacaoProcessada"],
                  "detail": {
                    "valor": [{ "numeric": [">=", 10000] }]
                  }
                }
                """;

        eventBridgeClient.putRule(PutRuleRequest.builder()
                .eventBusName(busName)
                .name("regra-auditoria-alta-renda")
                .eventPattern(eventPatternJson)
                .build());

        // 4. Vincula a Fila SQS como Target da Regra
        eventBridgeClient.putTargets(PutTargetsRequest.builder()
                .eventBusName(busName)
                .rule("regra-auditoria-alta-renda")
                .targets(Target.builder()
                        .id("target-sqs-auditoria")
                        .arn(queueArn)
                        .build())
                .build());
    }

    @Test
    @DisplayName("Deve rotear para a fila SQS apenas eventos com valor >= 10.000 descartando transações comuns")
    void deveRotearApenasEventosDeAltoValorPeloEventBridge() {
        String idVarejo = "TRX-VAREJO-" + UUID.randomUUID();
        String idAltaRenda = "TRX-ALTA-RENDA-" + UUID.randomUUID();

        TransacaoFinanceiraEvento transacaoPequena = new TransacaoFinanceiraEvento(
                idVarejo, "CONTA-01", "CONTA-02", new BigDecimal("500.00"), "BRL", "PIX", Instant.now());

        TransacaoFinanceiraEvento transacaoAltaRenda = new TransacaoFinanceiraEvento(
                idAltaRenda, "CONTA-VIP-10", "CONTA-VIP-20", new BigDecimal("75000.00"), "BRL", "TRANSFERENCIA",
                Instant.now());

        // Publica ambos os eventos no barramento do EventBridge
        publisher.publicar(transacaoPequena);
        publisher.publicar(transacaoAltaRenda);

        // Aguarda a propagação e lê a fila SQS
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            List<Message> mensagens = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(10)
                    .build()).messages();

            // PROVA DA REGRA DO EVENTBRIDGE: Apenas 1 mensagem chegou na fila!
            assertThat(mensagens).hasSize(1);
            assertThat(mensagens.getFirst().body()).contains(idAltaRenda);
            assertThat(mensagens.getFirst().body()).contains("75000");
            // A transação de R$ 500,00 foi filtrada e descartada pela AWS!
            assertThat(mensagens.getFirst().body()).doesNotContain(idVarejo);
        });
    }
}
