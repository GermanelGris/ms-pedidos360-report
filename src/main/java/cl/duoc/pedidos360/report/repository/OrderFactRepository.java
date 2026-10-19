package cl.duoc.pedidos360.report.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cl.duoc.pedidos360.report.model.OrderFact;

public interface OrderFactRepository extends JpaRepository<OrderFact, Long> {

    List<OrderFact> findByCreatedAtGreaterThanEqual(Instant since);

    /** Ventas (sin cancelados) de los pedidos creados en [from, to). */
    @Query("select coalesce(sum(o.total), 0) from OrderFact o "
            + "where o.status <> 'CANCELADO' and o.createdAt >= :from and o.createdAt < :to")
    BigDecimal salesBetween(@Param("from") Instant from, @Param("to") Instant to);
}
