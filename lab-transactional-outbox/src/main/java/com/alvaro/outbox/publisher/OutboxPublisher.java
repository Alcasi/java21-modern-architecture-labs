package com.alvaro.outbox.publisher;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.alvaro.outbox.domain.OutboxEvento;
import com.alvaro.outbox.repository.OutboxEventoRepository;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    public static final String TOPICO_PAGAMENTOS = "pagamentos-stream";

    private final OutboxEventoRepository outboxEventoRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(OutboxEventoRepository outboxEventoRepository,
            KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventoRepository = outboxEventoRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public int publicarEventosPendentes() {
        List<OutboxEvento> pendentes = outboxEventoRepository.findByStatus("PENDING");
        if (pendentes.isEmpty()) {
            return 0;
        }

        log.info("🚀 Despachando {} evento(s) pendente(s) da Outbox para o Kafka...", pendentes.size());

        for (OutboxEvento evento : pendentes) {
            // aggregateId como chave da partição garante ordem estrita no Kafka!
            kafkaTemplate.send(TOPICO_PAGAMENTOS, evento.getAggregateId(), evento.getPayload());
            evento.marcarComoProcessado();
            outboxEventoRepository.save(evento);
            log.info("✅ Evento {} enviado com sucesso ao Kafka e marcado como PROCESSED", evento.getId());
        }

        return pendentes.size();
    }
}
