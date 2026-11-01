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
@Table(name = "fleet_analytics_snapshots")
public class FleetAnalyticsSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(nullable = false, length = 10)
    private String timeframe;

    @Column(name = "total_trips_monitored", nullable = false)
    private int totalTripsMonitored;

    @Column(name = "active_disruptions_count", nullable = false)
    private int activeDisruptionsCount;

    @Column(name = "resolved_disruptions_count", nullable = false)
    private int resolvedDisruptionsCount;

    @Column(name = "mean_time_to_recovery_minutes", nullable = false)
    private double meanTimeToRecoveryMinutes;

    @Column(name = "driver_compliance_rate", nullable = false)
    private double driverComplianceRate;

    @Column(name = "estimated_cost_avoidance_usd", nullable = false)
    private double estimatedCostAvoidanceUsd;

    @Column(name = "saved_transit_hours", nullable = false)
    private double savedTransitHours;

    @Column(name = "spoilage_avoidance_rate", nullable = false)
    private double spoilageAvoidanceRate;

    @Column(name = "root_causes_json", nullable = false, columnDefinition = "jsonb")
    private String rootCausesJson = "[]";

    @Column(name = "trends_json", nullable = false, columnDefinition = "jsonb")
    private String trendsJson = "[]";

    @Column(name = "strategies_json", nullable = false, columnDefinition = "jsonb")
    private String strategiesJson = "[]";

    @Column(name = "snapshot_at", nullable = false)
    private Instant snapshotAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected FleetAnalyticsSnapshot() {
    }

    public FleetAnalyticsSnapshot(
            UUID companyId,
            String timeframe,
            int totalTripsMonitored,
            int activeDisruptionsCount,
            int resolvedDisruptionsCount,
            double meanTimeToRecoveryMinutes,
            double driverComplianceRate,
            double estimatedCostAvoidanceUsd,
            double savedTransitHours,
            double spoilageAvoidanceRate,
            String rootCausesJson,
            String trendsJson,
            String strategiesJson,
            Instant snapshotAt
    ) {
        this.companyId = companyId;
        this.timeframe = timeframe;
        this.totalTripsMonitored = totalTripsMonitored;
        this.activeDisruptionsCount = activeDisruptionsCount;
        this.resolvedDisruptionsCount = resolvedDisruptionsCount;
        this.meanTimeToRecoveryMinutes = meanTimeToRecoveryMinutes;
        this.driverComplianceRate = driverComplianceRate;
        this.estimatedCostAvoidanceUsd = estimatedCostAvoidanceUsd;
        this.savedTransitHours = savedTransitHours;
        this.spoilageAvoidanceRate = spoilageAvoidanceRate;
        this.rootCausesJson = rootCausesJson != null ? rootCausesJson : "[]";
        this.trendsJson = trendsJson != null ? trendsJson : "[]";
        this.strategiesJson = strategiesJson != null ? strategiesJson : "[]";
        this.snapshotAt = snapshotAt != null ? snapshotAt : Instant.now();
    }

    @PrePersist
    void onPrePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (snapshotAt == null) {
            snapshotAt = createdAt;
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public String getTimeframe() {
        return timeframe;
    }

    public int getTotalTripsMonitored() {
        return totalTripsMonitored;
    }

    public int getActiveDisruptionsCount() {
        return activeDisruptionsCount;
    }

    public int getResolvedDisruptionsCount() {
        return resolvedDisruptionsCount;
    }

    public double getMeanTimeToRecoveryMinutes() {
        return meanTimeToRecoveryMinutes;
    }

    public double getDriverComplianceRate() {
        return driverComplianceRate;
    }

    public double getEstimatedCostAvoidanceUsd() {
        return estimatedCostAvoidanceUsd;
    }

    public double getSavedTransitHours() {
        return savedTransitHours;
    }

    public double getSpoilageAvoidanceRate() {
        return spoilageAvoidanceRate;
    }

    public String getRootCausesJson() {
        return rootCausesJson;
    }

    public String getTrendsJson() {
        return trendsJson;
    }

    public String getStrategiesJson() {
        return strategiesJson;
    }

    public Instant getSnapshotAt() {
        return snapshotAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
