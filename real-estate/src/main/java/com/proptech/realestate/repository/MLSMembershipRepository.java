package com.proptech.realestate.repository;

import com.proptech.realestate.model.entity.MLSMembership;
import com.proptech.realestate.model.entity.MLSMembership.MembershipStatus;
import com.proptech.realestate.model.entity.MLSMembership.MembershipType;
import com.proptech.realestate.model.entity.MLSRegion;
import com.proptech.realestate.model.entity.Office;
import com.proptech.realestate.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for MLSMembership entity.
 * Provides CRUD operations and custom queries for MLS memberships.
 */
@Repository
public interface MLSMembershipRepository extends JpaRepository<MLSMembership, UUID> {

    /**
     * Find memberships by user and status.
     */
    List<MLSMembership> findByUserAndStatus(User user, MembershipStatus status);

    /**
     * Find memberships by user ID and status.
     */
    List<MLSMembership> findByUserIdAndStatus(UUID userId, MembershipStatus status);

    /**
     * Find active memberships for a user.
     */
    @Query("SELECT m FROM MLSMembership m WHERE m.user.id = :userId AND " +
           "(m.status = 'ACTIVE' OR m.status = 'GRACE_PERIOD')")
    List<MLSMembership> findActiveByUserId(@Param("userId") UUID userId);

    /**
     * Find memberships by MLS region and status.
     */
    List<MLSMembership> findByMlsRegionAndStatus(MLSRegion region, MembershipStatus status);

    /**
     * Find membership by membership number.
     */
    Optional<MLSMembership> findByMembershipNumber(String membershipNumber);

    /**
     * Find memberships by office.
     */
    List<MLSMembership> findByOffice(Office office);

    /**
     * Find memberships by office and status.
     */
    List<MLSMembership> findByOfficeAndStatus(Office office, MembershipStatus status);

    /**
     * Find active memberships by office.
     */
    @Query("SELECT m FROM MLSMembership m WHERE m.office = :office AND " +
           "(m.status = 'ACTIVE' OR m.status = 'GRACE_PERIOD')")
    List<MLSMembership> findActiveByOffice(@Param("office") Office office);

    /**
     * Find memberships by type.
     */
    List<MLSMembership> findByType(MembershipType type);

    /**
     * Find memberships by type and region.
     */
    List<MLSMembership> findByTypeAndMlsRegion(MembershipType type, MLSRegion region);

    /**
     * Find memberships expiring soon.
     */
    @Query("SELECT m FROM MLSMembership m WHERE m.endDate IS NOT NULL AND " +
           "m.endDate <= :expiryDate AND m.status = 'ACTIVE'")
    List<MLSMembership> findExpiringSoon(@Param("expiryDate") LocalDate expiryDate);

    /**
     * Find expired memberships.
     */
    @Query("SELECT m FROM MLSMembership m WHERE m.endDate IS NOT NULL AND " +
           "m.endDate < :currentDate AND m.status != 'EXPIRED'")
    List<MLSMembership> findExpired(@Param("currentDate") LocalDate currentDate);

    /**
     * Find memberships pending training completion.
     */
    List<MLSMembership> findByTrainingCompletedFalseAndStatus(MembershipStatus status);

    /**
     * Find memberships pending agreement acceptance.
     */
    List<MLSMembership> findByAgreementAcceptedFalseAndStatus(MembershipStatus status);

    /**
     * Check if user has active membership in region.
     */
    @Query("SELECT COUNT(m) > 0 FROM MLSMembership m WHERE m.user = :user AND " +
           "m.mlsRegion = :region AND (m.status = 'ACTIVE' OR m.status = 'GRACE_PERIOD')")
    boolean hasActiveMembershipInRegion(@Param("user") User user, @Param("region") MLSRegion region);

    /**
     * Check if membership number exists.
     */
    boolean existsByMembershipNumber(String membershipNumber);

    /**
     * Check if membership number exists and is different from given ID.
     */
    boolean existsByMembershipNumberAndIdNot(String membershipNumber, UUID id);

