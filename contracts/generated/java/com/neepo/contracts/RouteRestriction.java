package com.neepo.contracts;

import java.math.BigDecimal;
import java.util.UUID;

public record RouteRestriction(
        UUID restrictionId,
        String restrictionCode,
        String restrictionKind,
        BigDecimal limitValue,
        String unit,
        boolean blocked,
        ObservationMetadata observation
) {
}
