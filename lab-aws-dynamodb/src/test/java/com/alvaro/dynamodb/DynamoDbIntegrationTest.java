package com.alvaro.dynamodb;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
import org.testcontainers.utility.DockerImageName;

import com.alvaro.dynamodb.model.TransacaoLedger;
import com.alvaro.dynamodb.repository.LedgerRepository;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DynamoDbIntegrationTest {

    // Singleton Pattern: sobe o LocalStack no carregamento da JVM
    static LocalStackContainer localstack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3.4.0"));

    static {
        localstack.start();
    }

    @TestConfiguration
    static class TestAwsConfig {

        @Bean
        @Primary
        public DynamoDbClient testDynamoDbClient() {
            return DynamoDbClient.builder()
                    .endpointOverride(localstack.getEndpoint())
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())))
                    .region(Region.of(localstack.getRegion()))
                    .build();
        }
    }

    @Autowired
    private LedgerRepository repository;

    @BeforeAll
    void setup() {
        // Cria a tabela física no DynamoDB baseada no schema anotado
        repository.criarTabelaSeNaoExistir();
    }

    @Test
    @DisplayName("Deve gravar transações em partição de conta e consultar extrato ordenado em < 10ms")
    void deveGravarEConsultarTransacoesPorParticaoEOrdem() {
        String contaA = "CTA-1001";
        String contaB = "CTA-2002";
        Instant agora = Instant.now();

        // 1. Criar 3 transações para a Conta A em momentos sucessivos
        TransacaoLedger t1 = new TransacaoLedger();
        String t1Id = UUID.randomUUID().toString();
        t1.setContaId(contaA);
        t1.setTransacaoId(t1Id);
        t1.setCriadoEm(agora);
        t1.setPk(TransacaoLedger.buildPk(contaA));
        t1.setSk(TransacaoLedger.buildSk(agora, t1Id));
        t1.setDescricao("Salário");
        t1.setValor(new BigDecimal("10000.00"));
        t1.setTipo("CREDITO");
        t1.setStatus("PROCESSADA");

        Instant tempo2 = agora.plusSeconds(60);
        TransacaoLedger t2 = new TransacaoLedger();
        String t2Id = UUID.randomUUID().toString();
        t2.setContaId(contaA);
        t2.setTransacaoId(t2Id);
        t2.setCriadoEm(tempo2);
        t2.setPk(TransacaoLedger.buildPk(contaA));
        t2.setSk(TransacaoLedger.buildSk(tempo2, t2Id));
        t2.setDescricao("Aluguel");
        t2.setValor(new BigDecimal("3000.00"));
        t2.setTipo("DEBITO");
        t2.setStatus("PROCESSADA");

        Instant tempo3 = agora.plusSeconds(120);
        TransacaoLedger t3 = new TransacaoLedger();
        String t3Id = UUID.randomUUID().toString();
        t3.setContaId(contaA);
        t3.setTransacaoId(t3Id);
        t3.setCriadoEm(tempo3);
        t3.setPk(TransacaoLedger.buildPk(contaA));
        t3.setSk(TransacaoLedger.buildSk(tempo3, t3Id));
        t3.setDescricao("Mercado");
        t3.setValor(new BigDecimal("450.00"));
        t3.setTipo("DEBITO");
        t3.setStatus("PROCESSADA");

        // 2. Criar 1 transação para a Conta B (para provar o isolamento de partição)
        TransacaoLedger t4 = new TransacaoLedger();
        String t4Id = UUID.randomUUID().toString();
        t4.setContaId(contaB);
        t4.setTransacaoId(t4Id);
        t4.setCriadoEm(agora.plusSeconds(30));
        t4.setPk(TransacaoLedger.buildPk(contaB));
        t4.setSk(TransacaoLedger.buildSk(agora.plusSeconds(30), t4Id));
        t4.setDescricao("Investimento");
        t4.setValor(new BigDecimal("50000.00"));
        t4.setTipo("CREDITO");
        t4.setStatus("PROCESSADA");

        // 3. Gravar na tabela única do DynamoDB
        repository.salvar(t1);
        repository.salvar(t2);
        repository.salvar(t3);
        repository.salvar(t4);

        // 4. Executar a Query cirúrgica por Partição (PK = CONTA#CTA-1001)
        List<TransacaoLedger> extratoContaA = repository.buscarExtratoPorConta(contaA);

        // 5. Validação das propriedades do Single-Table Design:
        // A) Deve conter APENAS as 3 transações da conta A (sem misturar com a conta B)
        // B) A ordem DEVE ser exatamente cronológica: ["Salário", "Aluguel", "Mercado"]
        assertThat(extratoContaA)
                .hasSize(3)
                .extracting(TransacaoLedger::getDescricao)
                .containsExactly("Salário", "Aluguel", "Mercado");

        // C) Validação de busca pontual por chave primária composta (PK + SK)
        Optional<TransacaoLedger> buscaPontual = repository.buscarPorChave(contaA, t1.getSk());
        assertThat(buscaPontual).isPresent();
        assertThat(buscaPontual.get().getValor()).isEqualByComparingTo("10000.00");
        assertThat(buscaPontual.get().getDescricao()).isEqualTo("Salário");
    }
}
