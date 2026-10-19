package cl.duoc.pedidos360.report.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Nombres de exchanges, colas y binding keys, leídos de {@code pedidos360.rabbitmq} en application.yml.
 * Ninguna otra clase escribe nombres de RabbitMQ en el código.
 */
@Validated
@ConfigurationProperties("pedidos360.rabbitmq")
public record RabbitMessagingProperties(
        @Valid @NotNull Exchanges exchanges,
        @Valid @NotNull Retry retry,
        @Valid @NotNull Subscription orders) {

    public record Exchanges(@NotBlank String events, @NotBlank String retry, @NotBlank String deadLetter) {
    }

    public record Retry(@Min(1) int maxAttempts, @Min(100) int delayMs) {
    }

    /** Suscripción a un tipo de evento: cola principal, cola de espera para reintentos, DLQ y binding key. */
    public record Subscription(@NotBlank String queue, @NotBlank String retryQueue, @NotBlank String dlq,
                               @NotBlank String bindingKey) {
    }
}
