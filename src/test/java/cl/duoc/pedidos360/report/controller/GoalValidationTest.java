package cl.duoc.pedidos360.report.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import cl.duoc.pedidos360.report.service.ReportService;
import cl.duoc.pedidos360.report.service.SalesGoalService;

@WebMvcTest(ReportController.class)
class GoalValidationTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private SalesGoalService goals;

    @Test
    void rechazaMontoNoPositivoYRangoInvertido() throws Exception {
        mvc.perform(post("/api/report/goals").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Octubre","amount":0,"startDate":"2026-10-31","endDate":"2026-10-01"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("amount: debe ser mayor que 0")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(
                        "la fecha de término no puede ser anterior a la de inicio")));
        verifyNoInteractions(goals);
    }

    @Test
    void rechazaNombreVacio() throws Exception {
        mvc.perform(post("/api/report/goals").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":" ","amount":1000,"startDate":"2026-10-01","endDate":"2026-10-31"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("name: es obligatorio"));
    }
}
