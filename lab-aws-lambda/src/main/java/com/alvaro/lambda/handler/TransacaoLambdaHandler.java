package com.alvaro.lambda.handler;

import java.math.BigDecimal;
import java.time.Instant;

import org.crac.Core;
import org.crac.Resource;

import com.alvaro.lambda.model.TransacaoMensagem;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class TransacaoLambdaHandler implements RequestHandler<SQSEvent, String>, Resource {

    private final ObjectMapper objectMapper;
    private boolean aquecido = false;

    // --- 1. Construtor (Executa no INIT da AWS) ---
    public TransacaoLambdaHandler() {
        // Registra o JavaTimeModule para o Jackson entender o tipo java.time.Instant do
        // Record
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        // Registra esta instância no coordenador CRaC do SnapStart
        Core.getGlobalContext().register(this);
    }

    // --- 2. Gancho CRaC: Executa segundos antes de congelar a memória RAM ---
    @Override
    public void beforeCheckpoint(org.crac.Context<? extends Resource> context) throws Exception {
        System.out.println("⚡ [SnapStart] Pré-aquecendo o Jackson e carregando classes no ClassLoader...");

        // Simula um parse com dados dummy para compilar os bytecodes do Jackson via JIT
        TransacaoMensagem dummy = new TransacaoMensagem(
                "DUMMY", "CTA-000", BigDecimal.ZERO, "CREDITO", Instant.now());
        String json = objectMapper.writeValueAsString(dummy);
        objectMapper.readValue(json, TransacaoMensagem.class);

        this.aquecido = true;
        System.out.println("✅ [SnapStart] Memória pré-aquecida com sucesso. Pronto para o Snapshot!");
    }

    // --- 3. Gancho CRaC: Executa imediatamente após restaurar o snapshot ---
    @Override
    public void afterRestore(org.crac.Context<? extends Resource> context) throws Exception {
        System.out.println("🚀 [SnapStart] Função restaurada do snapshot em menos de 150ms! Pronta para tráfego.");
    }

    // --- 4. Negócio: Processa o lote de mensagens SQS ---
    @Override
    public String handleRequest(SQSEvent event, Context context) {
        BigDecimal totalProcessado = BigDecimal.ZERO;
        int quantidade = 0;

        if (event == null || event.getRecords() == null || event.getRecords().isEmpty()) {
            return "Nenhuma mensagem recebida no lote SQS";
        }

        for (SQSMessage message : event.getRecords()) {
            try {
                // Converte o corpo da mensagem SQS no nosso Record
                TransacaoMensagem transacao = objectMapper.readValue(
                        message.getBody(), TransacaoMensagem.class);

                System.out.printf("💳 Processando transação: ID=%s | Conta=%s | Valor=R$ %s | Tipo=%s%n",
                        transacao.transacaoId(),
                        transacao.contaId(),
                        transacao.valor(),
                        transacao.tipo());

                totalProcessado = totalProcessado.add(transacao.valor());
                quantidade++;

            } catch (Exception e) {
                System.err.println("❌ Erro ao desserializar mensagem SQS: " + e.getMessage());
                throw new RuntimeException("Falha no processamento do lote", e);
            }
        }

        String resultado = String.format("Sucesso: %d transações processadas | Total: R$ %s",
                quantidade, totalProcessado.toPlainString());

        System.out.println("🏁 " + resultado);
        return resultado;
    }

    // Método auxiliar para inspecionarmos nos testes unitários
    public boolean isAquecido() {
        return this.aquecido;
    }
}
