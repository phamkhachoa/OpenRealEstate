package com.proptech.realestate.dto;

import com.proptech.realestate.model.entity.MLSMembership;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class SearchMembershipRequest {
    private UUID userId;
    private UUID regionId;
    private UUID officeId;
    private MLSMembership.MembershipType type;
    private MLSMembership.MembershipStatus status;
} 