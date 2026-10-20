# ms-pedidos360-report

Microservicio de **reportería y KPIs** de Pedidos360: ventas por hora, productos más vendidos, lead time, pedidos por estado y metas de venta con su avance.

Se alimenta de forma **asíncrona** con los eventos de dominio que publica `ms-pedidos360-orders` en RabbitMQ. Guarda una proyección de lectura propia (schema `pedidos360_report`) y nunca consulta la base de pedidos.

## Topología RabbitMQ

Todos los nombres viven en `application.yml` (`pedidos360.rabbitmq.*`) y se leen con `RabbitMessagingProperties`. Los beans `Queue` / `Exchange` / `Binding` están en `config/RabbitTopologyConfig`.

| Exchange | Tipo | Función |
|---|---|---|
| `orders.events` | topic | Eventos de dominio de pedidos (`order.created`, `order.status-changed`). Lo comparte con `ms-pedidos360-audit`: cada suscriptor tiene su propia cola |
| `report.retry` | direct | Recibe los mensajes que se reintentan |
| `report.dlx` | direct | Dead-letter exchange: los NACK de la cola principal llegan a la DLQ |

| Cola | Binding | Función |
|---|---|---|
| `q.report.orders` | `orders.events` · `order.*` | Cola principal; la consume `OrderEventsListener` |
| `q.report.orders.retry` | `report.retry` · `q.report.orders` | Espera 5 s (TTL) y devuelve el mensaje **solo** a `q.report.orders` por el exchange por defecto, para que audit no lo reciba dos veces |
| `q.report.orders.dlq` | `report.dlx` · `q.report.orders` | Mensajes que no se pudieron procesar |

```
orders ──order.*──> orders.events (topic) ──> q.report.orders ──> OrderEventsListener ──> ProjectionService
                                   │              │  ▲
                                   │   NACK       │  │ TTL vencido (exchange por defecto)
                                   ▼              ▼  │
                         q.audit.orders   report.dlx ──> q.report.orders.dlq     report.retry ──> q.report.orders.retry
```

## Confirmación y manejo de errores

`acknowledge-mode: manual`, con la política en `messaging/EventAcknowledger`:

| Situación | Tipo | Acción |
|---|---|---|
| Evento proyectado | — | `basicAck` |
| `eventId` ya proyectado (tabla `processed_events`) | duplicado | `basicAck` sin repetir el efecto |
| JSON inválido, sin `eventId` o sin pedido | **no recuperable** | `basicNack(requeue=false)`: DLQ inmediata |
| Otra excepción (p. ej. la base de datos no responde) | **recuperable** | Se republica en `q.report.orders.retry` con `x-retry-count` y `basicAck` del original |
| Falla recuperable en el último intento (`max-attempts: 3`) | agotado | `basicNack(requeue=false)`: DLQ |

Nunca se usa `requeue=true`. Cada envío a la DLQ queda en el log con nivel `ERROR`. Si llega un evento más antiguo que el último aplicado, no retrocede el estado del pedido.

## Listener de las DLQ

`messaging/DeadLetterListener` consume `q.report.orders.dlq`. Por cada mensaje muerto registra con nivel `ERROR` la cola de origen, el motivo y la cantidad de veces (header `x-death`: `rejected`, `expired` o `maxlen`), el exchange, la routing key, el `messageId` y el payload (hasta 2000 caracteres), y después confirma con ACK:

```
ERROR DeadLetterListener : DLQ [<dlq>] mensaje muerto: origen=<cola> motivo=rejected veces=1 exchange=<exchange> routingKeys=[<key>] messageId=<id> payload={...}
```

Un mensaje sin `x-death` es uno que el productor reenvió a la DLQ porque no tenía cola de destino (NO_ROUTE).

## API

| Método | Ruta | Respuesta |
|---|---|---|
| GET | `/api/report/kpis` | Pedidos totales, activos, ventas, lead time promedio, pedidos por estado y hora del último evento |
| GET | `/api/report/sales-by-hour?hours=24` | Ventas por hora (máximo 168 h), incluye horas sin ventas |
| GET | `/api/report/top-products?limit=5` | Productos más vendidos, excluye pedidos cancelados |
| GET | `/api/report/goals` | Metas de venta con lo vendido en su rango (sin cancelados) y el % de avance |
| POST | `/api/report/goals` | `201` · crea una meta `{ "name": "Ventas de octubre", "amount": 500000, "startDate": "2026-10-01", "endDate": "2026-10-31" }`; `400` con el detalle si falta un campo o el monto no es positivo |
| DELETE | `/api/report/goals/{id}` | `204` |

Los KPIs son de solo lectura: se calculan a partir de los eventos. Las metas se guardan en el mismo schema, con el usuario que las creó (header `X-User-Name` del BFF).

Desde el front se consume a través del BFF (`/api/report/**`, solo rol Admin). Swagger: `http://localhost:8084/swagger-ui.html`.

## Variables de entorno

| Variable | Por defecto |
|---|---|
| `DB_HOST`, `DB_USER`, `DB_PASSWORD` | `localhost`, `root`, `root` |
| `RABBITMQ_HOST`, `RABBITMQ_USER`, `RABBITMQ_PASSWORD` | `localhost`, `pedidos360`, `pedidos360` |
| `REPORT_ZONE` | `America/Santiago` |

Puerto: `8084`.

## Autores

Germán Marabolí, Sebastián Velez & Camila Vera

Proyecto Pedidos360 · DSY1107 Desarrollo Cloud Native I · Duoc UC
