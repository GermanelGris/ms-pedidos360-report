package cl.duoc.pedidos360.report.messaging;

import java.io.IOException;

import com.rabbitmq.client.Channel;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import cl.duoc.pedidos360.report.config.RabbitMessagingProperties;
import cl.duoc.pedidos360.report.service.ProjectionService;

/** Consumidor del dominio reportería: cola {@code pedidos360.rabbitmq.orders.queue}, alimenta los KPIs. */
@Component
public class OrderEventsListener {

    private final EventAcknowledger acknowledger;
    private final ProjectionService projection;
    private final RabbitMessagingProperties.Subscription subscription;

    public OrderEventsListener(EventAcknowledger acknowledger, ProjectionService projection,
                               RabbitMessagingProperties properties) {
        this.acknowledger = acknowledger;
        this.projection = projection;
        this.subscription = properties.orders();
    }

    @RabbitListener(id = "report-order-events", queues = "${pedidos360.rabbitmq.orders.queue}")
    public void onMessage(Message message, Channel channel) throws IOException {
        acknowledger.process(subscription, message, channel, projection::apply);
    }
}
