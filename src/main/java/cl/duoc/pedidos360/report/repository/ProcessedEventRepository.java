package cl.duoc.pedidos360.report.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.duoc.pedidos360.report.model.ProcessedEvent;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
}
