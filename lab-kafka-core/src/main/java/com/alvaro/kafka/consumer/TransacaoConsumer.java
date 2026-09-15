package com.alvaro.kafka.consumer;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import com.alvaro.kafka.config.KafkaTopicConfig;
import com.alvaro.kafka.event.TransacaoEvento;

@Component
public class TransacaoConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransacaoConsumer.class);

    // Lista thread-safe para guardarmos os eventos recebidos durante os testes
    private final List<TransacaoEvento> eventosRecebidos = new CopyOnWriteArrayList<>();
    private final List<TransacaoEvento> eventosDlt = new CopyOnWriteArrayList<>();

    @RetryableTopic(attempts = "3", backoff = @Backoff(delay = 1000, multiplier = 1.5), autoCreateTopics = "true", topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE)
    @KafkaListener(topics = KafkaTopicConfig.TOPICO_TRANSACOES, groupId = "core-banking-group")
    public void consumir(
            @Payload TransacaoEvento evento,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int particao,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_KEY) String chave) {

        if (evento.valor().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Valor inválido detectado! Lançado erro para acionar retry: {}", evento.valor());
            throw new IllegalArgumentException("Valor da transação inválido: " + evento.valor());
        }

        log.info("📥 Evento consumido! Transacao: {} | Conta: {} | Chave: {} | Particao: {} | Offset: {}",
                evento.transacaoId(), evento.contaId(), chave, particao, offset);

        eventosRecebidos.add(evento);
    }

    public List<TransacaoEvento> getEventosRecebidos() {
        return eventosRecebidos;
    }

    @DltHandler
    public void processarDlt(
            @Payload TransacaoEvento evento,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topico) {

        log.error("🚨 [DLT ACIONADA] Evento enviado para a Dead Letter Queue! TransacaoId: {} no topico {}",
                evento.transacaoId(), topico);

        eventosDlt.add(evento);
    }

    public List<TransacaoEvento> getEventosDlt() {
        return eventosDlt;
    }

}
