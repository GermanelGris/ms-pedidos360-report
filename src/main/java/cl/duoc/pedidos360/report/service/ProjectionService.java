package cl.duoc.pedidos360.report.service;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.pedidos360.report.messaging.OrderEventMessage;
import cl.duoc.pedidos360.report.messaging.OrderEventMessage.OrderSnapshot;
import cl.duoc.pedidos360.report.model.OrderFact;
import cl.duoc.pedidos360.report.model.OrderItemFact;
import cl.duoc.pedidos360.report.model.ProcessedEvent;
import cl.duoc.pedidos360.report.repository.OrderFactRepository;
import cl.duoc.pedidos360.report.repository.OrderItemFactRepository;
import cl.duoc.pedidos360.report.repository.ProcessedEventRepository;

/** Aplica los eventos de orders.events sobre la proyección de lectura (idempotente). */
@Service
public class ProjectionService {

    private final OrderFactRepository orderFacts;
    private final OrderItemFactRepository orderItems;
    private final ProcessedEventRepository processedEvents;
    private final Clock clock;

    public ProjectionService(OrderFactRepository orderFacts, OrderItemFactRepository orderItems,
                             ProcessedEventRepository processedEvents, Clock clock) {
        this.orderFacts = orderFacts;
        this.orderItems = orderItems;
        this.processedEvents = processedEvents;
        this.clock = clock;
    }

    /** @return true si el evento se aplicó, false si ya había sido procesado. */
    @Transactional
    public boolean apply(OrderEventMessage event) {
        if (event.eventId() == null || event.order() == null || event.order().id() == null) {
            // Lanza excepción: tras los reintentos el mensaje termina en la DLT
            throw new IllegalArgumentException("Evento de pedido incompleto: " + event.eventId());
        }
        if (processedEvents.existsById(event.eventId())) {
            return false;
        }

        OrderSnapshot order = event.order();
        Instant eventTime = event.occurredAt() != null ? event.occurredAt() : Instant.now(clock);
        OrderFact fact = orderFacts.findById(order.id()).orElseGet(() -> new OrderFact(order.id()));

        // Si llega un evento más antiguo que el último aplicado, no retrocede el estado
        if (fact.getLastEventAt() == null || !eventTime.isBefore(fact.getLastEventAt())) {
            fact.setCustomerId(order.customerId());
            fact.setCustomerName(order.customerName());
            fact.setStatus(order.status());
            fact.setTotal(order.total());
            fact.setCreatedAt(order.createdAt());
            fact.setDeliveredAt(order.deliveredAt());
            fact.setLeadTimeMinutes(order.leadTimeMinutes());
            fact.setLastEventAt(eventTime);
            orderFacts.save(fact);
        }

        if (order.items() != null && !order.items().isEmpty() && !orderItems.existsByOrderId(order.id())) {
            orderItems.saveAll(order.items().stream()
                    .map(item -> new OrderItemFact(order.id(), item.productId(), item.productName(), item.quantity(),
                            item.subtotal()))
                    .toList());
        }

        processedEvents.save(new ProcessedEvent(event.eventId(), Instant.now(clock)));
        return true;
    }
}
