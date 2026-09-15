package com.alvaro.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ContainerCustomizer;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

import com.alvaro.kafka.consumer.KafkaRebalanceListener;

@Configuration
public class KafkaTopicConfig {

    public static final String TOPICO_TRANSACOES = "transacoes-financeiras";

    @Bean
    public NewTopic transacoesTopic() {
        return TopicBuilder.name(TOPICO_TRANSACOES)
                .partitions(3) // ◄◄ 3 partições para demonstrar o hashing da chave
                .replicas(1)
                .build();
    }

    @Bean
    public ContainerCustomizer<Object, Object, ConcurrentMessageListenerContainer<Object, Object>> containerCustomizer(
            KafkaRebalanceListener rebalanceListener) {
        return container -> container.getContainerProperties().setConsumerRebalanceListener(rebalanceListener);
    }

}
