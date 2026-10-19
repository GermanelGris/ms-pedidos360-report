package cl.duoc.pedidos360.report.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import cl.duoc.pedidos360.report.dto.GoalDtos.CreateGoalRequest;
import cl.duoc.pedidos360.report.dto.GoalDtos.GoalResponse;
import cl.duoc.pedidos360.report.model.SalesGoal;
import cl.duoc.pedidos360.report.repository.OrderFactRepository;
import cl.duoc.pedidos360.report.repository.SalesGoalRepository;

/** Metas de venta. El avance se calcula al consultar, con las ventas proyectadas desde los eventos de pedidos. */
@Service
public class SalesGoalService {

    private final SalesGoalRepository goals;
    private final OrderFactRepository orderFacts;
    private final Clock clock;
    private final ZoneId zone;

    public SalesGoalService(SalesGoalRepository goals, OrderFactRepository orderFacts, Clock clock,
                            @Value("${report.zone}") String zone) {
        this.goals = goals;
        this.orderFacts = orderFacts;
        this.clock = clock;
        this.zone = ZoneId.of(zone);
    }

    @Transactional(readOnly = true)
    public List<GoalResponse> list() {
        return goals.findAllByOrderByEndDateDescIdDesc().stream().map(this::withProgress).toList();
    }

    @Transactional
    public GoalResponse create(CreateGoalRequest request, String createdBy) {
        SalesGoal goal = goals.save(new SalesGoal(request.name().trim(), request.amount(), request.startDate(),
                request.endDate(), createdBy, Instant.now(clock)));
        return withProgress(goal);
    }

    @Transactional
    public void delete(Long id) {
        if (!goals.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La meta " + id + " no existe");
        }
        goals.deleteById(id);
    }

    private GoalResponse withProgress(SalesGoal goal) {
        // Rango inclusive en la zona horaria del negocio: [inicio 00:00, día siguiente al término 00:00)
        Instant from = goal.getStartDate().atStartOfDay(zone).toInstant();
        Instant to = goal.getEndDate().plusDays(1).atStartOfDay(zone).toInstant();
        BigDecimal achieved = orderFacts.salesBetween(from, to);
        double progress = achieved.multiply(BigDecimal.valueOf(100))
                .divide(goal.getAmount(), 1, RoundingMode.HALF_UP)
                .doubleValue();
        return new GoalResponse(goal.getId(), goal.getName(), goal.getAmount(), goal.getStartDate(),
                goal.getEndDate(), achieved, progress, goal.getCreatedBy(), goal.getCreatedAt());
    }
}
