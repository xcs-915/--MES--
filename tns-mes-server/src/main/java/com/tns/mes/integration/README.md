# Integration Context

External interfaces are intentionally kept outside the current stage-one/three domain services.

Planned adapters:

- SAP or middleware: products, vendors, customers, work orders, material movements and receipts.
- PLM/OA: engineering documents, approval and revision notifications.
- SCADA/device gateways: equipment status, parameters and production events.
- Oracle/external MES databases: controlled read-only or contract-based synchronization.

Each adapter must provide a versioned contract, authentication/signature, timeout, idempotency key, retry policy, dead-letter handling, reconciliation report and manual replay operation. Implementations should publish an internal event after the local transaction commits; they must not make an unbounded synchronous call from a web controller.

