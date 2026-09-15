package cl.duoc.pedidos360.report.messaging;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Evento del tópico orders.events (solo los campos que usa la reportería). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderEventMessage(String eventId, String type, Instant occurredAt, String traceId,
                                String correlationId, String source, OrderSnapshot order) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OrderSnapshot(Long id, String customerId, String customerName, String status, BigDecimal total,
                                Instant createdAt, Instant deliveredAt, Long leadTimeMinutes,
                                List<ItemSnapshot> items) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ItemSnapshot(Long productId, String productName, int quantity, BigDecimal unitPrice,
                               BigDecimal subtotal) {
    }
}
