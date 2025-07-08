package com.proptech.realestate.repository;

import com.proptech.realestate.model.entity.ListingAgreement;
import com.proptech.realestate.model.entity.ListingAgreement.AgreementStatus;
import com.proptech.realestate.model.entity.ListingAgreement.AgreementType;
import com.proptech.realestate.model.entity.ListingAgreement.CooperationLevel;
import com.proptech.realestate.model.entity.Listing;
import com.proptech.realestate.model.entity.MLSMembership;
import com.proptech.realestate.model.entity.Office;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for ListingAgreement entity.
 * Provides data access methods for MLS listing cooperation management.
 */
@Repository
public interface ListingAgreementRepository extends JpaRepository<ListingAgreement, UUID> {

    /**
     * Find agreement by listing.
     */
    Optional<ListingAgreement> findByListing(Listing listing);

    /**
     * Find agreement by listing ID.
     */
    @Query("SELECT la FROM ListingAgreement la WHERE la.listing.id = :listingId")
    Optional<ListingAgreement> findByListingId(@Param("listingId") UUID listingId);

    /**
     * Find agreements by listing agent.
     */
    List<ListingAgreement> findByListingAgent(MLSMembership listingAgent);

    /**
     * Find agreements by listing office.
     */
    List<ListingAgreement> findByListingOffice(Office listingOffice);

    /**
     * Find agreements by status.
     */
    List<ListingAgreement> findByStatus(AgreementStatus status);

    /**
     * Find active agreements.
     */
    @Query("SELECT la FROM ListingAgreement la WHERE la.status = 'ACTIVE' AND la.endDate >= :currentDate")
    List<ListingAgreement> findActiveAgreements(@Param("currentDate") LocalDate currentDate);

    /**
     * Find agreements by cooperation level.
     */
    List<ListingAgreement> findByCooperationLevel(CooperationLevel cooperationLevel);

    /**
     * Find agreements by agreement type.
     */
    List<ListingAgreement> findByAgreementType(AgreementType agreementType);

    /**
     * Find agreements allowing cooperation.
     */
    @Query("SELECT la FROM ListingAgreement la WHERE la.cooperationLevel != 'NO_COOPERATION' AND la.status = 'ACTIVE'")
    List<ListingAgreement> findAgreementsAllowingCooperation();

    /**
     * Find agreements allowing syndication.
     */
    @Query("SELECT la FROM ListingAgreement la WHERE la.syndicationAllowed = true AND la.status = 'ACTIVE'")
    List<ListingAgreement> findAgreementsAllowingSyndication();

