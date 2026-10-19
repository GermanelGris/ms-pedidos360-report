package cl.duoc.pedidos360.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

/**
 * Levanta el contexto sin MySQL ni RabbitMQ (base H2 en memoria, listeners detenidos)
 * y verifica la topología declarada.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:report;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.rabbitmq.listener.simple.auto-startup=false"})
class MsPedidos360ReportApplicationTests {

    @Autowired
    private ApplicationContext context;

    @Test
    void declaraColaPrincipalRetryYDlq() {
        Map<String, Queue> queues = context.getBeansOfType(Queue.class);
        assertThat(queues.values()).extracting(Queue::getName)
                .containsExactlyInAnyOrder("q.report.orders", "q.report.orders.retry", "q.report.orders.dlq");
        assertThat(context.getBeansOfType(Binding.class)).hasSize(3);
    }

    @Test
    void laColaSeSuscribeATodosLosEventosDePedidos() {
        Binding binding = context.getBean("ordersBinding", Binding.class);
        assertThat(binding.getExchange()).isEqualTo("orders.events");
        assertThat(binding.getRoutingKey()).isEqualTo("order.*");

        Queue main = context.getBean("ordersQueue", Queue.class);
        assertThat(main.getArguments()).containsEntry("x-dead-letter-exchange", "report.dlx");

        // El reintento vuelve solo a la cola de report (exchange por defecto), no al topic compartido
        Queue retry = context.getBean("ordersRetryQueue", Queue.class);
        assertThat(retry.getArguments())
                .containsEntry("x-message-ttl", 5000)
                .containsEntry("x-dead-letter-exchange", "")
                .containsEntry("x-dead-letter-routing-key", "q.report.orders");
    }
}
