package cl.duoc.pedidos360.report.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.pedidos360.report.dto.ReportDtos.HourlySales;
import cl.duoc.pedidos360.report.dto.ReportDtos.KpiResponse;
import cl.duoc.pedidos360.report.dto.TopProduct;
import cl.duoc.pedidos360.report.service.ReportService;

/** API de solo lectura; los datos llegan por streaming desde Kafka. */
@RestController
@RequestMapping("/api/report")
@Tag(name = "Reportería (solo lectura)")
public class ReportController {

    private final ReportService service;

    public ReportController(ReportService service) {
        this.service = service;
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
}
