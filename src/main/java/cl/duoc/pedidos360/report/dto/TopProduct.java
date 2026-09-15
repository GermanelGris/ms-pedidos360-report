package cl.duoc.pedidos360.report.dto;

import java.math.BigDecimal;

public record TopProduct(Long productId, String productName, Long quantity, BigDecimal revenue) {
}
