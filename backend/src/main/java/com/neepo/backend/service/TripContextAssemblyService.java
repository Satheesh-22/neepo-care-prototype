package com.neepo.backend.service;

import com.neepo.backend.dto.TelemetryObservationRequest;
import com.neepo.backend.exception.ResourceNotFoundException;
import com.neepo.contracts.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class TripContextAssemblyService {
    private static final String CORE_SQL = """
            SELECT t.id AS trip_id, t.company_id, t.vehicle_id, t.driver_id, t.cargo_id, t.route_id,
                   t.reference_code, t.status AS trip_status, t.delivery_deadline,
                   t.maximum_waiting_minutes, t.accepted_eta_threshold_minutes,
                   t.started_at, t.created_at AS trip_created_at, t.updated_at AS trip_updated_at,
                   v.registration_number, v.vehicle_type, v.status AS vehicle_status,
                   v.height_meters, v.width_meters, v.gross_weight_kg, v.payload_capacity_kg,
                   v.maximum_engine_temperature_c, v.current_engine_health_percent,
                   v.refrigeration_supported,
                   dr.status AS driver_status, dr.remaining_driving_minutes,
                   dr.max_daily_driving_minutes, dr.last_rest_completed_at,
                   c.cargo_type, c.weight_kg, c.minimum_temperature_c,
                   c.maximum_temperature_c, c.hazardous,
                   r.status AS route_status, r.planned_distance_km, r.planned_duration_minutes,
                   ST_Y(r.origin::geometry) AS origin_latitude,
                   ST_X(r.origin::geometry) AS origin_longitude,
                   ST_Y(r.destination::geometry) AS destination_latitude,
                   ST_X(r.destination::geometry) AS destination_longitude
            FROM trips t
            JOIN vehicles v ON v.id = t.vehicle_id AND v.company_id = t.company_id
            JOIN drivers dr ON dr.id = t.driver_id AND dr.company_id = t.company_id
            JOIN cargo c ON c.id = t.cargo_id AND c.company_id = t.company_id
            JOIN routes r ON r.id = t.route_id AND r.company_id = t.company_id
            WHERE t.id = ? AND t.company_id = ?
            """;
    private static final String RESTRICTIONS_SQL = """
            SELECT id, restriction_code, restriction_kind, limit_value, unit,
                   observed_at, expires_at, confidence
            FROM route_restrictions
            WHERE route_id = ?
            ORDER BY restriction_code, id
            """;
    private static final String PROVIDERS_SQL = """
            SELECT p.id, p.name, p.provider_type,
                   ST_Y(p.location::geometry) AS latitude,
                   ST_X(p.location::geometry) AS longitude,
                   p.reliability_score,
                   a.status AS availability_status, a.observed_at, a.expires_at,
                   a.estimated_arrival_minutes, a.confidence, a.required_resource_available
            FROM service_providers p
            LEFT JOIN LATERAL (
                SELECT status, observed_at, expires_at, estimated_arrival_minutes,
                       confidence, required_resource_available
                FROM provider_availability
                WHERE provider_id = p.id
                ORDER BY observed_at DESC, id DESC
                LIMIT 1
            ) a ON TRUE
            WHERE p.active = TRUE AND (p.company_id IS NULL OR p.company_id = ?)
            ORDER BY p.provider_type, p.id
            """;
    private static final String CAPABILITIES_SQL = """
            SELECT capability_code
            FROM provider_capabilities
            WHERE provider_id = ?
            ORDER BY capability_code
            """;
    private static final String SAFE_STOPS_SQL = """
            SELECT id, name, ST_Y(location::geometry) AS latitude,
                   ST_X(location::geometry) AS longitude,
                   maximum_vehicle_height_meters, maximum_vehicle_weight_kg,
                   security_verified, observed_at, expires_at, confidence
            FROM safe_stops
            ORDER BY id
            """;

    private final JdbcTemplate jdbc;

    public TripContextAssemblyService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public TripContext assemble(UUID companyId, TelemetryObservationRequest observation) {
        Core core = jdbc.query(CORE_SQL, this::mapCore, observation.tripId(), companyId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("TRIP_NOT_FOUND", "Trip was not found"));
        ObservationMetadata telemetryMetadata = new ObservationMetadata(
                observation.recordedAt(), observation.receivedAt(), observation.expiresAt(),
                observation.confidence().doubleValue(),
                com.neepo.contracts.ObservationStatus.valueOf(observation.observationStatus().name()),
                observation.source().name()
        );
        ObservationMetadata stateMetadata = new ObservationMetadata(
                observation.receivedAt(), observation.receivedAt(), observation.expiresAt(),
                1.0, ObservationStatus.FRESH, "SERVER_STATE"
        );
        GeoPoint currentLocation = new GeoPoint(
                observation.location().latitude().doubleValue(),
                observation.location().longitude().doubleValue()
        );
        List<String> requiredCapabilities = core.refrigerationSupported()
                ? List.of("REFRIGERATED_HEAVY_VEHICLE_REPAIR")
                : List.of("HEAVY_VEHICLE_REPAIR");
        List<String> handlingRequirements = core.cargoType() == CargoType.REFRIGERATED
                || core.cargoType() == CargoType.FROZEN
                ? List.of("MAINTAIN_TEMPERATURE")
                : List.of();

        var trip = new com.neepo.contracts.Trip(
                core.tripId(), core.companyId(), core.vehicleId(), core.driverId(), core.cargoId(), core.routeId(),
                core.referenceCode(), core.tripStatus(), core.deliveryDeadline(), core.maximumWaitingMinutes(),
                core.acceptedEtaThresholdMinutes(), core.startedAt(), core.tripCreatedAt(), core.tripUpdatedAt()
        );
        var vehicle = new VehicleContext(
                core.vehicleId(), core.registrationNumber(), core.vehicleType(), core.vehicleStatus(),
                core.heightMeters(), core.widthMeters(), core.grossWeightKg(), core.payloadCapacityKg(),
                core.maximumEngineTemperatureCelsius(), observation.engineTemperatureCelsius(),
                core.currentEngineHealthPercent(), core.refrigerationSupported(), requiredCapabilities,
                telemetryMetadata
        );
        var driver = new DriverContext(
                core.driverId(), core.driverStatus(), core.remainingDrivingMinutes(),
                core.maximumDailyDrivingMinutes(), core.lastRestCompletedAt(), stateMetadata
        );
        var cargo = new CargoContext(
                core.cargoId(), core.cargoType(), core.weightKg(), core.minimumTemperatureCelsius(),
                core.maximumTemperatureCelsius(), observation.cargoTemperatureCelsius(), core.hazardous(),
                handlingRequirements, telemetryMetadata
        );
        var route = new RouteContext(
                core.routeId(), core.routeStatus(), core.origin(), core.destination(), currentLocation,
                core.plannedDistanceKm(), core.plannedDurationMinutes(), restrictions(core.routeId()), stateMetadata
        );
        var telemetry = new TelemetryObservation(
                observation.eventId(), observation.tripId(), observation.vehicleId(), observation.recordedAt(),
                observation.receivedAt(), observation.expiresAt(), currentLocation,
                observation.engineTemperatureCelsius(), observation.cargoTemperatureCelsius(),
                observation.speedKph(), observation.confidence().doubleValue(),
                com.neepo.contracts.ObservationStatus.valueOf(observation.observationStatus().name()),
                com.neepo.contracts.TelemetrySource.valueOf(observation.source().name()),
                com.neepo.contracts.NetworkState.valueOf(observation.networkState().name())
        );
        return new TripContext(
                trip, vehicle, driver, cargo, route, providers(companyId, observation.receivedAt()),
                safeStops(), List.of(telemetry),
                com.neepo.contracts.NetworkState.valueOf(observation.networkState().name()),
                List.of(), null, observation.receivedAt(), contextHash(observation)
        );
    }

    private Core mapCore(ResultSet resultSet, int rowNumber) throws SQLException {
        return new Core(
                resultSet.getObject("trip_id", UUID.class), resultSet.getObject("company_id", UUID.class),
                resultSet.getObject("vehicle_id", UUID.class), resultSet.getObject("driver_id", UUID.class),
                resultSet.getObject("cargo_id", UUID.class), resultSet.getObject("route_id", UUID.class),
                resultSet.getString("reference_code"), TripStatus.valueOf(resultSet.getString("trip_status")),
                instant(resultSet, "delivery_deadline"), resultSet.getInt("maximum_waiting_minutes"),
                resultSet.getInt("accepted_eta_threshold_minutes"), nullableInstant(resultSet, "started_at"),
                instant(resultSet, "trip_created_at"), instant(resultSet, "trip_updated_at"),
                resultSet.getString("registration_number"), resultSet.getString("vehicle_type"),
                VehicleStatus.valueOf(resultSet.getString("vehicle_status")), resultSet.getBigDecimal("height_meters"),
                resultSet.getBigDecimal("width_meters"), resultSet.getBigDecimal("gross_weight_kg"),
                resultSet.getBigDecimal("payload_capacity_kg"), resultSet.getBigDecimal("maximum_engine_temperature_c"),
                resultSet.getBigDecimal("current_engine_health_percent"), resultSet.getBoolean("refrigeration_supported"),
                DriverStatus.valueOf(resultSet.getString("driver_status")), resultSet.getInt("remaining_driving_minutes"),
                resultSet.getInt("max_daily_driving_minutes"), nullableInstant(resultSet, "last_rest_completed_at"),
                CargoType.valueOf(resultSet.getString("cargo_type")), resultSet.getBigDecimal("weight_kg"),
                resultSet.getBigDecimal("minimum_temperature_c"), resultSet.getBigDecimal("maximum_temperature_c"),
                resultSet.getBoolean("hazardous"), RouteStatus.valueOf(resultSet.getString("route_status")),
                resultSet.getBigDecimal("planned_distance_km"), resultSet.getInt("planned_duration_minutes"),
                new GeoPoint(resultSet.getDouble("origin_latitude"), resultSet.getDouble("origin_longitude")),
                new GeoPoint(resultSet.getDouble("destination_latitude"), resultSet.getDouble("destination_longitude"))
        );
    }

    private List<RouteRestriction> restrictions(UUID routeId) {
        return jdbc.query(RESTRICTIONS_SQL, (resultSet, rowNumber) -> new RouteRestriction(
                resultSet.getObject("id", UUID.class), resultSet.getString("restriction_code"),
                resultSet.getString("restriction_kind"), resultSet.getBigDecimal("limit_value"),
                resultSet.getString("unit"), "BLOCKAGE".equals(resultSet.getString("restriction_kind")),
                metadata(resultSet, "observed_at", "expires_at", "confidence", "ROUTE_PROVIDER")
        ), routeId);
    }

    private List<ProviderContext> providers(UUID companyId, Instant evaluationTime) {
        return jdbc.query(PROVIDERS_SQL, (resultSet, rowNumber) -> {
            UUID providerId = resultSet.getObject("id", UUID.class);
            String status = resultSet.getString("availability_status");
            ObservationMetadata observation = status == null
                    ? unavailableMetadata(evaluationTime)
                    : metadata(resultSet, "observed_at", "expires_at", "confidence", "PROVIDER_FEED");
            return new ProviderContext(
                    providerId, resultSet.getString("name"),
                    ProviderType.valueOf(resultSet.getString("provider_type")),
                    new GeoPoint(resultSet.getDouble("latitude"), resultSet.getDouble("longitude")),
                    jdbc.query(CAPABILITIES_SQL, (capability, index) -> capability.getString(1), providerId),
                    status == null ? ProviderAvailabilityStatus.UNAVAILABLE : ProviderAvailabilityStatus.valueOf(status),
                    (Integer) resultSet.getObject("estimated_arrival_minutes"),
                    resultSet.getBigDecimal("reliability_score"),
                    status != null && resultSet.getBoolean("required_resource_available"), observation
            );
        }, companyId);
    }

    private List<SafeStopContext> safeStops() {
        return jdbc.query(SAFE_STOPS_SQL, (resultSet, rowNumber) -> new SafeStopContext(
                resultSet.getObject("id", UUID.class), resultSet.getString("name"),
                new GeoPoint(resultSet.getDouble("latitude"), resultSet.getDouble("longitude")),
                resultSet.getBigDecimal("maximum_vehicle_height_meters"),
                resultSet.getBigDecimal("maximum_vehicle_weight_kg"), resultSet.getBoolean("security_verified"),
                metadata(resultSet, "observed_at", "expires_at", "confidence", "SAFE_STOP_FEED")
        ));
    }

    private static ObservationMetadata metadata(
            ResultSet resultSet,
            String observedColumn,
            String expiresColumn,
            String confidenceColumn,
            String source
    ) throws SQLException {
        Instant observedAt = instant(resultSet, observedColumn);
        return new ObservationMetadata(
                observedAt, observedAt, instant(resultSet, expiresColumn),
                resultSet.getBigDecimal(confidenceColumn).doubleValue(), ObservationStatus.FRESH, source
        );
    }

    private static ObservationMetadata unavailableMetadata(Instant evaluationTime) {
        return new ObservationMetadata(
                evaluationTime, evaluationTime, evaluationTime.plusSeconds(1),
                0.0, ObservationStatus.UNVERIFIED, "PROVIDER_FEED"
        );
    }

    private static String contextHash(TelemetryObservationRequest observation) {
        String input = observation.tripId() + ":" + observation.eventId() + ":" + observation.recordedAt();
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static Instant instant(ResultSet resultSet, String column) throws SQLException {
        return resultSet.getObject(column, OffsetDateTime.class).toInstant();
    }

    private static Instant nullableInstant(ResultSet resultSet, String column) throws SQLException {
        OffsetDateTime value = resultSet.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    private record Core(
            UUID tripId, UUID companyId, UUID vehicleId, UUID driverId, UUID cargoId, UUID routeId,
            String referenceCode, TripStatus tripStatus, Instant deliveryDeadline, int maximumWaitingMinutes,
            int acceptedEtaThresholdMinutes, Instant startedAt, Instant tripCreatedAt, Instant tripUpdatedAt,
            String registrationNumber, String vehicleType, VehicleStatus vehicleStatus,
            java.math.BigDecimal heightMeters, java.math.BigDecimal widthMeters,
            java.math.BigDecimal grossWeightKg, java.math.BigDecimal payloadCapacityKg,
            java.math.BigDecimal maximumEngineTemperatureCelsius,
            java.math.BigDecimal currentEngineHealthPercent, boolean refrigerationSupported,
            DriverStatus driverStatus, int remainingDrivingMinutes, int maximumDailyDrivingMinutes,
            Instant lastRestCompletedAt, CargoType cargoType, java.math.BigDecimal weightKg,
            java.math.BigDecimal minimumTemperatureCelsius, java.math.BigDecimal maximumTemperatureCelsius,
            boolean hazardous, RouteStatus routeStatus, java.math.BigDecimal plannedDistanceKm,
            int plannedDurationMinutes, GeoPoint origin, GeoPoint destination
    ) { }
}
