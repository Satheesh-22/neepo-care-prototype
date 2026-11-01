package com.neepo.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public record ScimPatchOpRequest(
        List<String> schemas,
        @JsonProperty("Operations") List<Operation> operations
) {
    public record Operation(
            String op,
            String path,
            Object value
    ) {}
}

