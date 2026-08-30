package com.tns.mes.integration;

import java.time.Duration;

/** Stable boundary for SAP/PLM/OA/SCADA adapters; implementations live in integration subpackages. */
public interface ExternalSystemAdapter {
    String systemCode();
    String contractVersion();
    AdapterResult send(IntegrationCommand command, Duration timeout);

    class IntegrationCommand {
        private final String idempotencyKey;
        private final String operation;
        private final String payload;
        public IntegrationCommand(String idempotencyKey, String operation, String payload) { this.idempotencyKey = idempotencyKey; this.operation = operation; this.payload = payload; }
        public String getIdempotencyKey() { return idempotencyKey; }
        public String getOperation() { return operation; }
        public String getPayload() { return payload; }
    }

    class AdapterResult {
        private final boolean success;
        private final String externalReference;
        private final String message;
        public AdapterResult(boolean success, String externalReference, String message) { this.success = success; this.externalReference = externalReference; this.message = message; }
        public boolean isSuccess() { return success; }
        public String getExternalReference() { return externalReference; }
        public String getMessage() { return message; }
    }
}

