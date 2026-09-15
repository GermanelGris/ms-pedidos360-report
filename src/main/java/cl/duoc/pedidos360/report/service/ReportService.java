package cl.duoc.pedidos360.report.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.pedidos360.report.dto.ReportDtos.HourlySales;
import cl.duoc.pedidos360.report.dto.ReportDtos.KpiResponse;
import cl.duoc.pedidos360.report.dto.TopProduct;
import cl.duoc.pedidos360.report.model.OrderFact;
import cl.duoc.pedidos360.report.repository.OrderFactRepository;
import cl.duoc.pedidos360.report.repository.OrderItemFactRepository;

@Service
public class ReportService {

    private static final List<String> STATUS_ORDER =
            List.of("CREADO", "ACEPTADO", "EN_PREPARACION", "DESPACHADO", "ENTREGADO", "CANCELADO");
    private static final Set<String> CLOSED = Set.of("ENTREGADO", "CANCELADO");
    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00");

    private final OrderFactRepository orderFacts;
    private final OrderItemFactRepository orderItems;
    private final Clock clock;
    private final ZoneId zone;

    public ReportService(OrderFactRepository orderFacts, OrderItemFactRepository orderItems, Clock clock,
                         @Value("${report.zone}") String zone) {
        this.orderFacts = orderFacts;
        this.orderItems = orderItems;
        this.clock = clock;
        this.zone = ZoneId.of(zone);
    }

    @Transactional(readOnly = true)
    public KpiResponse kpis() {
        List<OrderFact> facts = orderFacts.findAll();

        long active = facts.stream().filter(f -> !CLOSED.contains(f.getStatus())).count();
        BigDecimal sales = facts.stream()
                .filter(f -> !"CANCELADO".equals(f.getStatus()))
                .map(OrderFact::getTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        OptionalDouble leadTime = facts.stream()
                .map(OrderFact::getLeadTimeMinutes)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .average();

        Map<String, Long> byStatus = new LinkedHashMap<>();
        STATUS_ORDER.forEach(status -> byStatus.put(status,
                facts.stream().filter(f -> status.equals(f.getStatus())).count()));

        Instant lastEvent = facts.stream()
                .map(OrderFact::getLastEventAt)
                .filter(Objects::nonNull)
                .max(Instant::compareTo)
                .orElse(null);

        return new KpiResponse(facts.size(), active, sales,
                leadTime.isPresent() ? Math.round(leadTime.getAsDouble() * 10) / 10.0 : null,
                byStatus, lastEvent);
    }

    /** Ventas por hora de las últimas {@code hours} horas (incluye horas sin ventas, para graficar). */
    @Transactional(readOnly = true)
    public List<HourlySales> salesByHour(int hours) {
        int range = Math.max(1, Math.min(hours, 168));
        ZonedDateTime currentHour = ZonedDateTime.now(clock.withZone(zone)).truncatedTo(ChronoUnit.HOURS);
        ZonedDateTime start = currentHour.minusHours(range - 1L);

        Map<ZonedDateTime, long[]> counts = new LinkedHashMap<>();
        Map<ZonedDateTime, BigDecimal> totals = new LinkedHashMap<>();
        for (int i = 0; i < range; i++) {
            ZonedDateTime hour = start.plusHours(i);
            counts.put(hour, new long[1]);
            totals.put(hour, BigDecimal.ZERO);
        }

        for (OrderFact fact : orderFacts.findByCreatedAtGreaterThanEqual(start.toInstant())) {
            if ("CANCELADO".equals(fact.getStatus()) || fact.getCreatedAt() == null) {
                continue;
            }
            ZonedDateTime hour = fact.getCreatedAt().atZone(zone).truncatedTo(ChronoUnit.HOURS);
            if (counts.containsKey(hour)) {
                counts.get(hour)[0]++;
                totals.merge(hour, fact.getTotal() != null ? fact.getTotal() : BigDecimal.ZERO, BigDecimal::add);
            }
        }

        List<HourlySales> result = new ArrayList<>();
        counts.forEach((hour, count) -> result.add(new HourlySales(hour.format(HOUR), count[0], totals.get(hour))));
        return result;
    }

    @Transactional(readOnly = true)
    public List<TopProduct> topProducts(int limit) {
        return orderItems.topProducts(PageRequest.of(0, Math.max(1, Math.min(limit, 20))));
    }
}
