package cl.duoc.pedidos360.report.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.duoc.pedidos360.report.model.OrderFact;

public interface OrderFactRepository extends JpaRepository<OrderFact, Long> {

    List<OrderFact> findByCreatedAtGreaterThanEqual(Instant since);
}
