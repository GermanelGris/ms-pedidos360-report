package cl.duoc.pedidos360.report.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** eventId ya aplicados: garantiza idempotencia si Kafka reentrega un mensaje. */
@Entity
@Table(name = "processed_events")
public class ProcessedEvent {

    @Id
    @Column(length = 64)
    private String eventId;

    @Column(nullable = false)
    private Instant processedAt;

    protected ProcessedEvent() {
    }

    public ProcessedEvent(String eventId, Instant processedAt) {
        this.eventId = eventId;
        this.processedAt = processedAt;
    }

    public String getEventId() {
        return eventId;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
