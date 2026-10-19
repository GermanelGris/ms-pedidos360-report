package cl.duoc.pedidos360.report.messaging;

import java.io.IOException;
import java.util.function.Predicate;

import com.rabbitmq.client.Channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import cl.duoc.pedidos360.report.config.RabbitMessagingProperties;
import cl.duoc.pedidos360.report.config.RabbitMessagingProperties.Subscription;

/**
 * Política de confirmación del consumidor (acknowledge-mode: manual).
 *
 * <ul>
 *   <li><b>Aplicado o duplicado</b> (eventId ya proyectado): basicAck.</li>
 *   <li><b>Error no recuperable</b> ({@link NonRecoverableMessageException}: JSON inválido, sin eventId o sin
 *       pedido): basicNack(requeue=false), RabbitMQ lo mueve a la DLQ al instante.</li>
 *   <li><b>Error recuperable</b> (cualquier otra excepción, p. ej. la base de datos no responde) con intentos
 *       disponibles: se republica en la cola .retry con el header {@value #RETRY_HEADER} y se hace basicAck del
 *       original; al vencer el TTL vuelve a la cola principal.</li>
 *   <li><b>Error recuperable sin intentos disponibles:</b> basicNack(requeue=false), DLQ.</li>
 * </ul>
 *
 * Nunca se usa requeue=true: un mensaje que siempre falla quedaría en un bucle infinito.
 */
@Component
public class EventAcknowledger {

    public static final String RETRY_HEADER = "x-retry-count";

    private static final Logger log = LoggerFactory.getLogger(EventAcknowledger.class);

    private final ObjectMapper mapper;
    private final RabbitTemplate rabbit;
    private final RabbitMessagingProperties properties;

    public EventAcknowledger(ObjectMapper mapper, RabbitTemplate rabbit, RabbitMessagingProperties properties) {
        this.mapper = mapper;
        this.rabbit = rabbit;
        this.properties = properties;
    }

    /** @param handler aplica el evento; devuelve false si ya había sido procesado (idempotencia). */
    public void process(Subscription subscription, Message message, Channel channel,
                        Predicate<OrderEventMessage> handler) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            OrderEventMessage event = parse(message);
            boolean applied = handler.test(event);
            channel.basicAck(deliveryTag, false);
            log.info("[{}] {} pedido #{} {}", subscription.queue(), event.type(), event.order().id(),
                    applied ? "proyectado" : "duplicado (ACK sin reprocesar)");
        } catch (NonRecoverableMessageException ex) {
            log.error("[{}] Mensaje no recuperable, NACK -> {}: {}", subscription.queue(), subscription.dlq(),
                    ex.getMessage());
            channel.basicNack(deliveryTag, false, false);
        } catch (RuntimeException ex) {
            retryOrDeadLetter(subscription, message, channel, deliveryTag, ex);
        }
    }

    private void retryOrDeadLetter(Subscription subscription, Message message, Channel channel, long deliveryTag,
                                   RuntimeException error) throws IOException {
        int attempt = retryCount(message) + 1;
        int maxAttempts = properties.retry().maxAttempts();
        if (attempt >= maxAttempts) {
            log.error("[{}] Intento {}/{} fallido, sin reintentos disponibles, NACK -> {}: {}",
                    subscription.queue(), attempt, maxAttempts, subscription.dlq(), error.getMessage());
            channel.basicNack(deliveryTag, false, false);
            return;
        }
        try {
            Message retry = MessageBuilder.fromMessage(message).setHeader(RETRY_HEADER, attempt).build();
            rabbit.send(properties.exchanges().retry(), subscription.queue(), retry);
            channel.basicAck(deliveryTag, false);
            log.warn("[{}] Intento {}/{} fallido, se reintenta en {} ms vía {}: {}", subscription.queue(), attempt,
                    maxAttempts, properties.retry().delayMs(), subscription.retryQueue(), error.getMessage());
        } catch (AmqpException publishError) {
            log.error("[{}] No se pudo programar el reintento, NACK -> {}: {}", subscription.queue(),
                    subscription.dlq(), publishError.getMessage());
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private OrderEventMessage parse(Message message) {
        OrderEventMessage event;
        try {
            event = mapper.readValue(message.getBody(), OrderEventMessage.class);
        } catch (JacksonException ex) {
            throw new NonRecoverableMessageException("El cuerpo no es un evento JSON válido", ex);
        }
        if (event.eventId() == null || event.eventId().isBlank()) {
            throw new NonRecoverableMessageException("El evento no tiene eventId");
        }
        if (event.order() == null || event.order().id() == null) {
            throw new NonRecoverableMessageException("El evento " + event.eventId() + " no trae el pedido");
        }
        return event;
    }

    static int retryCount(Message message) {
        Object value = message.getMessageProperties().getHeader(RETRY_HEADER);
        return value instanceof Number number ? number.intValue() : 0;
    }
}
