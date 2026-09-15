package cl.duoc.pedidos360.report.config;

import java.time.Clock;
import java.time.Duration;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    public static final String ORDERS_EVENTS = "orders.events";

    /** Dead Letter Topic propio de este consumidor. */
    public static final String DEAD_LETTER_TOPIC = "orders.events.report.DLT";

    @Bean
    NewTopic ordersEventsTopic() {
        return TopicBuilder.name(ORDERS_EVENTS)
                .partitions(3)
                .replicas(1)
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(Duration.ofDays(7).toMillis()))
                .build();
    }

    @Bean
    NewTopic reportDeadLetterTopic() {
        return TopicBuilder.name(DEAD_LETTER_TOPIC)
                .partitions(3)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(Duration.ofDays(14).toMillis()))
                .build();
    }

    /**
     * Reintenta 2 veces (1 s entre intentos) y luego publica en la DLT el mensaje original
     * con metadatos del error (excepción, stacktrace, tópico, partición y offset originales).
     */
    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<?, ?> template) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template,
                (record, ex) -> new TopicPartition(DEAD_LETTER_TOPIC, record.partition()));
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2L));
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
