package com.neepo.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ScimUserDto(
        List<String> schemas,
        String id,
        String externalId,
        String userName,
        Name name,
        List<Email> emails,
        List<PhoneNumber> phoneNumbers,
        Boolean active,
        String userType,
        Meta meta
) {
    public record Name(String formatted, String familyName, String givenName) {}
    public record Email(String value, Boolean primary, String type) {}
    public record PhoneNumber(String value, String type) {}
    public record Meta(String resourceType, String created, String lastModified, String location) {}
}

