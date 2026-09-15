package cl.duoc.pedidos360.report.model;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Proyección de lectura del pedido, construida a partir de los eventos de Kafka. */
@Entity
@Table(name = "order_facts")
public class OrderFact {

    @Id
    private Long orderId;

    @Column(length = 100)
    private String customerId;

    @Column(length = 150)
    private String customerName;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    private Instant createdAt;

    private Instant deliveredAt;

    private Long leadTimeMinutes;

    @Column(nullable = false)
    private Instant lastEventAt;

    protected OrderFact() {
    }

    public OrderFact(Long orderId) {
        this.orderId = orderId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(Instant deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public Long getLeadTimeMinutes() {
        return leadTimeMinutes;
    }

    public void setLeadTimeMinutes(Long leadTimeMinutes) {
        this.leadTimeMinutes = leadTimeMinutes;
    }

    public Instant getLastEventAt() {
        return lastEventAt;
    }

    public void setLastEventAt(Instant lastEventAt) {
        this.lastEventAt = lastEventAt;
    }
}
