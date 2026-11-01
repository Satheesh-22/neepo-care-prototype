package com.neepo.backend.entity;

import com.neepo.backend.domain.IdpType;
import com.neepo.backend.domain.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "company_sso_configs")
public class CompanySsoConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "company_id", nullable = false, unique = true)
    private UUID companyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "idp_type", nullable = false, length = 32)
    private IdpType idpType = IdpType.OIDC;

    @Column(name = "idp_issuer_url", nullable = false, length = 255)
    private String idpIssuerUrl;

    @Column(name = "client_id", nullable = false, length = 128)
    private String clientId;

    @Column(name = "client_secret_hash", nullable = false, length = 128)
    private String clientSecretHash;

    @Column(name = "sso_domain", nullable = false, unique = true, length = 128)
    private String ssoDomain;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "enforce_sso", nullable = false)
    private boolean enforceSso = false;

    @Column(name = "jit_provisioning_enabled", nullable = false)
    private boolean jitProvisioningEnabled = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_role", nullable = false, length = 32)
    private UserRole defaultRole = UserRole.DISPATCHER;

    @Column(name = "scim_enabled", nullable = false)
    private boolean scimEnabled = false;

    @Column(name = "scim_token_hash", length = 128)
    private String scimTokenHash;

    @Column(name = "scim_token_prefix", length = 32)
    private String scimTokenPrefix;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata = "{}";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    public CompanySsoConfig() {
    }

    public CompanySsoConfig(UUID companyId, IdpType idpType, String idpIssuerUrl, String clientId,
                            String clientSecretHash, String ssoDomain) {
        this.companyId = companyId;
        this.idpType = idpType;
        this.idpIssuerUrl = idpIssuerUrl;
        this.clientId = clientId;
        this.clientSecretHash = clientSecretHash;
        this.ssoDomain = ssoDomain != null ? ssoDomain.toLowerCase().trim() : null;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
    }

    public IdpType getIdpType() {
        return idpType;
    }

    public void setIdpType(IdpType idpType) {
        this.idpType = idpType;
    }

    public String getIdpIssuerUrl() {
        return idpIssuerUrl;
    }

    public void setIdpIssuerUrl(String idpIssuerUrl) {
        this.idpIssuerUrl = idpIssuerUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecretHash() {
        return clientSecretHash;
    }

    public void setClientSecretHash(String clientSecretHash) {
        this.clientSecretHash = clientSecretHash;
    }

    public String getSsoDomain() {
        return ssoDomain;
    }

    public void setSsoDomain(String ssoDomain) {
        this.ssoDomain = ssoDomain != null ? ssoDomain.toLowerCase().trim() : null;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnforceSso() {
        return enforceSso;
    }

    public void setEnforceSso(boolean enforceSso) {
        this.enforceSso = enforceSso;
    }

    public boolean isJitProvisioningEnabled() {
        return jitProvisioningEnabled;
    }

    public void setJitProvisioningEnabled(boolean jitProvisioningEnabled) {
        this.jitProvisioningEnabled = jitProvisioningEnabled;
    }

    public UserRole getDefaultRole() {
        return defaultRole;
    }

    public void setDefaultRole(UserRole defaultRole) {
        this.defaultRole = defaultRole;
    }

    public boolean isScimEnabled() {
        return scimEnabled;
    }

    public void setScimEnabled(boolean scimEnabled) {
        this.scimEnabled = scimEnabled;
    }

    public String getScimTokenHash() {
        return scimTokenHash;
    }

    public void setScimTokenHash(String scimTokenHash) {
        this.scimTokenHash = scimTokenHash;
    }

    public String getScimTokenPrefix() {
        return scimTokenPrefix;
    }

    public void setScimTokenPrefix(String scimTokenPrefix) {
        this.scimTokenPrefix = scimTokenPrefix;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }
}
