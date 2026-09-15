package cl.duoc.pedidos360.report.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Evento de negocio de un pedido (OrderCreated, OrderAccepted, ...).
 * Contrato definido; el consumidor aún no está implementado (fuera del alcance de la EP1).
 */
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
