package com.neepo.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "gateway_sync_logs")
public class GatewaySyncLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "gateway_id", nullable = false)
    private UUID gatewayId;

    @Column(name = "trip_id")
    private UUID tripId;

    @Column(name = "batch_size", nullable = false)
    private Integer batchSize;

    @Column(name = "deduplicated_count", nullable = false)
    private Integer deduplicatedCount = 0;

    @Column(name = "sync_mode", nullable = false, length = 32)
    private String syncMode = "STREAM";

    @Column(name = "hmac_verified", nullable = false)
    private Boolean hmacVerified = true;

    @Column(name = "network_carrier", length = 64)
    private String networkCarrier;

    @Column(name = "signal_rssi")
    private Integer signalRssi;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public GatewaySyncLog() {}

    public GatewaySyncLog(UUID companyId, UUID gatewayId, UUID tripId, Integer batchSize,
                          Integer deduplicatedCount, String syncMode, Boolean hmacVerified) {
        this.companyId = companyId;
        this.gatewayId = gatewayId;
        this.tripId = tripId;
        this.batchSize = batchSize;
        this.deduplicatedCount = deduplicatedCount;
        this.syncMode = syncMode;
        this.hmacVerified = hmacVerified;
    }

    @PrePersist
    public void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }

    public UUID getGatewayId() { return gatewayId; }
    public void setGatewayId(UUID gatewayId) { this.gatewayId = gatewayId; }

    public UUID getTripId() { return tripId; }
    public void setTripId(UUID tripId) { this.tripId = tripId; }

    public Integer getBatchSize() { return batchSize; }
    public void setBatchSize(Integer batchSize) { this.batchSize = batchSize; }

    public Integer getDeduplicatedCount() { return deduplicatedCount; }
    public void setDeduplicatedCount(Integer deduplicatedCount) { this.deduplicatedCount = deduplicatedCount; }

    public String getSyncMode() { return syncMode; }
    public void setSyncMode(String syncMode) { this.syncMode = syncMode; }

    public Boolean getHmacVerified() { return hmacVerified; }
    public void setHmacVerified(Boolean hmacVerified) { this.hmacVerified = hmacVerified; }

    public String getNetworkCarrier() { return networkCarrier; }
    public void setNetworkCarrier(String networkCarrier) { this.networkCarrier = networkCarrier; }

    public Integer getSignalRssi() { return signalRssi; }
    public void setSignalRssi(Integer signalRssi) { this.signalRssi = signalRssi; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
