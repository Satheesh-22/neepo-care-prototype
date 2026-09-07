package com.neepo.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "companies")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "legal_name", length = 150)
    private String legalName;

    @Column(name = "trading_name", length = 150)
    private String tradingName;

    @Column(name = "registration_number", length = 80, unique = true)
    private String registrationNumber;

    @Column(length = 80)
    private String industry;

    @Column(length = 255)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(name = "state_province", length = 100)
    private String stateProvince;

    @Column(length = 100)
    private String country;

    @Column(name = "postal_code", length = 30)
    private String postalCode;

    @Column(name = "contact_person", length = 100)
    private String contactPerson;

    @Column(name = "business_email", length = 150)
    private String businessEmail;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Column(nullable = false, length = 60)
    private String timezone = "UTC";

    @Column(name = "measurement_units", nullable = false, length = 20)
    private String measurementUnits = "METRIC";

    @Column(name = "cargo_temperature_policy", nullable = false, length = 60)
    private String cargoTemperaturePolicy = "STRICT_COLD_CHAIN";

    @Column(name = "logo_reference", length = 255)
    private String logoReference;

    @Column(name = "data_retention_days", nullable = false)
    private int dataRetentionDays = 90;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected Company() {
    }

    public static Company create(String name, String code, String legalName, String tradingName,
                                 String registrationNumber, String industry, String address,
                                 String city, String stateProvince, String country, String postalCode,
                                 String contactPerson, String businessEmail, String phoneNumber,
                                 String timezone, String measurementUnits, String cargoTemperaturePolicy,
                                 String logoReference, int dataRetentionDays) {
        Company company = new Company();
        company.name = name;
        company.code = code;
        company.legalName = legalName;
        company.tradingName = tradingName;
        company.registrationNumber = registrationNumber;
        company.industry = industry;
        company.address = address;
        company.city = city;
        company.stateProvince = stateProvince;
        company.country = country;
        company.postalCode = postalCode;
        company.contactPerson = contactPerson;
        company.businessEmail = businessEmail;
        company.phoneNumber = phoneNumber;
        company.timezone = timezone != null ? timezone : "UTC";
        company.measurementUnits = measurementUnits != null ? measurementUnits : "METRIC";
        company.cargoTemperaturePolicy = cargoTemperaturePolicy != null ? cargoTemperaturePolicy : "STRICT_COLD_CHAIN";
        company.logoReference = logoReference;
        company.dataRetentionDays = dataRetentionDays > 0 ? dataRetentionDays : 90;
        return company;
    }

    public void updateProfile(String legalName, String tradingName, String registrationNumber,
                              String industry, String address, String city, String stateProvince,
                              String country, String postalCode, String contactPerson,
                              String businessEmail, String phoneNumber, String timezone,
                              String measurementUnits, String cargoTemperaturePolicy,
                              String logoReference, int dataRetentionDays) {
        this.legalName = legalName;
        this.tradingName = tradingName;
        this.name = tradingName != null && !tradingName.isBlank() ? tradingName : legalName;
        this.registrationNumber = registrationNumber;
        this.industry = industry;
        this.address = address;
        this.city = city;
        this.stateProvince = stateProvince;
        this.country = country;
        this.postalCode = postalCode;
        this.contactPerson = contactPerson;
        this.businessEmail = businessEmail;
        this.phoneNumber = phoneNumber;
        if (timezone != null && !timezone.isBlank()) {
            this.timezone = timezone;
        }
        if (measurementUnits != null && !measurementUnits.isBlank()) {
            this.measurementUnits = measurementUnits;
        }
        if (cargoTemperaturePolicy != null && !cargoTemperaturePolicy.isBlank()) {
            this.cargoTemperaturePolicy = cargoTemperaturePolicy;
        }
        this.logoReference = logoReference;
        if (dataRetentionDays > 0) {
            this.dataRetentionDays = dataRetentionDays;
        }
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public String getLegalName() { return legalName; }
    public String getTradingName() { return tradingName; }
    public String getRegistrationNumber() { return registrationNumber; }
    public String getIndustry() { return industry; }
    public String getAddress() { return address; }
    public String getCity() { return city; }
    public String getStateProvince() { return stateProvince; }
    public String getCountry() { return country; }
    public String getPostalCode() { return postalCode; }
    public String getContactPerson() { return contactPerson; }
    public String getBusinessEmail() { return businessEmail; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getTimezone() { return timezone; }
    public String getMeasurementUnits() { return measurementUnits; }
    public String getCargoTemperaturePolicy() { return cargoTemperaturePolicy; }
    public String getLogoReference() { return logoReference; }
    public int getDataRetentionDays() { return dataRetentionDays; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
