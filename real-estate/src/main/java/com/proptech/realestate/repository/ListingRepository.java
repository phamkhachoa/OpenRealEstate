package com.proptech.realestate.repository;

import com.proptech.realestate.model.entity.Listing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
public interface ListingRepository extends JpaRepository<Listing, UUID> {
    
    // Basic search by title and description
    @Query("SELECT l FROM Listing l WHERE " +
           "(:keyword IS NULL OR " +
           "LOWER(l.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(l.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:location IS NULL OR LOWER(l.fullAddress) LIKE LOWER(CONCAT('%', :location, '%'))) AND " +
           "(:listingType IS NULL OR l.listingType = :listingType) AND " +
           "(:status IS NULL OR l.status = :status) AND " +
           "(:categoryId IS NULL OR l.category.id = :categoryId) AND " +
           "(:minPrice IS NULL OR l.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR l.price <= :maxPrice) AND " +
           "(:minArea IS NULL OR l.area >= :minArea) AND " +
           "(:maxArea IS NULL OR l.area <= :maxArea) AND " +
           "(:minBedrooms IS NULL OR l.numBedrooms >= :minBedrooms) AND " +
           "(:maxBedrooms IS NULL OR l.numBedrooms <= :maxBedrooms) AND " +
           "(:minBathrooms IS NULL OR l.numBathrooms >= :minBathrooms) AND " +
           "(:maxBathrooms IS NULL OR l.numBathrooms <= :maxBathrooms)")
    Page<Listing> searchListings(
            @Param("keyword") String keyword,
            @Param("location") String location,
            @Param("listingType") String listingType,
            @Param("status") String status,
            @Param("categoryId") UUID categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("minArea") Double minArea,
            @Param("maxArea") Double maxArea,
            @Param("minBedrooms") Integer minBedrooms,
            @Param("maxBedrooms") Integer maxBedrooms,
            @Param("minBathrooms") Integer minBathrooms,
            @Param("maxBathrooms") Integer maxBathrooms,
            Pageable pageable
    );
    
    // Count total results for search
    @Query("SELECT COUNT(l) FROM Listing l WHERE " +
           "(:keyword IS NULL OR " +
           "LOWER(l.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(l.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:location IS NULL OR LOWER(l.fullAddress) LIKE LOWER(CONCAT('%', :location, '%'))) AND " +
           "(:listingType IS NULL OR l.listingType = :listingType) AND " +
           "(:status IS NULL OR l.status = :status) AND " +
           "(:categoryId IS NULL OR l.category.id = :categoryId) AND " +
           "(:minPrice IS NULL OR l.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR l.price <= :maxPrice) AND " +
           "(:minArea IS NULL OR l.area >= :minArea) AND " +
           "(:maxArea IS NULL OR l.area <= :maxArea) AND " +
           "(:minBedrooms IS NULL OR l.numBedrooms >= :minBedrooms) AND " +
           "(:maxBedrooms IS NULL OR l.numBedrooms <= :maxBedrooms) AND " +
           "(:minBathrooms IS NULL OR l.numBathrooms >= :minBathrooms) AND " +
           "(:maxBathrooms IS NULL OR l.numBathrooms <= :maxBathrooms)")
    long countSearchResults(
            @Param("keyword") String keyword,
            @Param("location") String location,
            @Param("listingType") String listingType,
            @Param("status") String status,
            @Param("categoryId") UUID categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("minArea") Double minArea,
            @Param("maxArea") Double maxArea,
            @Param("minBedrooms") Integer minBedrooms,
            @Param("maxBedrooms") Integer maxBedrooms,
            @Param("minBathrooms") Integer minBathrooms,
            @Param("maxBathrooms") Integer maxBathrooms
    );
    
    // Search by dynamic attributes (for future enhancement)
    @Query("SELECT DISTINCT l FROM Listing l " +
           "LEFT JOIN l.attributes la " +
           "LEFT JOIN la.definition ad " +
           "WHERE ad.name = :attributeName AND la.value = :attributeValue")
    Page<Listing> searchByDynamicAttribute(
            @Param("attributeName") String attributeName,
            @Param("attributeValue") String attributeValue,
            Pageable pageable
    );
} 