package cl.duoc.pedidos360.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cl.duoc.pedidos360.report.messaging.OrderEventMessage;
import cl.duoc.pedidos360.report.messaging.OrderEventMessage.ItemSnapshot;
import cl.duoc.pedidos360.report.messaging.OrderEventMessage.OrderSnapshot;
import cl.duoc.pedidos360.report.model.OrderFact;
import cl.duoc.pedidos360.report.model.ProcessedEvent;
import cl.duoc.pedidos360.report.repository.OrderFactRepository;
import cl.duoc.pedidos360.report.repository.OrderItemFactRepository;
import cl.duoc.pedidos360.report.repository.ProcessedEventRepository;

@ExtendWith(MockitoExtension.class)
class ProjectionServiceTest {

    @Mock
    private OrderFactRepository orderFacts;

    @Mock
    private OrderItemFactRepository orderItems;

    @Mock
    private ProcessedEventRepository processedEvents;

    private ProjectionService service;

    @BeforeEach
    void setUp() {
        service = new ProjectionService(orderFacts, orderItems, processedEvents,
                Clock.fixed(Instant.parse("2026-09-14T15:00:00Z"), ZoneOffset.UTC));
    }

    private static OrderEventMessage event(String eventId, String status, String occurredAt) {
        OrderSnapshot order = new OrderSnapshot(7L, "cliente-1", "Cliente Uno", status, new BigDecimal("17980"),
                Instant.parse("2026-09-14T14:00:00Z"), null, null,
                List.of(new ItemSnapshot(1L, "Pizza", 2, new BigDecimal("8990"), new BigDecimal("17980"))));
        return new OrderEventMessage(eventId, "OrderAccepted", Instant.parse(occurredAt), "trace", "order-7",
                "ms-pedidos360-orders", order);
    }

    @Test
    void eventoNuevoActualizaLaProyeccionYGuardaItems() {
        given(processedEvents.existsById("evt-1")).willReturn(false);
        given(orderFacts.findById(7L)).willReturn(Optional.empty());
        given(orderItems.existsByOrderId(7L)).willReturn(false);

        boolean applied = service.apply(event("evt-1", "ACEPTADO", "2026-09-14T14:05:00Z"));

        assertThat(applied).isTrue();
        ArgumentCaptor<OrderFact> fact = ArgumentCaptor.forClass(OrderFact.class);
        verify(orderFacts).save(fact.capture());
        assertThat(fact.getValue().getStatus()).isEqualTo("ACEPTADO");
        assertThat(fact.getValue().getTotal()).isEqualByComparingTo("17980");
        verify(orderItems).saveAll(anyList());
        verify(processedEvents).save(any(ProcessedEvent.class));
    }

    @Test
    void eventoDuplicadoSeIgnora() {
        given(processedEvents.existsById("evt-1")).willReturn(true);

        assertThat(service.apply(event("evt-1", "ACEPTADO", "2026-09-14T14:05:00Z"))).isFalse();
        verifyNoInteractions(orderFacts, orderItems);
    }

    @Test
    void eventoAntiguoNoRetrocedeElEstado() {
        OrderFact current = new OrderFact(7L);
        current.setStatus("DESPACHADO");
        current.setLastEventAt(Instant.parse("2026-09-14T14:30:00Z"));
        given(processedEvents.existsById("evt-viejo")).willReturn(false);
        given(orderFacts.findById(7L)).willReturn(Optional.of(current));
        given(orderItems.existsByOrderId(7L)).willReturn(true);

        service.apply(event("evt-viejo", "ACEPTADO", "2026-09-14T14:05:00Z"));

        assertThat(current.getStatus()).isEqualTo("DESPACHADO");
        verify(orderFacts, never()).save(any(OrderFact.class));
    }

    @Test
    void eventoIncompletoLanzaErrorParaIrALaDlt() {
        OrderEventMessage broken = new OrderEventMessage("evt-2", "OrderCreated", null, null, null, null, null);

        assertThatThrownBy(() -> service.apply(broken)).isInstanceOf(IllegalArgumentException.class);
    }
}
