package com.neepo.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

@Entity
@Immutable
@Table(name = "routes")
public class RouteReference {
    @Id
    private UUID id;
    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    protected RouteReference() {
    }

    public static RouteReference reference(UUID id, UUID companyId) {
        RouteReference route = new RouteReference();
        route.id = id;
        route.companyId = companyId;
        return route;
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
}
