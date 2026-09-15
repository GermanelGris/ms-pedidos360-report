package cl.duoc.pedidos360.report.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import cl.duoc.pedidos360.report.dto.TopProduct;
import cl.duoc.pedidos360.report.model.OrderItemFact;

public interface OrderItemFactRepository extends JpaRepository<OrderItemFact, Long> {

    boolean existsByOrderId(Long orderId);

    @Query("""
            select new cl.duoc.pedidos360.report.dto.TopProduct(i.productId, i.productName, sum(i.quantity), sum(i.subtotal))
            from OrderItemFact i, OrderFact o
            where o.orderId = i.orderId and o.status <> 'CANCELADO'
            group by i.productId, i.productName
            order by sum(i.quantity) desc
            """)
    List<TopProduct> topProducts(Pageable pageable);
}
