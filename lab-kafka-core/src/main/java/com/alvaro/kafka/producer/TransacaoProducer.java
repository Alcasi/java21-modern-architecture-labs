package com.alvaro.kafka.producer;

import java.util.concurrent.CompletableFuture;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import com.alvaro.kafka.config.KafkaTopicConfig;
import com.alvaro.kafka.event.TransacaoEvento;

@Service
public class TransacaoProducer {

    private static final Logger log = LoggerFactory.getLogger(TransacaoProducer.class);

    private final KafkaTemplate<String, TransacaoEvento> kafkaTemplate;

    public TransacaoProducer(KafkaTemplate<String, TransacaoEvento> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public CompletableFuture<SendResult<String, TransacaoEvento>> enviar(TransacaoEvento evento) {
        // A REGRA DE OURO: Passamos evento.contaId() como CHAVE de partição!
        CompletableFuture<SendResult<String, TransacaoEvento>> future =
                kafkaTemplate.send(KafkaTopicConfig.TOPICO_TRANSACOES, evento.contaId(), evento);

        // Tratamento de callback assíncrono não-bloqueante
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                RecordMetadata metadata = result.getRecordMetadata();
                log.info("📤 Evento publicado! Transacao: {} | Conta: {} | Particao: {} | Offset: {}",
                        evento.transacaoId(), evento.contaId(), metadata.partition(), metadata.offset());
            } else {
                log.error("❌ Falha ao publicar evento da conta {}", evento.contaId(), ex);
            }
        });

        return future;
    }
}
