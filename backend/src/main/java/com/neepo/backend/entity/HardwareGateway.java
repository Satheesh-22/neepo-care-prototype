package com.neepo.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hardware_gateways")
public class HardwareGateway {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "vehicle_id")
    private UUID vehicleId;

    @Column(name = "device_serial", nullable = false, length = 64)
    private String deviceSerial;

    @Column(length = 32)
    private String imei;

    @Column(name = "secure_element_id", length = 64)
    private String secureElementId;

    @Column(name = "firmware_version", nullable = false, length = 32)
    private String firmwareVersion = "v1.1.0-prod";

    @Column(name = "hmac_secret_hash", nullable = false, length = 128)
    private String hmacSecretHash;

    @Column(nullable = false, length = 32)
    private String status = "ACTIVE";

    @Column(name = "last_heartbeat_at")
    private Instant lastHeartbeatAt;

    @Column(name = "last_sync_event_id")
    private UUID lastSyncEventId;

    @Column(name = "buffer_capacity_records", nullable = false)
    private Integer bufferCapacityRecords = 10000;

    @Column(name = "reporting_interval_seconds", nullable = false)
    private Integer reportingIntervalSeconds = 5;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata = "{}";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version = 0L;

    public HardwareGateway() {}

    public HardwareGateway(UUID companyId, String deviceSerial, String hmacSecretHash) {
        this.companyId = companyId;
        this.deviceSerial = deviceSerial;
        this.hmacSecretHash = hmacSecretHash;
    }

    @PrePersist
    public void onCreate() {
        var now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }

    public UUID getVehicleId() { return vehicleId; }
    public void setVehicleId(UUID vehicleId) { this.vehicleId = vehicleId; }

    public String getDeviceSerial() { return deviceSerial; }
    public void setDeviceSerial(String deviceSerial) { this.deviceSerial = deviceSerial; }

    public String getImei() { return imei; }
    public void setImei(String imei) { this.imei = imei; }

    public String getSecureElementId() { return secureElementId; }
    public void setSecureElementId(String secureElementId) { this.secureElementId = secureElementId; }

    public String getFirmwareVersion() { return firmwareVersion; }
    public void setFirmwareVersion(String firmwareVersion) { this.firmwareVersion = firmwareVersion; }

    public String getHmacSecretHash() { return hmacSecretHash; }
    public void setHmacSecretHash(String hmacSecretHash) { this.hmacSecretHash = hmacSecretHash; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getLastHeartbeatAt() { return lastHeartbeatAt; }
    public void setLastHeartbeatAt(Instant lastHeartbeatAt) { this.lastHeartbeatAt = lastHeartbeatAt; }

    public UUID getLastSyncEventId() { return lastSyncEventId; }
    public void setLastSyncEventId(UUID lastSyncEventId) { this.lastSyncEventId = lastSyncEventId; }

    public Integer getBufferCapacityRecords() { return bufferCapacityRecords; }
    public void setBufferCapacityRecords(Integer bufferCapacityRecords) { this.bufferCapacityRecords = bufferCapacityRecords; }

    public Integer getReportingIntervalSeconds() { return reportingIntervalSeconds; }
    public void setReportingIntervalSeconds(Integer reportingIntervalSeconds) { this.reportingIntervalSeconds = reportingIntervalSeconds; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
