package com.proptech.realestate.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class MembershipStats {
    private UUID regionId;
    private String regionName;
    private long totalMembers;
    private long activeMembers;
    private long pendingMembers;
    private long suspendedMembers;
    private long terminatedMembers;
    private long expiredMembers;
    private long brokers;
    private long agents;
} 