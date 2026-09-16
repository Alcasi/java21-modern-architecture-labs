package com.alvaro.idempotence.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.alvaro.idempotence.consumer.PagamentoConsumer;
import com.alvaro.idempotence.event.PagamentoEvento;

@Component
public class PagamentoProducer {

    private static final Logger log = LoggerFactory.getLogger(PagamentoProducer.class);

    private final KafkaTemplate<String, PagamentoEvento> kafkaTemplate;

    public PagamentoProducer(KafkaTemplate<String, PagamentoEvento> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void enviar(PagamentoEvento evento) {
        log.info("📤 Publicando evento no Kafka: {} para conta {}", evento.transacaoId(), evento.contaOrigem());
        kafkaTemplate.send(PagamentoConsumer.TOPICO_PAGAMENTOS, evento.contaOrigem(), evento);
    }
}
