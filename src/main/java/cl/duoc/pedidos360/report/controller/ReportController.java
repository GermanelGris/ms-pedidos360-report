package cl.duoc.pedidos360.report.controller;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.pedidos360.report.dto.GoalDtos.CreateGoalRequest;
import cl.duoc.pedidos360.report.dto.GoalDtos.GoalResponse;
import cl.duoc.pedidos360.report.dto.ReportDtos.HourlySales;
import cl.duoc.pedidos360.report.dto.ReportDtos.KpiResponse;
import cl.duoc.pedidos360.report.dto.TopProduct;
import cl.duoc.pedidos360.report.service.ReportService;
import cl.duoc.pedidos360.report.service.SalesGoalService;

/** Reportería: los KPIs llegan como eventos de RabbitMQ (orders.events); las metas de venta se administran aquí. */
@RestController
@RequestMapping("/api/report")
@Tag(name = "Reportería")
public class ReportController {

    private final ReportService service;
    private final SalesGoalService goals;

    public ReportController(ReportService service, SalesGoalService goals) {
        this.service = service;
        this.goals = goals;
    }

    @GetMapping("/kpis")
    @Operation(summary = "Pedidos totales, activos, ventas, lead time promedio y pedidos por estado")
    public KpiResponse kpis() {
        return service.kpis();
    }

    @GetMapping("/sales-by-hour")
    @Operation(summary = "Ventas por hora de las últimas N horas (máximo 168)")
    public List<HourlySales> salesByHour(@RequestParam(defaultValue = "24") int hours) {
        return service.salesByHour(hours);
    }

    @GetMapping("/top-products")
    @Operation(summary = "Productos más vendidos (excluye pedidos cancelados)")
    public List<TopProduct> topProducts(@RequestParam(defaultValue = "5") int limit) {
        return service.topProducts(limit);
    }

    @GetMapping("/goals")
    @Operation(summary = "Metas de venta con su avance (ventas del rango sin cancelados)")
    public List<GoalResponse> goals() {
        return goals.list();
    }

    @PostMapping("/goals")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Agregar una meta de venta para un rango de fechas")
    public GoalResponse createGoal(@Valid @RequestBody CreateGoalRequest request,
                                   @RequestHeader(value = "X-User-Name", required = false) String userName) {
        String createdBy = userName == null ? null : URLDecoder.decode(userName, StandardCharsets.UTF_8);
        return goals.create(request, createdBy);
    }

    @DeleteMapping("/goals/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar una meta de venta")
    public void deleteGoal(@PathVariable Long id) {
        goals.delete(id);
    }
}
