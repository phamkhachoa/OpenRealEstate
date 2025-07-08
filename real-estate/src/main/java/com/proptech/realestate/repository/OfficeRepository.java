package com.proptech.realestate.repository;

import com.proptech.realestate.model.entity.Office;
import com.proptech.realestate.model.entity.Office.OfficeType;
import com.proptech.realestate.model.entity.MLSRegion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Office entity.
 * Provides CRUD operations and custom queries for real estate offices.
 */
@Repository
public interface OfficeRepository extends JpaRepository<Office, UUID> {

    /**
     * Find offices by MLS region.
     */
    List<Office> findByMlsRegion(MLSRegion mlsRegion);

    /**
     * Find active offices by MLS region.
     */
    List<Office> findByMlsRegionAndIsActiveTrue(MLSRegion mlsRegion);

    /**
     * Find offices by office type.
     */
    List<Office> findByOfficeType(OfficeType officeType);

    /**
     * Find offices by office type and region.
     */
    List<Office> findByOfficeTypeAndMlsRegion(OfficeType officeType, MLSRegion mlsRegion);

    /**
     * Find office by license number.
     */
    Optional<Office> findByLicenseNumber(String licenseNumber);

    /**
     * Find offices by franchise name.
     */
    List<Office> findByFranchiseName(String franchiseName);

    /**
     * Find offices by franchise name and region.
     */
    List<Office> findByFranchiseNameAndMlsRegion(String franchiseName, MLSRegion mlsRegion);

    /**
     * Find offices by name containing search term (case-insensitive).
     */
    List<Office> findByOfficeNameContainingIgnoreCase(String officeName);

    /**
     * Find offices by email.
     */
    Optional<Office> findByEmail(String email);

    /**
     * Find all active offices.
     */
    List<Office> findByIsActiveTrue();

    /**
     * Find offices within geographic area.
     */
    @Query("SELECT o FROM Office o WHERE " +
           "o.latitude IS NOT NULL AND o.longitude IS NOT NULL AND " +
           "o.latitude BETWEEN :minLat AND :maxLat AND " +
           "o.longitude BETWEEN :minLng AND :maxLng AND " +
           "o.isActive = true")
    List<Office> findWithinGeographicArea(@Param("minLat") Double minLatitude,
                                         @Param("maxLat") Double maxLatitude,
                                         @Param("minLng") Double minLongitude,
                                         @Param("maxLng") Double maxLongitude);

    /**
     * Find offices near a location within radius (simplified distance calculation).
     */
    @Query("SELECT o FROM Office o WHERE " +
           "o.latitude IS NOT NULL AND o.longitude IS NOT NULL AND " +
           "ABS(o.latitude - :latitude) <= :radiusDegrees AND " +
           "ABS(o.longitude - :longitude) <= :radiusDegrees AND " +
           "o.isActive = true")
    List<Office> findNearLocation(@Param("latitude") Double latitude,
                                 @Param("longitude") Double longitude,
                                 @Param("radiusDegrees") Double radiusDegrees);

    /**
     * Check if license number exists.
     */
    boolean existsByLicenseNumber(String licenseNumber);

    /**
     * Check if license number exists and is different from given ID.
     */
    boolean existsByLicenseNumberAndIdNot(String licenseNumber, UUID id);

    /**
     * Check if email exists.
     */
    boolean existsByEmail(String email);

    /**
     * Check if email exists and is different from given ID.
     */
    boolean existsByEmailAndIdNot(String email, UUID id);

    /**
     * Count active offices by region.
     */
    long countByMlsRegionAndIsActiveTrue(MLSRegion mlsRegion);

    /**
     * Count active offices by type and region.
     */
    long countByOfficeTypeAndMlsRegionAndIsActiveTrue(OfficeType officeType, MLSRegion mlsRegion);

    /**
     * Find offices with most agents.
     */
    @Query("SELECT o FROM Office o WHERE o.isActive = true ORDER BY o.totalAgents DESC")
    List<Office> findOfficesOrderedByAgentCount();

    /**
     * Find offices with most agents in region.
     */
    @Query("SELECT o FROM Office o WHERE o.mlsRegion = :region AND o.isActive = true " +
           "ORDER BY o.totalAgents DESC")
    List<Office> findOfficesOrderedByAgentCountInRegion(@Param("region") MLSRegion region);

    /**
     * Search offices by multiple criteria.
     */
    @Query("SELECT o FROM Office o WHERE " +
           "(:officeName IS NULL OR LOWER(o.officeName) LIKE LOWER(CONCAT('%', :officeName, '%'))) AND " +
           "(:city IS NULL OR LOWER(o.city) LIKE LOWER(CONCAT('%', :city, '%'))) AND " +
           "(:state IS NULL OR LOWER(o.state) = LOWER(:state)) AND " +
           "(:officeType IS NULL OR o.officeType = :officeType) AND " +
           "(:regionId IS NULL OR o.mlsRegion.id = :regionId) AND " +
           "(:isActive IS NULL OR o.isActive = :isActive)")
    List<Office> searchOffices(@Param("officeName") String officeName,
                              @Param("city") String city,
                              @Param("state") String state,
                              @Param("officeType") OfficeType officeType,
                              @Param("regionId") UUID regionId,
                              @Param("isActive") Boolean isActive);

    /**
     * Find offices by city and state.
     */
    List<Office> findByCityAndState(String city, String state);

    /**
     * Find active offices by city and state.
     */
    List<Office> findByCityAndStateAndIsActiveTrue(String city, String state);

    /**
     * Find offices ordered by creation date.
     */
    List<Office> findAllByOrderByCreatedAtDesc();

    /**
     * Find active offices ordered by name.
     */
    List<Office> findByIsActiveTrueOrderByOfficeNameAsc();

    /**
     * Find offices ordered by name in region.
     */
    List<Office> findByMlsRegionAndIsActiveTrueOrderByOfficeNameAsc(MLSRegion mlsRegion);
} 