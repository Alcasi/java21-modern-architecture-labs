package com.alvaro.eventbridge.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.alvaro.eventbridge.event.TransacaoFinanceiraEvento;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

@Component
public class EventBridgePublisher {

    private static final Logger log = LoggerFactory.getLogger(EventBridgePublisher.class);

    private final EventBridgeClient eventBridgeClient;
    private final ObjectMapper objectMapper;
    private final String busName;

    public EventBridgePublisher(EventBridgeClient eventBridgeClient,
            ObjectMapper objectMapper,
            @Value("${aws.eventbridge.bus-name}") String busName) {
        this.eventBridgeClient = eventBridgeClient;
        this.objectMapper = objectMapper;
        this.busName = busName;
    }

    public String publicar(TransacaoFinanceiraEvento evento) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(evento);

            PutEventsRequestEntry entry = PutEventsRequestEntry.builder()
                    .eventBusName(busName)
                    .source("banking.pagamentos")
                    .detailType("TransacaoProcessada")
                    .detail(jsonPayload)
                    .time(evento.timestamp())
                    .build();

            PutEventsRequest request = PutEventsRequest.builder()
                    .entries(entry)
                    .build();

            log.info("🚀 Publicando evento no Amazon EventBridge (Bus: {}): TransacaoId={}", busName,
                    evento.transacaoId());
            PutEventsResponse response = eventBridgeClient.putEvents(request);

            String eventId = response.entries().getFirst().eventId();
            log.info("✅ Evento aceito pelo EventBridge com ID AWS: {}", eventId);
            return eventId;

        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Falha ao serializar evento para o EventBridge", e);
        }
    }
}
