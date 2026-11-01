package com.neepo.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ScimListResponse<T>(
        List<String> schemas,
        long totalResults,
        int startIndex,
        int itemsPerPage,
        @JsonProperty("Resources") List<T> resources
) {}

