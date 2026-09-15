# ms-pedidos360-report

Microservicio de **reportería y KPIs** de Pedidos360. Consume el tópico de Kafka `orders.events` y construye una proyección de lectura en MySQL, **sin bloquear el core** de pedidos.

## Endpoints (solo lectura)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/report/kpis` | Pedidos totales, activos, ventas, lead time promedio y pedidos por estado |
| GET | `/api/report/sales-by-hour?hours=24` | Ventas por hora (incluye horas en cero, para graficar) |
| GET | `/api/report/top-products?limit=5` | Productos más vendidos (excluye cancelados) |

Swagger: `http://localhost:8084/swagger-ui.html`

## Kafka

| Tópico | Particiones | Política | Retención |
|---|---|---|---|
| `orders.events` | 3 | delete | 7 días |
| `orders.events.report.DLT` | 3 | delete | 14 días |

- **Idempotencia:** los `eventId` aplicados se guardan en `processed_events`.
- **Orden:** un evento más antiguo que el último aplicado no retrocede el estado del pedido.
- **Errores:** se reintenta 2 veces y luego se publica en la **DLT** con el mensaje original y los metadatos del error (excepción, stacktrace, offset).

## Base de datos

Schema `pedidos360_report` con las tablas `order_facts`, `order_item_facts` y `processed_events`.

| Variable | Por defecto |
|---|---|
| `DB_HOST` / `DB_USER` / `DB_PASSWORD` | `localhost` / `root` / `root` |
| `KAFKA_BOOTSTRAP` | `localhost:9092` |
| `REPORT_ZONE` | `America/Santiago` |

## Ejecutar

```bash
./mvnw test
./mvnw spring-boot:run
```

## Autores

Germán Maraboli & Camila Vera

Proyecto Pedidos360 · DSY1107 Desarrollo Cloud Native I · Duoc UC
