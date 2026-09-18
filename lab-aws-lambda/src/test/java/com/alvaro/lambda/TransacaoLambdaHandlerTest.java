package com.alvaro.lambda;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.alvaro.lambda.handler.TransacaoLambdaHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;

import static org.assertj.core.api.Assertions.assertThat;

class TransacaoLambdaHandlerTest {

    private TransacaoLambdaHandler handler;
    private Context context;

    @BeforeEach
    void setUp() {
        handler = new TransacaoLambdaHandler();
        context = Mockito.mock(Context.class);
    }

    @Test
    @DisplayName("1. Deve simular o ciclo de vida do SnapStart/CRaC e pré-aquecer a memória")
    void deveExecutarCicloDeVidaSnapStartEPreAquecerMemoria() throws Exception {
        // Antes do checkpoint: ainda não aqueceu
        assertThat(handler.isAquecido()).isFalse();

        // AWS chama beforeCheckpoint antes de tirar o snapshot
        handler.beforeCheckpoint(null);
        assertThat(handler.isAquecido()).isTrue();

        // AWS restaura do snapshot quando chega tráfego
        handler.afterRestore(null);
        assertThat(handler.isAquecido()).isTrue();
    }

    @Test
    @DisplayName("2. Deve processar lote de mensagens SQS e calcular o total financeiro")
    void deveProcessarLoteDeMensagensSqsComSucesso() {
        // 1. Montamos duas mensagens JSON de transação usando Java Text Blocks
        SQSMessage msg1 = new SQSMessage();
        msg1.setBody("""
                    {
                        "transacaoId": "TRX-001",
                        "contaId": "CTA-1001",
                        "valor": 5000.00,
                        "tipo": "CREDITO",
                        "timestamp": "2026-09-18T19:00:00Z"
                    }
                """);

        SQSMessage msg2 = new SQSMessage();
        msg2.setBody("""
                    {
                        "transacaoId": "TRX-002",
                        "contaId": "CTA-2002",
                        "valor": 2500.00,
                        "tipo": "DEBITO",
                        "timestamp": "2026-09-18T19:05:00Z"
                    }
                """);

        // 2. Encapsulamos no SQSEvent que a AWS entrega ao Lambda
        SQSEvent event = new SQSEvent();
        event.setRecords(List.of(msg1, msg2));

        // 3. Executamos o Handler
        String resultado = handler.handleRequest(event, context);

        // 4. Validamos que as 2 transações foram processadas e totalizaram R$ 7500.00
        assertThat(resultado)
                .contains("2 transações processadas")
                .contains("R$ 7500.00");
    }

    @Test
    @DisplayName("3. Deve tratar evento vazio sem estourar exceção")
    void deveTratarEventoVazioComSeguranca() {
        SQSEvent eventoVazio = new SQSEvent();
        String resultado = handler.handleRequest(eventoVazio, context);

        assertThat(resultado).isEqualTo("Nenhuma mensagem recebida no lote SQS");
    }
}
