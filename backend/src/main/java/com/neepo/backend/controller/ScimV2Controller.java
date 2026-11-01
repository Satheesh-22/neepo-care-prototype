package com.neepo.backend.controller;

import com.neepo.backend.dto.ScimListResponse;
import com.neepo.backend.dto.ScimPatchOpRequest;
import com.neepo.backend.dto.ScimUserDto;
import com.neepo.backend.service.ScimService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/scim/v2", produces = {"application/scim+json", "application/json"})
public class ScimV2Controller {

    private final ScimService scimService;

    public ScimV2Controller(ScimService scimService) {
        this.scimService = scimService;
    }

    @GetMapping("/ServiceProviderConfig")
    public ResponseEntity<Map<String, Object>> getServiceProviderConfig(HttpServletRequest request) {
        return ResponseEntity.ok(Map.of(
                "schemas", List.of("urn:ietf:params:scim:schemas:core:2.0:ServiceProviderConfig"),
                "documentationUri", "https://docs.neepo-care.com/scim",
                "patch", Map.of("supported", true),
                "bulk", Map.of("supported", false, "maxOperations", 0, "maxPayloadSize", 0),
                "filter", Map.of("supported", true, "maxResults", 100),
                "changePassword", Map.of("supported", false),
                "sort", Map.of("supported", false),
                "etag", Map.of("supported", false),
                "authenticationSchemes", List.of(
                        Map.of(
                                "name", "OAuth Bearer Token",
                                "description", "Authentication Scheme using the OAuth Bearer Token Standard",
                                "specUri", "https://www.rfc-editor.org/info/rfc6750",
                                "type", "oauthbearertoken",
                                "primary", true
                        )
                )
        ));
    }

    @GetMapping("/Schemas")
    public ResponseEntity<Map<String, Object>> getSchemas() {
        return ResponseEntity.ok(Map.of(
                "schemas", List.of("urn:ietf:params:scim:api:messages:2.0:ListResponse"),
                "totalResults", 1,
                "startIndex", 1,
                "itemsPerPage", 1,
                "Resources", List.of(
                        Map.of(
                                "id", "urn:ietf:params:scim:schemas:core:2.0:User",
                                "name", "User",
                                "description", "User Account Schema"
                        )
                )
        ));
    }

    @GetMapping("/Users")
    public ResponseEntity<ScimListResponse<ScimUserDto>> listUsers(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(name = "startIndex", defaultValue = "1") int startIndex,
            @RequestParam(name = "count", defaultValue = "50") int count,
            HttpServletRequest request
    ) {
        UUID companyId = scimService.authenticateScimBearerToken(authHeader);
        String baseUrl = extractBaseUrl(request);
        return ResponseEntity.ok(scimService.listScimUsers(companyId, startIndex, count, baseUrl));
    }

    @GetMapping("/Users/{id}")
    public ResponseEntity<ScimUserDto> getUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable("id") UUID userId,
            HttpServletRequest request
    ) {
        UUID companyId = scimService.authenticateScimBearerToken(authHeader);
        String baseUrl = extractBaseUrl(request);
        return ResponseEntity.ok(scimService.getScimUser(companyId, userId, baseUrl));
    }

    @PostMapping("/Users")
    public ResponseEntity<ScimUserDto> createUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody ScimUserDto userDto,
            HttpServletRequest request
    ) {
        UUID companyId = scimService.authenticateScimBearerToken(authHeader);
        String baseUrl = extractBaseUrl(request);
        ScimUserDto created = scimService.createScimUser(companyId, userDto, baseUrl);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/Users/{id}")
    public ResponseEntity<ScimUserDto> updateUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable("id") UUID userId,
            @RequestBody ScimUserDto userDto,
            HttpServletRequest request
    ) {
        UUID companyId = scimService.authenticateScimBearerToken(authHeader);
        String baseUrl = extractBaseUrl(request);
        return ResponseEntity.ok(scimService.updateScimUser(companyId, userId, userDto, baseUrl));
    }

    @PatchMapping("/Users/{id}")
    public ResponseEntity<ScimUserDto> patchUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable("id") UUID userId,
            @RequestBody ScimPatchOpRequest patchRequest,
            HttpServletRequest request
    ) {
        UUID companyId = scimService.authenticateScimBearerToken(authHeader);
        String baseUrl = extractBaseUrl(request);
        return ResponseEntity.ok(scimService.patchScimUser(companyId, userId, patchRequest, baseUrl));
    }

    @DeleteMapping("/Users/{id}")
    public ResponseEntity<Void> deleteUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable("id") UUID userId
    ) {
        UUID companyId = scimService.authenticateScimBearerToken(authHeader);
        scimService.deleteScimUser(companyId, userId);
        return ResponseEntity.noContent().build();
    }

    private String extractBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int port = request.getServerPort();
        boolean isDefaultPort = ("http".equalsIgnoreCase(scheme) && port == 80)
                || ("https".equalsIgnoreCase(scheme) && port == 443);

        return isDefaultPort ? scheme + "://" + serverName : scheme + "://" + serverName + ":" + port;
    }
}

