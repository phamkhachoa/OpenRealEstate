package com.proptech.realestate.dto;

import com.proptech.realestate.model.entity.MLSMembership;
import lombok.Builder;
import lombok.Data;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class CreateMembershipRequest {
    @NotNull
    private UUID userId;

    @NotNull
    private UUID regionId;

    private UUID officeId;

    @NotNull
    private MLSMembership.MembershipType type;

    private LocalDate startDate;

    @Future
    private LocalDate endDate;

    private BigDecimal annualFee;

    @NotBlank
    private String licenseNumber;

    private String licenseState;

    @Future
    private LocalDate licenseExpiryDate;

    private String billingAddress;
    
    private String paymentMethod;
} 