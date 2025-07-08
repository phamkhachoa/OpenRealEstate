package com.proptech.realestate.repository;

import com.proptech.realestate.model.entity.MLSRegion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for MLSRegion entity.
 * Provides CRUD operations and custom queries for MLS regions.
 */
@Repository
public interface MLSRegionRepository extends JpaRepository<MLSRegion, UUID> {

    /**
     * Find MLS region by region code.
     */
    Optional<MLSRegion> findByRegionCode(String regionCode);

    /**
     * Find all active MLS regions.
     */
    List<MLSRegion> findByIsActiveTrue();

    /**
     * Find MLS regions by name containing the search term (case-insensitive).
     */
    List<MLSRegion> findByNameContainingIgnoreCase(String name);

    /**
     * Find MLS region that contains the given location coordinates.
     */
    @Query("SELECT r FROM MLSRegion r WHERE " +
           "(:latitude IS NULL OR r.northBoundary IS NULL OR :latitude <= r.northBoundary) AND " +
           "(:latitude IS NULL OR r.southBoundary IS NULL OR :latitude >= r.southBoundary) AND " +
           "(:longitude IS NULL OR r.eastBoundary IS NULL OR :longitude <= r.eastBoundary) AND " +
           "(:longitude IS NULL OR r.westBoundary IS NULL OR :longitude >= r.westBoundary) AND " +
           "r.isActive = true")
    List<MLSRegion> findByLocationWithinBoundaries(@Param("latitude") Double latitude, 
                                                   @Param("longitude") Double longitude);

    /**
     * Find the first active MLS region that contains the given location.
     */
    @Query("SELECT r FROM MLSRegion r WHERE " +
           "(:latitude IS NULL OR r.northBoundary IS NULL OR :latitude <= r.northBoundary) AND " +
           "(:latitude IS NULL OR r.southBoundary IS NULL OR :latitude >= r.southBoundary) AND " +
           "(:longitude IS NULL OR r.eastBoundary IS NULL OR :longitude <= r.eastBoundary) AND " +
           "(:longitude IS NULL OR r.westBoundary IS NULL OR :longitude >= r.westBoundary) AND " +
           "r.isActive = true " +
           "ORDER BY r.createdAt ASC")
    Optional<MLSRegion> findFirstByLocationWithinBoundaries(@Param("latitude") Double latitude, 
                                                           @Param("longitude") Double longitude);

    /**
     * Check if a region code already exists.
     */
    boolean existsByRegionCode(String regionCode);

    /**
     * Check if a region code exists and is different from the given ID (for updates).
     */
    boolean existsByRegionCodeAndIdNot(String regionCode, UUID id);

    /**
     * Find all regions with listing prefix.
     */
    List<MLSRegion> findByListingPrefixIsNotNull();

    /**
     * Find regions that require approval for listings.
     */
    List<MLSRegion> findByRequiresApprovalTrue();

    /**
     * Count total active regions.
     */
    long countByIsActiveTrue();

    /**
     * Find regions by admin email.
     */
    Optional<MLSRegion> findByAdminEmail(String adminEmail);

    /**
     * Search regions by multiple criteria.
     */
    @Query("SELECT r FROM MLSRegion r WHERE " +
           "(:name IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:regionCode IS NULL OR LOWER(r.regionCode) LIKE LOWER(CONCAT('%', :regionCode, '%'))) AND " +
           "(:isActive IS NULL OR r.isActive = :isActive)")
    List<MLSRegion> searchRegions(@Param("name") String name,
                                 @Param("regionCode") String regionCode,
                                 @Param("isActive") Boolean isActive);

    /**
     * Find regions ordered by creation date.
     */
    List<MLSRegion> findAllByOrderByCreatedAtDesc();

    /**
     * Find active regions ordered by name.
     */
    List<MLSRegion> findByIsActiveTrueOrderByNameAsc();
} 