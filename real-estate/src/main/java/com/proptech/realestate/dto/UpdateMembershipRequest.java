package com.proptech.realestate.dto;

import com.proptech.realestate.model.entity.MLSMembership;
import lombok.Builder;
import lombok.Data;

import jakarta.validation.constraints.Future;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class UpdateMembershipRequest {

    private MLSMembership.MembershipType type;

    private MLSMembership.MembershipStatus status;

    @Future
    private LocalDate endDate;

    private BigDecimal annualFee;

    private UUID officeId;

    private String licenseNumber;

    private String licenseState;

    @Future
    private LocalDate licenseExpiryDate;

    private String billingAddress;

    private String paymentMethod;
} 