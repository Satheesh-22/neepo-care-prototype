package com.neepo.backend.dto;

import java.math.BigDecimal;

public record GeoPointResponse(BigDecimal latitude, BigDecimal longitude) {
}
