package org.example.stockitbe.hq.circularbuyer.sync.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.common.model.BaseEntity;

import java.time.Instant;

@Entity
@Table(name = "es_sync_outbox", indexes = {
        @Index(name = "idx_es_sync_outbox_status_next_retry", columnList = "status,next_retry_at"),
        @Index(name = "idx_es_sync_outbox_entity", columnList = "entity_type,entity_key")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EsSyncOutbox extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false, length = 64)
    private String entityType;

    @Column(name = "entity_key", nullable = false, length = 128)
    private String entityKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "op_type", nullable = false, length = 16)
    private EsSyncOperationType opType;

    @Lob
    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private EsSyncOutboxStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "next_retry_at", nullable = false)
    private Instant nextRetryAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Builder
    private EsSyncOutbox(String entityType,
                         String entityKey,
                         EsSyncOperationType opType,
                         String payload,
                         EsSyncOutboxStatus status,
                         int retryCount,
                         Instant nextRetryAt,
                         String lastError) {
        this.entityType = entityType;
        this.entityKey = entityKey;
        this.opType = opType;
        this.payload = payload;
        this.status = status;
        this.retryCount = retryCount;
        this.nextRetryAt = nextRetryAt;
        this.lastError = lastError;
    }

    public static EsSyncOutbox pending(String entityType,
                                       String entityKey,
                                       EsSyncOperationType opType,
                                       String payload,
                                       Instant nextRetryAt,
                                       String lastError) {
        return EsSyncOutbox.builder()
                .entityType(entityType)
                .entityKey(entityKey)
                .opType(opType)
                .payload(payload)
                .status(EsSyncOutboxStatus.PENDING)
                .retryCount(0)
                .nextRetryAt(nextRetryAt)
                .lastError(lastError)
                .build();
    }

    public void markProcessing() {
        this.status = EsSyncOutboxStatus.PROCESSING;
    }

    public void markDone() {
        this.status = EsSyncOutboxStatus.DONE;
        this.lastError = null;
    }

    public void markRetry(Instant nextRetryAt, String errorMessage) {
        this.status = EsSyncOutboxStatus.PENDING;
        this.retryCount += 1;
        this.nextRetryAt = nextRetryAt;
        this.lastError = errorMessage;
    }

    public void markDead(String errorMessage) {
        this.status = EsSyncOutboxStatus.DEAD;
        this.retryCount += 1;
        this.lastError = errorMessage;
    }
}
