package com.alvaro.kafka.consumer;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.listener.ConsumerAwareRebalanceListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaRebalanceListener implements ConsumerAwareRebalanceListener {

    private final List<TopicPartition> particoesAtribuidas = new CopyOnWriteArrayList<>();
    private static final Logger log = LoggerFactory.getLogger(KafkaRebalanceListener.class);

    @Override
    public void onPartitionsRevokedBeforeCommit(Consumer<?, ?> consumer, Collection<TopicPartition> partitions) {

        log.warn("Partições devolvidas {} para o grupo {}", partitions,
                consumer.groupMetadata().groupId());
        particoesAtribuidas.removeAll(partitions);

    }

    @Override
    public void onPartitionsAssigned(Consumer<?, ?> consumer, Collection<TopicPartition> partitions) {

        log.info("Partições recebidas {} para o grupo {}", partitions, consumer.groupMetadata().groupId());
        particoesAtribuidas.addAll(partitions);

    }

    @Override
    public void onPartitionsLost(Consumer<?, ?> consumer, Collection<TopicPartition> partitions) {

        log.error("🚨 [REBALANCE] Partições perdidas abruptamente: {} no grupo: {}",
                partitions, consumer.groupMetadata().groupId());
        particoesAtribuidas.clear();

    }

    public List<TopicPartition> getParticoesAtribuidas() {
        return particoesAtribuidas;
    }

}
