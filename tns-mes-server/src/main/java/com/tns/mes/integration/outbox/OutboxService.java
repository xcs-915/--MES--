package com.tns.mes.integration.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class OutboxService {
    private final OutboxMessageRepository repository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxMessageRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public OutboxMessage enqueue(String aggregateType, String aggregateId, String eventType, Object payload) {
        OutboxMessage message = new OutboxMessage();
        message.setEventId(UUID.randomUUID().toString().replace("-", ""));
        message.setAggregateType(aggregateType);
        message.setAggregateId(aggregateId);
        message.setEventType(eventType);
        try {
            message.setPayload(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Unable to serialize outbox payload", ex);
        }
        message.setNextAttemptAt(Instant.now());
        return repository.save(message);
    }
}

