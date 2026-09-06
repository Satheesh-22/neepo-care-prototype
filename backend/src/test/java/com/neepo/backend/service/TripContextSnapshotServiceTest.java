package com.neepo.backend.service;

import com.neepo.backend.dto.TripContextSnapshotRecord;
import com.neepo.backend.entity.Trip;
import com.neepo.backend.exception.ResourceConflictException;
import com.neepo.backend.exception.ResourceNotFoundException;
import com.neepo.backend.repository.TripContextSnapshotJdbcRepository;
import com.neepo.backend.repository.TripRepository;
import com.neepo.contracts.CargoContext;
import com.neepo.contracts.CargoType;
import com.neepo.contracts.DriverContext;
import com.neepo.contracts.DriverStatus;
import com.neepo.contracts.NetworkState;
import com.neepo.contracts.ObservationMetadata;
import com.neepo.contracts.ObservationStatus;
import com.neepo.contracts.RouteContext;
import com.neepo.contracts.RouteStatus;
import com.neepo.contracts.TripContext;
import com.neepo.contracts.TripStatus;
import com.neepo.contracts.VehicleContext;
import com.neepo.contracts.VehicleStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TripContextSnapshotServiceTest {
    private static final UUID COMPANY_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID TRIP_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID VEHICLE_ID = UUID.fromString("30000000-0000-0000-0000-000000000003");
    private static final UUID DRIVER_ID = UUID.fromString("40000000-0000-0000-0000-000000000004");
    private static final UUID CARGO_ID = UUID.fromString("50000000-0000-0000-0000-000000000005");
    private static final UUID ROUTE_ID = UUID.fromString("60000000-0000-0000-0000-000000000006");
    private static final Instant NOW = Instant.parse("2026-08-24T09:30:00Z");
    private static final String HASH = "A".repeat(64);

    private TripRepository trips;
    private TripContextSnapshotJdbcRepository snapshots;
    private TripContextSnapshotService service;

    @BeforeEach
    void setUp() {
        trips = mock(TripRepository.class);
        snapshots = mock(TripContextSnapshotJdbcRepository.class);
        service = new TripContextSnapshotService(trips, snapshots, JsonMapper.builder().build());
    }

    @Test
    void persistsExactContextFreshnessAndAllocatedSequence() {
        prepareActiveTrip(7);

        TripContextSnapshotRecord result = service.persist(
                COMPANY_ID, TRIP_ID, context(NOW.plusSeconds(300), HASH)
        );

        assertEquals(TRIP_ID, result.tripId());
        assertEquals(7, result.snapshotSequence());
        assertEquals(NOW, result.capturedAt());
        assertEquals(new BigDecimal("0.950"), result.minimumConfidence());
        assertFalse(result.hasExpiredRequiredData());
        assertEquals(NetworkState.ONLINE, result.networkState());
        assertEquals(HASH.toLowerCase(), result.contextHash());
        assertTrue(result.contextPayload().contains(TRIP_ID.toString()));
        assertTrue(result.contextPayload().contains(HASH));
        verify(snapshots).insert(result);
    }

    @Test
    void expiredRequiredEvidenceIsPersistedWithoutBeingHidden() {
        prepareActiveTrip(1);

        TripContextSnapshotRecord result = service.persist(COMPANY_ID, TRIP_ID, context(NOW, HASH));

        assertTrue(result.hasExpiredRequiredData());
    }

    @Test
    void locksTenantTripBeforeAllocatingSequenceAndWriting() {
        prepareActiveTrip(3);

        service.persist(COMPANY_ID, TRIP_ID, context(NOW.plusSeconds(60), HASH));

        InOrder order = inOrder(snapshots);
        order.verify(snapshots).lockTrip(COMPANY_ID, TRIP_ID);
        order.verify(snapshots).nextSequence(TRIP_ID);
        order.verify(snapshots).insert(any(TripContextSnapshotRecord.class));
    }

    @Test
    void missingTenantTripCannotCreateSnapshot() {
        when(trips.findByIdAndCompanyId(TRIP_ID, COMPANY_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.persist(COMPANY_ID, TRIP_ID, context(NOW.plusSeconds(60), HASH)));

        verifyNoInteractions(snapshots);
    }

    @Test
    void inactiveTripCannotCreateSnapshot() {
        when(trips.findByIdAndCompanyId(TRIP_ID, COMPANY_ID)).thenReturn(Optional.of(inactiveTrip()));

        assertThrows(ResourceConflictException.class,
                () -> service.persist(COMPANY_ID, TRIP_ID, context(NOW.plusSeconds(60), HASH)));

        verify(snapshots, never()).lockTrip(any(), any());
    }

    @Test
    void failedTenantRowLockCannotAllocateSequence() {
        when(trips.findByIdAndCompanyId(TRIP_ID, COMPANY_ID)).thenReturn(Optional.of(activeTrip()));
        when(snapshots.lockTrip(COMPANY_ID, TRIP_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> service.persist(COMPANY_ID, TRIP_ID, context(NOW.plusSeconds(60), HASH)));

        verify(snapshots, never()).nextSequence(any());
        verify(snapshots, never()).insert(any());
    }

    @Test
    void contextMustBelongToRequestedTripBeforeRepositoryAccess() {
        UUID anotherTrip = UUID.fromString("70000000-0000-0000-0000-000000000007");

        assertThrows(IllegalArgumentException.class,
                () -> service.persist(COMPANY_ID, anotherTrip, context(NOW.plusSeconds(60), HASH)));

        verifyNoInteractions(trips, snapshots);
    }

    @Test
    void snapshotRecordRejectsInvalidPersistenceBoundaries() {
        TripContextSnapshotRecord valid = new TripContextSnapshotRecord(
                UUID.randomUUID(), TRIP_ID, 1, NOW, new BigDecimal("0.950"), false,
                NetworkState.ONLINE, "{}", HASH
        );

        assertThrows(IllegalArgumentException.class, () -> new TripContextSnapshotRecord(
                valid.id(), TRIP_ID, 0, NOW, valid.minimumConfidence(), false,
                NetworkState.ONLINE, "{}", HASH
        ));
        assertThrows(IllegalArgumentException.class, () -> new TripContextSnapshotRecord(
                valid.id(), TRIP_ID, 1, NOW, new BigDecimal("1.001"), false,
                NetworkState.ONLINE, "{}", HASH
        ));
    }

    @Test
    void loadsAndIntegrityChecksPersistedContextPayload() {
        prepareActiveTrip(9);
        TripContext expected = context(NOW.plusSeconds(300), HASH);
        TripContextSnapshotRecord snapshot = service.persist(COMPANY_ID, TRIP_ID, expected);
        when(snapshots.findById(COMPANY_ID, TRIP_ID, snapshot.id())).thenReturn(Optional.of(snapshot));

        TripContext loaded = service.load(COMPANY_ID, TRIP_ID, snapshot.id());

        assertEquals(expected, loaded);
    }

    @Test
    void missingTenantSnapshotHasNotFoundOutcome() {
        UUID snapshotId = UUID.fromString("80000000-0000-0000-0000-000000000008");
        when(snapshots.findById(COMPANY_ID, TRIP_ID, snapshotId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                service.load(COMPANY_ID, TRIP_ID, snapshotId));
    }

    @Test
    void alteredSnapshotHashFailsIntegrityCheck() {
        prepareActiveTrip(10);
        TripContextSnapshotRecord valid = service.persist(
                COMPANY_ID, TRIP_ID, context(NOW.plusSeconds(300), HASH)
        );
        TripContextSnapshotRecord altered = new TripContextSnapshotRecord(
                valid.id(), valid.tripId(), valid.snapshotSequence(), valid.capturedAt(),
                valid.minimumConfidence(), valid.hasExpiredRequiredData(), valid.networkState(),
                valid.contextPayload(), "B".repeat(64)
        );
        when(snapshots.findById(COMPANY_ID, TRIP_ID, valid.id())).thenReturn(Optional.of(altered));

        assertThrows(IllegalStateException.class, () ->
                service.load(COMPANY_ID, TRIP_ID, valid.id()));
    }

    @Test
    void malformedSnapshotPayloadFailsClosed() {
        UUID snapshotId = UUID.fromString("80000000-0000-0000-0000-000000000008");
        TripContextSnapshotRecord malformed = new TripContextSnapshotRecord(
                snapshotId, TRIP_ID, 1, NOW, new BigDecimal("0.950"), false,
                NetworkState.ONLINE, "{not-json}", HASH
        );
        when(snapshots.findById(COMPANY_ID, TRIP_ID, snapshotId)).thenReturn(Optional.of(malformed));

        assertThrows(IllegalStateException.class, () ->
                service.load(COMPANY_ID, TRIP_ID, snapshotId));
    }

    private void prepareActiveTrip(long sequence) {
        when(trips.findByIdAndCompanyId(TRIP_ID, COMPANY_ID)).thenReturn(Optional.of(activeTrip()));
        when(snapshots.lockTrip(COMPANY_ID, TRIP_ID)).thenReturn(true);
        when(snapshots.nextSequence(TRIP_ID)).thenReturn(sequence);
    }

    private Trip activeTrip() {
        Trip trip = baseTrip();
        trip.assign(VEHICLE_ID, DRIVER_ID);
        trip.start(NOW.minusSeconds(600));
        return trip;
    }

    private Trip inactiveTrip() {
        return baseTrip();
    }

    private Trip baseTrip() {
        return Trip.create(
                COMPANY_ID, VEHICLE_ID, DRIVER_ID, CARGO_ID, ROUTE_ID, "TRIP-CARE-1",
                NOW.plusSeconds(3600), 60, 30
        );
    }

    private TripContext context(Instant expiresAt, String hash) {
        ObservationMetadata metadata = new ObservationMetadata(
                NOW.minusSeconds(10), NOW.minusSeconds(9), expiresAt, 0.95,
                ObservationStatus.FRESH, "SIMULATOR"
        );
        return new TripContext(
                new com.neepo.contracts.Trip(
                        TRIP_ID, COMPANY_ID, VEHICLE_ID, DRIVER_ID, CARGO_ID, ROUTE_ID,
                        "TRIP-CARE-1", TripStatus.IN_PROGRESS, NOW.plusSeconds(3600), 60, 30,
                        NOW.minusSeconds(600), NOW.minusSeconds(700), NOW.minusSeconds(5)
                ),
                new VehicleContext(
                        VEHICLE_ID, "KA01AB1234", "REFRIGERATED_TRUCK", VehicleStatus.IN_TRANSIT,
                        null, null, null, null, null, null, null, true, List.of(), metadata
                ),
                new DriverContext(DRIVER_ID, DriverStatus.DRIVING, 300, 540, NOW.minusSeconds(3600), metadata),
                new CargoContext(CARGO_ID, CargoType.REFRIGERATED, null, null, null, null,
                        false, List.of(), metadata),
                new RouteContext(ROUTE_ID, RouteStatus.ACTIVE, null, null, null, null,
                        60, List.of(), metadata),
                List.of(), List.of(), List.of(), NetworkState.ONLINE, List.of(), null, NOW, hash
        );
    }
}
