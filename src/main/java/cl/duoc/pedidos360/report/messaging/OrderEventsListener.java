package cl.duoc.pedidos360.report.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

import cl.duoc.pedidos360.report.config.KafkaConfig;
import cl.duoc.pedidos360.report.service.ProjectionService;

/** Consume orders.events sin bloquear el core: la reportería se alimenta de forma asíncrona. */
@Component
public class OrderEventsListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventsListener.class);

    private final ObjectMapper mapper;
    private final ProjectionService projection;

    public OrderEventsListener(ObjectMapper mapper, ProjectionService projection) {
        this.mapper = mapper;
        this.projection = projection;
    }

    @KafkaListener(topics = KafkaConfig.ORDERS_EVENTS)
    public void onOrderEvent(String payload) {
        OrderEventMessage event = mapper.readValue(payload, OrderEventMessage.class);
        boolean applied = projection.apply(event);
        log.info("{} pedido #{} {}", event.type(), event.order() != null ? event.order().id() : null,
                applied ? "proyectado" : "ya procesado (idempotencia)");
    }
}
