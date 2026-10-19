package cl.duoc.pedidos360.report.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Caso de uso: alimentar la proyección de KPIs con los eventos de pedidos.
 *
 * <pre>
 * orders.events  --order.*------------->  q.report.orders        (si falla sin remedio: NACK -> report.dlx)
 * report.retry   --q.report.orders----->  q.report.orders.retry  (TTL; al expirar vuelve SOLO a q.report.orders
 *                                                                  por el exchange por defecto)
 * report.dlx     --q.report.orders----->  q.report.orders.dlq
 * </pre>
 *
 * El reintento no vuelve a pasar por orders.events: así ms-pedidos360-audit no recibe el mensaje dos veces.
 */
@Configuration
public class RabbitTopologyConfig {

    private final RabbitMessagingProperties properties;
    private final RabbitMessagingProperties.Subscription orders;

    public RabbitTopologyConfig(RabbitMessagingProperties properties) {
        this.properties = properties;
        this.orders = properties.orders();
    }

    // ---- Exchanges ----

    /** Lo declara también ms-pedidos360-orders; la declaración es idempotente si coinciden los argumentos. */
    @Bean
    TopicExchange orderEventsExchange() {
        return ExchangeBuilder.topicExchange(properties.exchanges().events()).durable(true).build();
    }

    @Bean
    DirectExchange retryExchange() {
        return ExchangeBuilder.directExchange(properties.exchanges().retry()).durable(true).build();
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange(properties.exchanges().deadLetter()).durable(true).build();
    }

    // ---- Colas ----

    @Bean
    Queue ordersQueue() {
        return QueueBuilder.durable(orders.queue())
                .deadLetterExchange(properties.exchanges().deadLetter())
                .deadLetterRoutingKey(orders.queue())
                .build();
    }

    @Bean
    Queue ordersRetryQueue() {
        return QueueBuilder.durable(orders.retryQueue())
                .ttl(properties.retry().delayMs())
                .deadLetterExchange("")               // exchange por defecto: entrega directa a la cola nombrada
                .deadLetterRoutingKey(orders.queue())
                .build();
    }

    @Bean
    Queue ordersDeadLetterQueue() {
        return QueueBuilder.durable(orders.dlq()).build();
    }

    // ---- Bindings ----

    @Bean
    Binding ordersBinding(@Qualifier("orderEventsExchange") TopicExchange events) {
        return BindingBuilder.bind(ordersQueue()).to(events).with(orders.bindingKey());
    }

    @Bean
    Binding ordersRetryBinding(@Qualifier("retryExchange") DirectExchange retry) {
        return BindingBuilder.bind(ordersRetryQueue()).to(retry).with(orders.queue());
    }

    @Bean
    Binding ordersDeadLetterBinding(@Qualifier("deadLetterExchange") DirectExchange deadLetter) {
        return BindingBuilder.bind(ordersDeadLetterQueue()).to(deadLetter).with(orders.queue());
    }
}
