package cl.duoc.pedidos360.report.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.pedidos360.report.dto.ReportDtos.HourlySales;
import cl.duoc.pedidos360.report.dto.ReportDtos.KpiResponse;
import cl.duoc.pedidos360.report.dto.TopProduct;

/**
 * API de reportería (solo lectura). Contrato definido; aún sin implementación:
 * todos los endpoints responden 501 Not Implemented (fuera del alcance de la EP1).
 */
@RestController
@RequestMapping("/api/report")
public class ReportController {

    @GetMapping("/kpis")
    public ResponseEntity<KpiResponse> kpis() {
        return notImplemented();
    }

    @GetMapping("/sales-by-hour")
    public ResponseEntity<List<HourlySales>> salesByHour(@RequestParam(defaultValue = "24") int hours) {
        return notImplemented();
    }

    @GetMapping("/top-products")
    public ResponseEntity<List<TopProduct>> topProducts(@RequestParam(defaultValue = "5") int limit) {
        return notImplemented();
    }

    private static <T> ResponseEntity<T> notImplemented() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
