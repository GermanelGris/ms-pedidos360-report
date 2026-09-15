package cl.duoc.pedidos360.report.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/** Respuestas de la API de reportería. */
public final class ReportDtos {

    private ReportDtos() {
    }

    public record KpiResponse(long totalOrders, long activeOrders, BigDecimal salesTotal, Double avgLeadTimeMinutes,
                              Map<String, Long> ordersByStatus, Instant lastEventAt) {
    }

    public record HourlySales(String hour, long orders, BigDecimal sales) {
    }
}
