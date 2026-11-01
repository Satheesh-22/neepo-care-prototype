package com.neepo.backend.controller;

import com.neepo.backend.dto.CreateDepotRequest;
import com.neepo.backend.dto.DepotResponse;
import com.neepo.backend.dto.UpdateDepotRequest;
import com.neepo.backend.config.TenantScope;
import com.neepo.backend.service.DepotService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class DepotController {

    private final DepotService depotService;

    public DepotController(DepotService depotService) {
        this.depotService = depotService;
    }

    @GetMapping("/depots")
    public ResponseEntity<List<DepotResponse>> listDepots(@AuthenticationPrincipal Jwt jwt) {
        UUID companyId = TenantScope.companyId(jwt);
        return ResponseEntity.ok(depotService.listDepots(companyId));
    }

    @GetMapping("/depots/{id}")
    public ResponseEntity<DepotResponse> getDepot(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") UUID depotId
    ) {
        UUID companyId = TenantScope.companyId(jwt);
        return ResponseEntity.ok(depotService.getDepot(companyId, depotId));
    }

    @PostMapping("/admin/depots")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepotResponse> createDepot(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateDepotRequest request
    ) {
        UUID companyId = TenantScope.companyId(jwt);
        DepotResponse created = depotService.createDepot(companyId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/admin/depots/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepotResponse> updateDepot(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") UUID depotId,
            @Valid @RequestBody UpdateDepotRequest request
    ) {
        UUID companyId = TenantScope.companyId(jwt);
        return ResponseEntity.ok(depotService.updateDepot(companyId, depotId, request));
    }

    @DeleteMapping("/admin/depots/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDepot(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") UUID depotId
    ) {
        UUID companyId = TenantScope.companyId(jwt);
        depotService.deleteDepot(companyId, depotId);
        return ResponseEntity.noContent().build();
    }
}
