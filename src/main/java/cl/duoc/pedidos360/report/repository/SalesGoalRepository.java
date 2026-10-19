package cl.duoc.pedidos360.report.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.duoc.pedidos360.report.model.SalesGoal;

public interface SalesGoalRepository extends JpaRepository<SalesGoal, Long> {

    List<SalesGoal> findAllByOrderByEndDateDescIdDesc();
}
