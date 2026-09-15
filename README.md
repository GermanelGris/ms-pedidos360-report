# ms-pedidos360-report

> 🚧 **En construcción.** Fuera del alcance de la EP1: por ahora solo contiene los DTO y el controller. Los endpoints responden `501 Not Implemented`.

Microservicio de **reportería y KPIs** de Pedidos360: ventas por hora, lead time y estados activos.

## Contenido actual

| Tipo | Clase | Descripción |
|---|---|---|
| Controller | `controller/ReportController` | Define la API; aún sin implementación (501) |
| DTO | `dto/ReportDtos.KpiResponse` | Pedidos totales, activos, ventas, lead time promedio y pedidos por estado |
| DTO | `dto/ReportDtos.HourlySales` | Ventas por hora |
| DTO | `dto/TopProduct` | Productos más vendidos |
| DTO | `dto/OrderEventMessage` | Evento de negocio de un pedido, con el pedido y sus ítems |

## Endpoints definidos

| Método | Ruta | Respuesta futura |
|---|---|---|
| GET | `/api/report/kpis` | `KpiResponse` |
| GET | `/api/report/sales-by-hour?hours=24` | `List<HourlySales>` |
| GET | `/api/report/top-products?limit=5` | `List<TopProduct>` |

Puerto reservado: `8084`.

## Autores

Germán Maraboli & Camila Vera

Proyecto Pedidos360 · DSY1107 Desarrollo Cloud Native I · Duoc UC
