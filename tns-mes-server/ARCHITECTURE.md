# TNS MES Architecture Baseline

This project is a modular monolith for the first delivery. It is intentionally organized so that a later split into independently deployed services does not require rewriting domain rules.

## Bounded contexts

| Package | Responsibility | Current delivery | Reserved expansion |
| --- | --- | --- | --- |
| `identity` | users, roles, permissions, authentication | login, JWT, user/role administration | SSO, MFA, organization data scope |
| `basic` | enterprise and organization master data | 13 master-data types and hierarchy validation | units, calendars, defect codes, numbering rules |
| `engineering` | product and manufacturing engineering | products, BOM, process routes, inspection rules | revisions, ECO, work instructions, attachments |
| `production` | production planning and execution | work orders and lifecycle | batches, serials, material issue, station execution, reporting, packing, traceability |
| `common` | cross-cutting infrastructure only | API envelope, errors, i18n, audit, Redis, request context | outbox, metrics, tracing, file service |
| `integration` | external system adapters | SAP generic HTTP boundary, product/work-order import | middleware/PLM/OA/SCADA adapters, reconciliation, replay |
| `warehouse` | inventory and logistics | reserved boundary | receiving, picking, issue, return, transfer, count, stock ledger |
| `quality` | quality execution | reserved boundary | IQC/IPQC/FQC/OQC, SPC, NCR, 8D, release gates |
| `equipment` | asset and maintenance | reserved boundary | assets, PM, breakdown, spare parts, tooling |

## Dependency rules

1. `web` depends on its own `service` and DTO/view mapper; controllers do not access repositories directly.
2. A domain package may depend on another domain through an application service or an ID/reference, not through database entities from another bounded context. The current `production` references engineering aggregates only at its service boundary and stores their IDs in the schema where possible.
3. `common` cannot contain MES-specific workflow or inventory rules. Redis helpers, error handling, audit and request context belong here; business decisions do not.
4. External systems are accessed through adapters under `integration`; no controller or domain service will call SAP/SCADA/PLM directly.
5. Every stateful aggregate has an explicit status transition rule, optimistic version, unique business key and audit metadata.

## Transaction and messaging rules

- SQL Server is the source of truth for business facts. Redis is rebuildable cache, short-lived coordination state, idempotency storage, rate limiting and Streams transport.
- Synchronous APIs are used for validations that must return an immediate result. Long-running imports, device ingestion, notifications and external callbacks use a transactional outbox/Streams consumer boundary.
- Before production deployment, add an outbox table and consumer persistence so a database commit and message publication are recoverable as one business operation. The current `StreamQueueService` is infrastructure only and is deliberately not used as a hidden source of truth.

## Delivery sequence

The first three functional stages are implemented in this order:

1. Basic data and IAM foundation.
2. Engineering preparation.
3. Work-order planning foundation.

The next P0 slice must extend `production` with material preparation/issue, station execution, quality gates, reporting, packing, warehousing and traceability before calling the production loop complete. WMS/QMS/EAM and external interfaces remain separate contexts and are not folded into the current CRUD services.

## Production hardening before deployment

- Switch to Java 17 / Spring Boot 3 after the target runtime is confirmed, or keep the current Java 11 / Spring Boot 2.7 baseline for compatibility.
- Run Flyway against a new SQL Server database; keep Hibernate `ddl-auto=validate` in SQL Server profile.
- Add SQL Server integration tests, Redis Testcontainers, contract tests for each external adapter, load tests and failure/replay tests.
- Add an outbox, dead-letter and reconciliation implementation before enabling irreversible external writes.
