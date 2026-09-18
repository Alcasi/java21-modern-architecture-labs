package com.alvaro.dynamodb.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.alvaro.dynamodb.model.TransacaoLedger;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

@Repository
public class LedgerRepository {

    private final DynamoDbTable<TransacaoLedger> table;

    public DynamoDbEnhancedClient enhancedClient;

    public String tableName;

    // Construtor idiomático: Spring resolve os parâmetros antes de instanciar
    public LedgerRepository(
            DynamoDbEnhancedClient enhancedClient,
            @Value("${aws.dynamodb.table-name:banking_ledger}") String tableName) {
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(TransacaoLedger.class));
    }

    public void salvar(TransacaoLedger transacao) {
        table.putItem(transacao);
    }

    public Optional<TransacaoLedger> buscarPorChave(String contaId, String sk) {

        Key key = Key.builder()
                .partitionValue(TransacaoLedger.buildPk(contaId))
                .sortValue(sk)
                .build();
        return Optional.ofNullable(table.getItem(key));

    }

    public List<TransacaoLedger> buscarExtratoPorConta(String contaId) {

        QueryConditional queryConditional = QueryConditional.keyEqualTo(
                Key.builder().partitionValue(TransacaoLedger.buildPk(contaId)).build());

        // scanIndexForward(true) garante ordem cronológica ascendente da Sort Key
        return table.query(r -> r.queryConditional(queryConditional).scanIndexForward(true))
                .items()
                .stream()
                .toList(); // Java 21 stream toList direto

    }

    public void criarTabelaSeNaoExistir() {
        try {
            table.createTable();
        } catch (Exception e) {
            // Tabela já existe
        }
    }

}
