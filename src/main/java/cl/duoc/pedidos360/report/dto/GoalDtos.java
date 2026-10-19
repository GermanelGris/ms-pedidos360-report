package cl.duoc.pedidos360.report.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

/** Metas de venta: alta y respuesta con el avance calculado. */
public final class GoalDtos {

    private GoalDtos() {
    }

    public record CreateGoalRequest(
            @Schema(example = "Ventas de octubre")
            @NotBlank(message = "es obligatorio") @Size(max = 80, message = "admite como máximo 80 caracteres")
            String name,

            @Schema(example = "500000")
            @NotNull(message = "es obligatorio") @Positive(message = "debe ser mayor que 0")
            @DecimalMax(value = "9999999999.99", message = "es demasiado grande")
            BigDecimal amount,

            @Schema(example = "2026-10-01")
            @NotNull(message = "es obligatoria")
            LocalDate startDate,

            @Schema(example = "2026-10-31")
            @NotNull(message = "es obligatoria")
            LocalDate endDate) {

        @Schema(hidden = true)
        @AssertTrue(message = "la fecha de término no puede ser anterior a la de inicio")
        public boolean isRangeValid() {
            return startDate == null || endDate == null || !endDate.isBefore(startDate);
        }
    }

    /**
     * Meta con su avance: {@code achieved} = ventas del rango (sin cancelados) y {@code progressPercent}
     * = achieved / amount × 100, que puede superar 100 cuando la meta se sobrepasa.
     */
    public record GoalResponse(Long id, String name, BigDecimal amount, LocalDate startDate, LocalDate endDate,
                               BigDecimal achieved, double progressPercent, String createdBy, Instant createdAt) {
    }
}