    /**
     * Find admins in region.
     */
    @Query("SELECT m FROM MLSMembership m WHERE m.mlsRegion = :region AND " +
           "m.type IN ('SUPER_ADMIN', 'MLS_ADMIN', 'BROKER_ADMIN') AND " +
           "(m.status = 'ACTIVE' OR m.status = 'GRACE_PERIOD')")
    List<MLSMembership> findAdminsInRegion(@Param("region") MLSRegion region);

    /**
     * Count active memberships by region.
     */
    @Query("SELECT COUNT(m) FROM MLSMembership m WHERE m.mlsRegion = :region AND " +
           "(m.status = 'ACTIVE' OR m.status = 'GRACE_PERIOD')")
    long countActiveByRegion(@Param("region") MLSRegion region);

    /**
     * Count active memberships by type and region.
     */
    @Query("SELECT COUNT(m) FROM MLSMembership m WHERE m.mlsRegion = :region AND " +
           "m.type = :type AND (m.status = 'ACTIVE' OR m.status = 'GRACE_PERIOD')")
    long countActiveByTypeAndRegion(@Param("type") MembershipType type, @Param("region") MLSRegion region);

    /**
     * Find memberships with specific permissions.
     */
    @Query("SELECT m FROM MLSMembership m WHERE " +
           "(:canListProperties IS NULL OR m.canListProperties = :canListProperties) AND " +
           "(:canViewAllListings IS NULL OR m.canViewAllListings = :canViewAllListings) AND " +
           "(:canAccessIDXFeeds IS NULL OR m.canAccessIDXFeeds = :canAccessIDXFeeds) AND " +
           "m.status = 'ACTIVE'")
    List<MLSMembership> findByPermissions(@Param("canListProperties") Boolean canListProperties,
                                         @Param("canViewAllListings") Boolean canViewAllListings,
                                         @Param("canAccessIDXFeeds") Boolean canAccessIDXFeeds);

    /**
     * Search memberships by multiple criteria.
     */
    @Query("SELECT m FROM MLSMembership m WHERE " +
           "(:userId IS NULL OR m.user.id = :userId) AND " +
           "(:regionId IS NULL OR m.mlsRegion.id = :regionId) AND " +
           "(:officeId IS NULL OR m.office.id = :officeId) AND " +
           "(:type IS NULL OR m.type = :type) AND " +
           "(:status IS NULL OR m.status = :status)")
    List<MLSMembership> searchMemberships(@Param("userId") UUID userId,
                                         @Param("regionId") UUID regionId,
                                         @Param("officeId") UUID officeId,
                                         @Param("type") MembershipType type,
                                         @Param("status") MembershipStatus status);

    /**
     * Find memberships ordered by creation date.
     */
    List<MLSMembership> findAllByOrderByCreatedAtDesc();

    /**
     * Find active memberships ordered by membership number.
     */
    @Query("SELECT m FROM MLSMembership m WHERE " +
           "(m.status = 'ACTIVE' OR m.status = 'GRACE_PERIOD') " +
           "ORDER BY m.membershipNumber ASC")
    List<MLSMembership> findActiveOrderByMembershipNumber();

    List<MLSMembership> findByMlsRegion_IdAndStatus(UUID regionId, MLSMembership.MembershipStatus status);

    List<MLSMembership> findByMlsRegion_IdAndType(UUID regionId, MLSMembership.MembershipType type);

    @Query("SELECT m FROM MLSMembership m WHERE m.expiryDate <= :expiryDate AND (m.mlsRegion.id = :regionId OR :regionId IS NULL)")
    List<MLSMembership> findExpiringMemberships(@Param("expiryDate") LocalDate expiryDate, @Param("regionId") UUID regionId);

    long countByMlsRegion_Id(UUID regionId);

    long countByMlsRegion_IdAndStatus(UUID regionId, MLSMembership.MembershipStatus status);

    long countByMlsRegion_IdAndType(UUID regionId, MLSMembership.MembershipType type);
} 