    /**
     * Find expiring agreements.
     */
    @Query("SELECT la FROM ListingAgreement la WHERE la.endDate BETWEEN :startDate AND :endDate AND la.status = 'ACTIVE'")
    List<ListingAgreement> findExpiringAgreements(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Find agreements needing renewal.
     */
    @Query("SELECT la FROM ListingAgreement la WHERE la.autoRenewal = true AND la.endDate <= :renewalDate AND la.status = 'ACTIVE'")
    List<ListingAgreement> findAgreementsNeedingRenewal(@Param("renewalDate") LocalDate renewalDate);

    /**
     * Find expired agreements.
     */
    @Query("SELECT la FROM ListingAgreement la WHERE la.endDate < :currentDate OR la.status = 'EXPIRED'")
    List<ListingAgreement> findExpiredAgreements(@Param("currentDate") LocalDate currentDate);

    /**
     * Find agreements by office and status.
     */
    List<ListingAgreement> findByListingOfficeAndStatus(Office office, AgreementStatus status);

    /**
     * Find agreements by agent and status.
     */
    List<ListingAgreement> findByListingAgentAndStatus(MLSMembership agent, AgreementStatus status);

    /**
     * Find agreements by date range.
     */
    @Query("SELECT la FROM ListingAgreement la WHERE la.startDate >= :startDate AND la.endDate <= :endDate")
    List<ListingAgreement> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Count agreements by office.
     */
    long countByListingOffice(Office office);

    /**
     * Count agreements by agent.
     */
    long countByListingAgent(MLSMembership agent);

    /**
     * Count active agreements by office.
     */
    @Query("SELECT COUNT(la) FROM ListingAgreement la WHERE la.listingOffice = :office AND la.status = 'ACTIVE'")
    long countActiveByOffice(@Param("office") Office office);

    /**
     * Count active agreements by agent.
     */
    @Query("SELECT COUNT(la) FROM ListingAgreement la WHERE la.listingAgent = :agent AND la.status = 'ACTIVE'")
    long countActiveByAgent(@Param("agent") MLSMembership agent);

    /**
     * Find agreements by exclusive status.
     */
    List<ListingAgreement> findByIsExclusive(Boolean isExclusive);

    /**
     * Find agreements with cooperation restrictions.
     */
    @Query("SELECT la FROM ListingAgreement la WHERE la.cooperationLevel IN ('LIMITED_COOPERATION', 'APPROVAL_REQUIRED', 'MLS_MEMBERS_ONLY')")
    List<ListingAgreement> findAgreementsWithCooperationRestrictions();

    /**
     * Find agreements by listing type and status.
     */
    List<ListingAgreement> findByListingTypeAndStatus(ListingAgreement.ListingType listingType, AgreementStatus status);

    /**
     * Find agreements with special terms.
     */
    @Query("SELECT la FROM ListingAgreement la WHERE la.specialTerms IS NOT NULL AND la.specialTerms != ''")
    List<ListingAgreement> findAgreementsWithSpecialTerms();

    /**
     * Find agreements requiring showing instructions.
     */
    List<ListingAgreement> findByRequiresShowingInstructions(Boolean requiresShowingInstructions);

    /**
     * Find agreements with commission exceptions.
     */
    @Query("SELECT la FROM ListingAgreement la WHERE la.compensationExceptions IS NOT NULL AND la.compensationExceptions != ''")
    List<ListingAgreement> findAgreementsWithCommissionExceptions();

    /**
     * Find agreements by commission structure type.
     */
    List<ListingAgreement> findByCommissionStructure(ListingAgreement.CommissionStructureType commissionStructure);

    /**
     * Check if listing has active agreement.
     */
    @Query("SELECT CASE WHEN COUNT(la) > 0 THEN true ELSE false END FROM ListingAgreement la WHERE la.listing.id = :listingId AND la.status = 'ACTIVE'")
    boolean hasActiveAgreement(@Param("listingId") UUID listingId);

    /**
     * Find agreements created by user.
     */
    List<ListingAgreement> findByCreatedBy(UUID createdBy);

    /**
     * Find agreements updated by user.
     */
    List<ListingAgreement> findByUpdatedBy(UUID updatedBy);

    /**
     * Search agreements by criteria.
     */
    @Query("SELECT la FROM ListingAgreement la WHERE " +
           "(:status IS NULL OR la.status = :status) AND " +
           "(:agreementType IS NULL OR la.agreementType = :agreementType) AND " +
           "(:cooperationLevel IS NULL OR la.cooperationLevel = :cooperationLevel) AND " +
           "(:isExclusive IS NULL OR la.isExclusive = :isExclusive) AND " +
           "(:syndicationAllowed IS NULL OR la.syndicationAllowed = :syndicationAllowed) AND " +
           "(:autoRenewal IS NULL OR la.autoRenewal = :autoRenewal)")
    List<ListingAgreement> searchAgreements(
        @Param("status") AgreementStatus status,
        @Param("agreementType") AgreementType agreementType,
        @Param("cooperationLevel") CooperationLevel cooperationLevel,
        @Param("isExclusive") Boolean isExclusive,
        @Param("syndicationAllowed") Boolean syndicationAllowed,
        @Param("autoRenewal") Boolean autoRenewal
    );

    /**
     * Find agreements with lockbox access.
     */
    List<ListingAgreement> findByKeyBoxAccess(Boolean keyBoxAccess);

    /**
     * Find agreements by lockbox number.
     */
    Optional<ListingAgreement> findByLockboxNumber(String lockboxNumber);

    /**
     * Find agreements with variable commission notes.
     */
    @Query("SELECT la FROM ListingAgreement la WHERE la.variableCommissionNotes IS NOT NULL AND la.variableCommissionNotes != ''")
    List<ListingAgreement> findAgreementsWithVariableCommissionNotes();
} 