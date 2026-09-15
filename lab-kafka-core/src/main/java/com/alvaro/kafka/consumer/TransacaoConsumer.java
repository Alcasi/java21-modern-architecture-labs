package com.alvaro.kafka.consumer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.alvaro.kafka.config.KafkaTopicConfig;
import com.alvaro.kafka.event.TransacaoEvento;

@Component
public class TransacaoConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransacaoConsumer.class);

    // Lista thread-safe para guardarmos os eventos recebidos durante os testes
    private final List<TransacaoEvento> eventosRecebidos = new CopyOnWriteArrayList<>();

    @KafkaListener(topics = KafkaTopicConfig.TOPICO_TRANSACOES, groupId = "core-banking-group")
    public void consumir(
            @Payload TransacaoEvento evento,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int particao,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_KEY) String chave) {

        log.info("📥 Evento consumido! Transacao: {} | Conta: {} | Chave: {} | Particao: {} | Offset: {}",
                evento.transacaoId(), evento.contaId(), chave, particao, offset);

        eventosRecebidos.add(evento);
    }

    public List<TransacaoEvento> getEventosRecebidos() {
        return eventosRecebidos;
    }
}
