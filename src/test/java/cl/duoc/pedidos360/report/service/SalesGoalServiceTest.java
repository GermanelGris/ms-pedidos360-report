package cl.duoc.pedidos360.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import cl.duoc.pedidos360.report.dto.GoalDtos.CreateGoalRequest;
import cl.duoc.pedidos360.report.dto.GoalDtos.GoalResponse;
import cl.duoc.pedidos360.report.model.SalesGoal;
import cl.duoc.pedidos360.report.repository.OrderFactRepository;
import cl.duoc.pedidos360.report.repository.SalesGoalRepository;

@ExtendWith(MockitoExtension.class)
class SalesGoalServiceTest {

    @Mock
    private SalesGoalRepository goals;

    @Mock
    private OrderFactRepository orderFacts;

    private SalesGoalService service;

    @BeforeEach
    void setUp() {
        service = new SalesGoalService(goals, orderFacts,
                Clock.fixed(Instant.parse("2026-10-19T12:00:00Z"), ZoneOffset.UTC), "UTC");
    }

    private static SalesGoal goal(String amount) {
        return new SalesGoal("Octubre", new BigDecimal(amount), LocalDate.parse("2026-10-01"),
                LocalDate.parse("2026-10-31"), "Admin", Instant.parse("2026-10-01T00:00:00Z"));
    }

    @Test
    void calculaElAvanceConLasVentasDelRangoInclusive() {
        given(goals.findAllByOrderByEndDateDescIdDesc()).willReturn(List.of(goal("200000")));
        given(orderFacts.salesBetween(Instant.parse("2026-10-01T00:00:00Z"), Instant.parse("2026-11-01T00:00:00Z")))
                .willReturn(new BigDecimal("50000"));

        GoalResponse response = service.list().get(0);

        assertThat(response.achieved()).isEqualByComparingTo("50000");
        assertThat(response.progressPercent()).isEqualTo(25.0);
    }

    @Test
    void elAvancePuedeSuperarElCienPorCiento() {
        given(goals.findAllByOrderByEndDateDescIdDesc()).willReturn(List.of(goal("100000")));
        given(orderFacts.salesBetween(any(), any())).willReturn(new BigDecimal("125000"));

        assertThat(service.list().get(0).progressPercent()).isEqualTo(125.0);
    }

    @Test
    void creaLaMetaConElNombreLimpioYQuienLaCreo() {
        given(goals.save(any(SalesGoal.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(orderFacts.salesBetween(any(), any())).willReturn(BigDecimal.ZERO);

        GoalResponse response = service.create(new CreateGoalRequest("  Noviembre ", new BigDecimal("300000"),
                LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-30")), "Admin Demo");

        assertThat(response.name()).isEqualTo("Noviembre");
        assertThat(response.createdBy()).isEqualTo("Admin Demo");
        assertThat(response.progressPercent()).isZero();
    }

    @Test
    void eliminarUnaMetaInexistenteResponde404() {
        given(goals.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> service.delete(99L)).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
        verify(goals, never()).deleteById(any());
    }
}
