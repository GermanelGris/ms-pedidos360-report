package cl.duoc.pedidos360.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cl.duoc.pedidos360.report.dto.ReportDtos.HourlySales;
import cl.duoc.pedidos360.report.dto.ReportDtos.KpiResponse;
import cl.duoc.pedidos360.report.model.OrderFact;
import cl.duoc.pedidos360.report.repository.OrderFactRepository;
import cl.duoc.pedidos360.report.repository.OrderItemFactRepository;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private OrderFactRepository orderFacts;

    @Mock
    private OrderItemFactRepository orderItems;

    private ReportService service;

    @BeforeEach
    void setUp() {
        service = new ReportService(orderFacts, orderItems,
                Clock.fixed(Instant.parse("2026-09-14T12:45:00Z"), ZoneOffset.UTC), "UTC");
    }

    private static OrderFact fact(long id, String status, String total, String createdAt, Long leadTime) {
        OrderFact fact = new OrderFact(id);
        fact.setStatus(status);
        fact.setTotal(new BigDecimal(total));
        fact.setCreatedAt(Instant.parse(createdAt));
        fact.setLeadTimeMinutes(leadTime);
        fact.setLastEventAt(Instant.parse(createdAt));
        return fact;
    }

    @Test
    void calculaKpis() {
        given(orderFacts.findAll()).willReturn(List.of(
                fact(1, "CREADO", "10000", "2026-09-14T12:10:00Z", null),
                fact(2, "ENTREGADO", "20000", "2026-09-14T11:00:00Z", 30L),
                fact(3, "ENTREGADO", "5000", "2026-09-14T10:00:00Z", 45L),
                fact(4, "CANCELADO", "99000", "2026-09-14T09:00:00Z", null)));

        KpiResponse kpis = service.kpis();

        assertThat(kpis.totalOrders()).isEqualTo(4);
        assertThat(kpis.activeOrders()).isEqualTo(1);
        assertThat(kpis.salesTotal()).isEqualByComparingTo("35000");
        assertThat(kpis.avgLeadTimeMinutes()).isEqualTo(37.5);
        assertThat(kpis.ordersByStatus()).containsEntry("ENTREGADO", 2L).containsEntry("CANCELADO", 1L);
    }

    @Test
    void agrupaVentasPorHoraIncluyendoHorasSinVentas() {
        given(orderFacts.findByCreatedAtGreaterThanEqual(any())).willReturn(List.of(
                fact(1, "CREADO", "10000", "2026-09-14T12:10:00Z", null),
                fact(2, "ACEPTADO", "5000", "2026-09-14T12:40:00Z", null),
                fact(3, "ENTREGADO", "7000", "2026-09-14T10:05:00Z", 20L),
                fact(4, "CANCELADO", "99000", "2026-09-14T12:20:00Z", null)));

        List<HourlySales> sales = service.salesByHour(3);

        assertThat(sales).extracting(HourlySales::hour)
                .containsExactly("2026-09-14 10:00", "2026-09-14 11:00", "2026-09-14 12:00");
        assertThat(sales).extracting(HourlySales::orders).containsExactly(1L, 0L, 2L);
        assertThat(sales.get(2).sales()).isEqualByComparingTo("15000");
    }
}
