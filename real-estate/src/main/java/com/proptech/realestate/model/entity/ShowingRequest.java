package com.proptech.realestate.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a property showing request between listing and selling agents.
 * Manages showing appointments and coordination for MLS cooperation.
 */
@Entity
@Table(name = "showing_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShowingRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cooperating_agreement_id", nullable = false)
    private CooperatingAgreement cooperatingAgreement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requesting_agent_id", nullable = false)
    private MLSMembership requestingAgent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requesting_office_id", nullable = false)
    private Office requestingOffice;

    @Column(name = "requested_date", nullable = false)
    private LocalDateTime requestedDate;

    @Column(name = "requested_duration_minutes")
    private Integer requestedDurationMinutes;

    @Column(name = "alternative_date_1")
    private LocalDateTime alternativeDate1;

    @Column(name = "alternative_date_2")
    private LocalDateTime alternativeDate2;

    @Column(name = "confirmed_date")
    private LocalDateTime confirmedDate;

    @Column(name = "confirmed_duration_minutes")
    private Integer confirmedDurationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ShowingStatus status = ShowingStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "showing_type", nullable = false)
    private ShowingType showingType;

    @Column(name = "number_of_buyers")
    private Integer numberOfBuyers;

    @Column(name = "buyer_prequalified", nullable = false)
    @Builder.Default
    private Boolean buyerPrequalified = false;

    @Column(name = "prequalification_amount")
    private java.math.BigDecimal prequalificationAmount;

    @Column(name = "lender_name")
    private String lenderName;

    @Column(name = "buyer_agent_phone", length = 20)
    private String buyerAgentPhone;

    @Column(name = "buyer_agent_email", length = 100)
    private String buyerAgentEmail;

    @Column(name = "special_instructions", length = 1000)
    private String specialInstructions;

    @Column(name = "access_method")
    private String accessMethod; // Lockbox, Owner Present, Agent Present, etc.

    @Column(name = "lockbox_code")
    private String lockboxCode;

    @Column(name = "contact_prior_minutes")
    private Integer contactPriorMinutes; // How many minutes before to contact

    @Column(name = "requires_approval", nullable = false)
    @Builder.Default
    private Boolean requiresApproval = false;

    @Column(name = "approved_by_listing_agent", nullable = false)
    @Builder.Default
    private Boolean approvedByListingAgent = false;

    @Column(name = "approval_date")
    private LocalDateTime approvalDate;

    @Column(name = "approved_by_user_id")
    private UUID approvedByUserId;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "showing_notes", length = 1000)
    private String showingNotes;

    @Column(name = "feedback_provided", nullable = false)
    @Builder.Default
    private Boolean feedbackProvided = false;

    @Column(name = "buyer_interest_level")
    private Integer buyerInterestLevel; // 1-10 scale

    @Column(name = "buyer_feedback", length = 2000)
    private String buyerFeedback;

    @Column(name = "agent_feedback", length = 1000)
    private String agentFeedback;

    @Column(name = "showing_completed", nullable = false)
    @Builder.Default
    private Boolean showingCompleted = false;

    @Column(name = "completion_date")
    private LocalDateTime completionDate;

    @Column(name = "no_show", nullable = false)
    @Builder.Default
    private Boolean noShow = false;

    @Column(name = "cancelled_by_buyer", nullable = false)
    @Builder.Default
    private Boolean cancelledByBuyer = false;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "rescheduled", nullable = false)
    @Builder.Default
    private Boolean rescheduled = false;

    @Column(name = "reschedule_count")
    @Builder.Default
    private Integer rescheduleCount = 0;

    @Column(name = "follow_up_required", nullable = false)
    @Builder.Default
    private Boolean followUpRequired = false;

    @Column(name = "follow_up_date")
    private LocalDateTime followUpDate;

    @Column(name = "follow_up_notes", length = 1000)
    private String followUpNotes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    // Enums
    public enum ShowingStatus {
        PENDING,
        APPROVED,
        REJECTED,
        CONFIRMED,
        COMPLETED,
        CANCELLED,
        NO_SHOW,
        RESCHEDULED
    }

    public enum ShowingType {
        BUYER_SHOWING,
        AGENT_PREVIEW,
        BROKER_TOUR,
        INSPECTION,
        APPRAISAL,
        PHOTOGRAPHER,
        VIRTUAL_TOUR,
        OPEN_HOUSE
    }

    // Business methods
    public boolean isPending() {
        return status == ShowingStatus.PENDING;
    }

    public boolean isApproved() {
        return status == ShowingStatus.APPROVED || status == ShowingStatus.CONFIRMED;
    }

    public boolean isCompleted() {
        return status == ShowingStatus.COMPLETED;
    }

    public boolean isCancelled() {
        return status == ShowingStatus.CANCELLED;
    }

    public boolean isOverdue() {
        return confirmedDate != null && 
               LocalDateTime.now().isAfter(confirmedDate.plusMinutes(confirmedDurationMinutes != null ? confirmedDurationMinutes : 60)) &&
               !showingCompleted;
    }

    public boolean isUpcoming() {
        return confirmedDate != null && 
               confirmedDate.isAfter(LocalDateTime.now()) && 
               status == ShowingStatus.CONFIRMED;
    }

    public boolean needsContactPrior() {
        return contactPriorMinutes != null && 
               confirmedDate != null &&
               LocalDateTime.now().isAfter(confirmedDate.minusMinutes(contactPriorMinutes)) &&
               LocalDateTime.now().isBefore(confirmedDate);
    }

    public long getMinutesUntilShowing() {
        if (confirmedDate == null) return 0;
        return java.time.temporal.ChronoUnit.MINUTES.between(LocalDateTime.now(), confirmedDate);
    }

    public void approve(UUID approvedByUserId) {
        this.status = ShowingStatus.APPROVED;
        this.approvedByListingAgent = true;
        this.approvalDate = LocalDateTime.now();
        this.approvedByUserId = approvedByUserId;
    }

    public void reject(String reason) {
        this.status = ShowingStatus.REJECTED;
        this.rejectionReason = reason;
        this.approvedByListingAgent = false;
    }

    public void confirm(LocalDateTime confirmedDateTime, Integer durationMinutes) {
        this.status = ShowingStatus.CONFIRMED;
        this.confirmedDate = confirmedDateTime;
        this.confirmedDurationMinutes = durationMinutes;
    }

    public void complete() {
        this.status = ShowingStatus.COMPLETED;
        this.showingCompleted = true;
        this.completionDate = LocalDateTime.now();
    }

    public void markNoShow() {
        this.status = ShowingStatus.NO_SHOW;
        this.noShow = true;
        this.completionDate = LocalDateTime.now();
    }

    public void cancel(String reason, boolean byBuyer) {
        this.status = ShowingStatus.CANCELLED;
        this.cancellationReason = reason;
        this.cancelledByBuyer = byBuyer;
    }

    public void reschedule(LocalDateTime newDate) {
        this.status = ShowingStatus.PENDING;
        this.requestedDate = newDate;
        this.confirmedDate = null;
        this.rescheduled = true;
        this.rescheduleCount++;
    }

    public void addFeedback(Integer interestLevel, String buyerComments, String agentComments) {
        this.buyerInterestLevel = interestLevel;
        this.buyerFeedback = buyerComments;
        this.agentFeedback = agentComments;
        this.feedbackProvided = true;
    }

    public void scheduleFollowUp(LocalDateTime followUpDateTime, String notes) {
        this.followUpRequired = true;
        this.followUpDate = followUpDateTime;
        this.followUpNotes = notes;
    }

    public void completeFollowUp() {
        this.followUpRequired = false;
        this.followUpDate = LocalDateTime.now();
    }

    public String getStatusSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append(status.toString());
        
        if (confirmedDate != null) {
            summary.append(" for ").append(confirmedDate.toString());
        }
        
        if (rescheduled && rescheduleCount > 0) {
            summary.append(" (rescheduled ").append(rescheduleCount).append(" times)");
        }
        
        return summary.toString();
    }

    public boolean allowsRescheduling() {
        return rescheduleCount < 3 && // Maximum 3 reschedules
               !isCompleted() && !isCancelled();
    }

    public boolean requiresFeedback() {
        return showingCompleted && !feedbackProvided;
    }
} 