package com.proptech.realestate.repository;

import com.proptech.realestate.model.document.PropertySearchDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.geo.Distance;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Elasticsearch repository for property search
 * Phase 8 Implementation
 */
@Repository
public interface PropertySearchRepository extends ElasticsearchRepository<PropertySearchDocument, String> {

    /**
     * Find properties by city
     */
    List<PropertySearchDocument> findByCity(String city);

    /**
     * Find properties by city with pagination
     */
    Page<PropertySearchDocument> findByCity(String city, Pageable pageable);

    /**
     * Find properties by state
     */
    List<PropertySearchDocument> findByState(String state);

    /**
     * Find properties by zip code
     */
    List<PropertySearchDocument> findByZipCode(String zipCode);

    /**
     * Find properties by property type
     */
    List<PropertySearchDocument> findByPropertyType(String propertyType);

    /**
     * Find properties by status
     */
    List<PropertySearchDocument> findByStandardStatus(String status);

    /**
     * Find active properties
     */
    @Query("{\"term\": {\"standardStatus\": \"Active\"}}")
    Page<PropertySearchDocument> findActiveProperties(Pageable pageable);

    /**
     * Find properties near a location
     */
    List<PropertySearchDocument> findByLocationNear(GeoPoint point, Distance distance);

    /**
     * Find properties within price range
     */
    @Query("{\"range\": {\"listPrice\": {\"gte\": ?0, \"lte\": ?1}}}")
    List<PropertySearchDocument> findByPriceRange(BigDecimal minPrice, BigDecimal maxPrice);

    /**
     * Find properties with minimum bedrooms
     */
    @Query("{\"range\": {\"bedroomsTotal\": {\"gte\": ?0}}}")
    List<PropertySearchDocument> findByMinimumBedrooms(Integer minBedrooms);

    /**
     * Find properties with minimum bathrooms
     */
    @Query("{\"range\": {\"bathroomsTotalInteger\": {\"gte\": ?0}}}")
    List<PropertySearchDocument> findByMinimumBathrooms(Integer minBathrooms);

    /**
     * Find properties by MLS ID
     */
    List<PropertySearchDocument> findByMlsId(String mlsId);

    /**
     * Find properties by listing key
     */
    PropertySearchDocument findByListingKey(String listingKey);

    /**
     * Find properties with photos
     */
    @Query("{\"exists\": {\"field\": \"photos\"}}")
    List<PropertySearchDocument> findPropertiesWithPhotos();

    /**
     * Find properties with virtual tours
     */
    @Query("{\"term\": {\"hasVirtualTour\": true}}")
    List<PropertySearchDocument> findPropertiesWithVirtualTours();

    /**
     * Find recently updated properties
     */
    @Query("{\"range\": {\"modificationTimestamp\": {\"gte\": \"now-7d\"}}}")
    List<PropertySearchDocument> findRecentlyUpdatedProperties();

    /**
     * Find properties by school district
     */
    List<PropertySearchDocument> findBySchoolDistrict(String schoolDistrict);

    /**
     * Find properties by agent
     */
    List<PropertySearchDocument> findByListAgentMlsId(String agentMlsId);

    /**
     * Find properties by office
     */
    List<PropertySearchDocument> findByListOfficeMlsId(String officeMlsId);

    /**
     * Search properties by text in address, city, or remarks
     */
    @Query("{" +
           "\"multi_match\": {" +
           "\"query\": \"?0\"," +
           "\"fields\": [\"address^3\", \"city^2\", \"publicRemarks\", \"neighborhood^1.5\"]," +
           "\"type\": \"best_fields\"," +
           "\"fuzziness\": \"AUTO\"" +
           "}" +
           "}")
    List<PropertySearchDocument> searchByText(String query);

    /**
     * Find properties with specific features
     */
    @Query("{" +
           "\"nested\": {" +
           "\"path\": \"features\"," +
           "\"query\": {" +
           "\"term\": {\"features.name\": \"?0\"}" +
           "}" +
           "}" +
           "}")
    List<PropertySearchDocument> findByFeature(String featureName);

    /**
     * Count properties by city
     */
    @Query("{\"term\": {\"city.keyword\": \"?0\"}}")
    long countByCity(String city);

    /**
     * Count active properties
     */
    @Query("{\"term\": {\"standardStatus\": \"Active\"}}")
    long countActiveProperties();

    /**
     * Find expensive properties (top price range)
     */
    @Query("{" +
           "\"bool\": {" +
           "\"must\": [" +
           "{\"term\": {\"standardStatus\": \"Active\"}}," +
           "{\"range\": {\"listPrice\": {\"gte\": ?0}}}" +
           "]" +
           "}" +
           "}")
    List<PropertySearchDocument> findExpensiveProperties(BigDecimal minPrice);

    /**
     * Find luxury properties (high-end features)
     */
    @Query("{" +
           "\"bool\": {" +
           "\"must\": [" +
           "{\"term\": {\"standardStatus\": \"Active\"}}," +
           "{\"range\": {\"listPrice\": {\"gte\": 1000000}}}," +
           "{\"range\": {\"livingArea\": {\"gte\": 3000}}}" +
           "]" +
           "}" +
           "}")
    List<PropertySearchDocument> findLuxuryProperties();

    /**
     * Find new listings (last 7 days)
     */
    @Query("{" +
           "\"bool\": {" +
           "\"must\": [" +
           "{\"term\": {\"standardStatus\": \"Active\"}}," +
           "{\"range\": {\"listingContractDate\": {\"gte\": \"now-7d\"}}}" +
           "]" +
           "}" +
           "}")
    List<PropertySearchDocument> findNewListings();

    /**
     * Find price reduced properties
     */
    @Query("{" +
           "\"bool\": {" +
           "\"must\": [" +
           "{\"term\": {\"standardStatus\": \"Active\"}}," +
           "{\"script\": {" +
           "\"script\": \"doc['listPrice'].value < doc['originalListPrice'].value\"" +
           "}}" +
           "]" +
           "}" +
           "}")
    List<PropertySearchDocument> findPriceReducedProperties();
}
