package cl.duoc.pedidos360.report.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReportController.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void kpisAunNoImplementado() throws Exception {
        mvc.perform(get("/api/report/kpis")).andExpect(status().isNotImplemented());
    }

    @Test
    void ventasPorHoraAunNoImplementado() throws Exception {
        mvc.perform(get("/api/report/sales-by-hour").param("hours", "12")).andExpect(status().isNotImplemented());
    }

    @Test
    void topProductosAunNoImplementado() throws Exception {
        mvc.perform(get("/api/report/top-products")).andExpect(status().isNotImplemented());
    }
}
