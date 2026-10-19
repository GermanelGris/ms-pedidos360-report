package cl.duoc.pedidos360.report.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;

import com.rabbitmq.client.Channel;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import tools.jackson.databind.json.JsonMapper;

import cl.duoc.pedidos360.report.config.RabbitMessagingProperties;
import cl.duoc.pedidos360.report.config.RabbitMessagingProperties.Exchanges;
import cl.duoc.pedidos360.report.config.RabbitMessagingProperties.Retry;
import cl.duoc.pedidos360.report.config.RabbitMessagingProperties.Subscription;

class EventAcknowledgerTest {

    private static final Subscription ORDERS =
            new Subscription("q.report.orders", "q.report.orders.retry", "q.report.orders.dlq", "order.*");
    private static final RabbitMessagingProperties PROPERTIES = new RabbitMessagingProperties(
            new Exchanges("orders.events", "report.retry", "report.dlx"), new Retry(3, 5000), ORDERS);
    private static final String VALID = """
            {"eventId":"evt-1","type":"OrderCreated","order":{"id":7,"status":"CREADO","total":100}}
            """;

    private final Channel channel = mock(Channel.class);
    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final EventAcknowledger acknowledger =
            new EventAcknowledger(JsonMapper.builder().build(), rabbit, PROPERTIES);

    private static Message message(String body, Integer retryCount) {
        MessageBuilder builder = MessageBuilder.withBody(body.getBytes(StandardCharsets.UTF_8));
        if (retryCount != null) {
            builder.setHeader(EventAcknowledger.RETRY_HEADER, retryCount);
        }
        Message message = builder.build();
        message.getMessageProperties().setDeliveryTag(42L);
        return message;
    }

    @Test
    void eventoAplicadoHaceAck() throws Exception {
        acknowledger.process(ORDERS, message(VALID, null), channel, event -> true);

        verify(channel).basicAck(42L, false);
    }

    @Test
    void eventoDuplicadoTambienHaceAck() throws Exception {
        acknowledger.process(ORDERS, message(VALID, null), channel, event -> false);

        verify(channel).basicAck(42L, false);
        verify(channel, never()).basicNack(any(Long.class), any(Boolean.class), any(Boolean.class));
    }

    @Test
    void jsonInvalidoVaDirectoALaDlq() throws Exception {
        acknowledger.process(ORDERS, message("no es json", null), channel, event -> true);

        verify(channel).basicNack(42L, false, false);
        verify(rabbit, never()).send(anyString(), anyString(), any(Message.class));
    }

    @Test
    void eventoSinPedidoVaDirectoALaDlq() throws Exception {
        acknowledger.process(ORDERS, message("{\"eventId\":\"evt-1\"}", null), channel, event -> true);

        verify(channel).basicNack(42L, false, false);
    }

    @Test
    void errorRecuperableSeReintentaPorLaColaRetry() throws Exception {
        acknowledger.process(ORDERS, message(VALID, null), channel, event -> {
            throw new IllegalStateException("La base de datos no responde");
        });

        verify(rabbit).send(eq("report.retry"), eq("q.report.orders"), any(Message.class));
        verify(channel).basicAck(42L, false);
    }

    @Test
    void sinIntentosDisponiblesVaALaDlq() throws Exception {
        acknowledger.process(ORDERS, message(VALID, 2), channel, event -> {
            throw new IllegalStateException("La base de datos no responde");
        });

        verify(channel).basicNack(42L, false, false);
        verify(rabbit, never()).send(anyString(), anyString(), any(Message.class));
    }
}
