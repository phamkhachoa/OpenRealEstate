package com.proptech.realestate.service.search;

import com.proptech.realestate.model.document.PropertySearchDocument;
import com.proptech.realestate.model.entity.Listing;
import com.proptech.realestate.repository.PropertySearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing property search index in Elasticsearch
 * Handles indexing, updating, and removing properties from search index
 * Phase 7 Implementation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PropertySearchIndexService {

    private final PropertySearchRepository searchRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    private static final String PROPERTY_INDEX_NAME = "properties";

    /**
     * Index a single property
     */
    public void indexProperty(Listing property) {
        try {
            PropertySearchDocument document = convertToSearchDocument(property);
            searchRepository.save(document);
            log.debug("Indexed property: {}", property.getListingId());
        } catch (Exception e) {
            log.error("Failed to index property: {}", property.getListingId(), e);
            throw new SearchIndexException("Failed to index property: " + property.getListingId(), e);
        }
    }

    /**
     * Index multiple properties (batch operation)
     */
    public void indexProperties(List<Listing> properties) {
        if (properties == null || properties.isEmpty()) {
            log.debug("No properties to index");
            return;
        }

        try {
            List<PropertySearchDocument> documents = properties.stream()
                    .map(this::convertToSearchDocument)
                    .collect(Collectors.toList());

            searchRepository.saveAll(documents);
            log.info("Successfully indexed {} properties", documents.size());
        } catch (Exception e) {
            log.error("Failed to index properties batch of size: {}", properties.size(), e);
            throw new SearchIndexException("Failed to index properties batch", e);
        }
    }

    /**
     * Update property in search index
     */
    public void updateProperty(Listing property) {
        try {
            // Check if property exists in index
            if (searchRepository.existsById(property.getId().toString())) {
                PropertySearchDocument document = convertToSearchDocument(property);
                searchRepository.save(document);
                log.debug("Updated property in index: {}", property.getListingId());
            } else {
                // Property doesn't exist, index it
                indexProperty(property);
            }
        } catch (Exception e) {
            log.error("Failed to update property in index: {}", property.getListingId(), e);
            throw new SearchIndexException("Failed to update property in index: " + property.getListingId(), e);
        }
    }

    /**
     * Remove property from search index
     */
    public void removeProperty(String propertyId) {
        try {
            searchRepository.deleteById(propertyId);
            log.debug("Removed property from index: {}", propertyId);
        } catch (Exception e) {
            log.error("Failed to remove property from index: {}", propertyId, e);
            throw new SearchIndexException("Failed to remove property from index: " + propertyId, e);
        }
    }

    /**
     * Remove multiple properties from search index
     */
    public void removeProperties(List<String> propertyIds) {
        try {
            searchRepository.deleteAllById(propertyIds);
            log.info("Removed {} properties from index", propertyIds.size());
        } catch (Exception e) {
            log.error("Failed to remove properties from index", e);
            throw new SearchIndexException("Failed to remove properties from index", e);
        }
    }

    /**
     * Reindex all properties
     */
    public void reindexAll(List<Listing> allProperties) {
        try {
            log.info("Starting full reindex of {} properties", allProperties.size());
            
            // Clear existing index
            searchRepository.deleteAll();
            
            // Index all properties
            indexProperties(allProperties);
            
            log.info("Completed full reindex of {} properties", allProperties.size());
        } catch (Exception e) {
            log.error("Failed to reindex all properties", e);
            throw new SearchIndexException("Failed to reindex all properties", e);
        }
    }

    /**
     * Check if property exists in index
     */
    public boolean existsInIndex(String propertyId) {
        return searchRepository.existsById(propertyId);
    }

    /**
     * Get total count of indexed properties
     */
    public long getIndexedCount() {
        return searchRepository.count();
    }

    /**
     * Find properties in index by MLS ID
     */
    public List<PropertySearchDocument> findByMlsId(String mlsId) {
        return searchRepository.findByMlsId(mlsId);
    }

    /**
     * Create or update search index
     */
    public void createIndex() {
        try {
            IndexOperations indexOps = elasticsearchOperations.indexOps(PropertySearchDocument.class);
            
            if (!indexOps.exists()) {
                indexOps.create();
                indexOps.putMapping();
                log.info("Created search index for properties");
            } else {
                log.info("Search index for properties already exists");
            }
        } catch (Exception e) {
            log.error("Failed to create search index", e);
            throw new SearchIndexException("Failed to create search index", e);
        }
    }

    /**
     * Delete search index
     */
    public void deleteIndex() {
        try {
            IndexOperations indexOps = elasticsearchOperations.indexOps(PropertySearchDocument.class);
            
            if (indexOps.exists()) {
                indexOps.delete();
                log.info("Deleted search index for properties");
            }
        } catch (Exception e) {
            log.error("Failed to delete search index", e);
            throw new SearchIndexException("Failed to delete search index", e);
        }
    }

    /**
     * Check index health
     */
    public boolean isIndexHealthy() {
        try {
            IndexOperations indexOps = elasticsearchOperations.indexOps(PropertySearchDocument.class);
            return indexOps.exists();
        } catch (Exception e) {
            log.error("Failed to check index health", e);
            return false;
        }
    }

    /**
     * Get index statistics
     */
    public IndexStatistics getIndexStatistics() {
        try {
            long totalDocuments = searchRepository.count();
            boolean healthy = isIndexHealthy();
            
            return IndexStatistics.builder()
                    .indexName(PROPERTY_INDEX_NAME)
                    .totalDocuments(totalDocuments)
                    .healthy(healthy)
                    .lastUpdated(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            log.error("Failed to get index statistics", e);
            return IndexStatistics.builder()
                    .indexName(PROPERTY_INDEX_NAME)
                    .totalDocuments(0L)
                    .healthy(false)
                    .lastUpdated(LocalDateTime.now())
                    .error(e.getMessage())
                    .build();
        }
    }

    /**
     * Convert Listing entity to PropertySearchDocument
     */
    private PropertySearchDocument convertToSearchDocument(Listing property) {
        PropertySearchDocument.PropertySearchDocumentBuilder builder = PropertySearchDocument.builder()
                .id(property.getId().toString())
                .listingId(property.getListingId())
                .mlsId(property.getMlsId())
                .title(property.getTitle())
                .description(property.getDescription())
                .listPrice(property.getListPrice())
                .originalListPrice(property.getOriginalListPrice())
                .propertyType(property.getPropertyType())
                .propertySubType(property.getPropertySubType())
                .bedroomsTotal(property.getBedroomsTotal())
                .bathroomsTotalInteger(property.getBathroomsTotalInteger())
                .livingArea(property.getLivingArea())
                .yearBuilt(property.getYearBuilt())
                .standardStatus(property.getStandardStatus() != null ? property.getStandardStatus().name() : null)
                .mlsStatus(property.getMlsStatus())
                .unparsedAddress(property.getUnparsedAddress())
                .city(property.getCity())
                .stateOrProvince(property.getStateOrProvince())
                .postalCode(property.getPostalCode())
                .country(property.getCountry())
                .listingContractDate(property.getListingContractDate())
                .onMarketDate(property.getOnMarketDate())
                .modificationTimestamp(property.getModificationTimestamp())
                .daysOnMarket(property.getDaysOnMarket())
                .publicRemarks(property.getPublicRemarks())
                .privateRemarks(property.getPrivateRemarks())
                .lotSizeAcres(property.getLotSizeAcres())
                .lotSizeSquareFeet(property.getLotSizeSquareFeet())
                .createdAt(property.getCreatedAt())
                .updatedAt(property.getUpdatedAt());

        // Set geographic location
        if (property.getLatitude() != null && property.getLongitude() != null) {
            builder.location(new GeoPoint(property.getLatitude(), property.getLongitude()));
        }

        // Set listing agent information
        if (property.getListingAgent() != null) {
            builder.listAgentFullName(property.getListingAgent().getFirstName() + " " + 
                                    property.getListingAgent().getLastName())
                   .listAgentEmail(property.getListingAgent().getEmail())
                   .listAgentPhone(property.getListingAgent().getPhone());
        }

        // Set listing office information
        if (property.getListingOffice() != null) {
            builder.listOfficeName(property.getListingOffice().getOfficeName())
                   .listOfficePhone(property.getListingOffice().getPhone());
        }

        return builder.build();
    }

    /**
     * Index statistics DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class IndexStatistics {
        private String indexName;
        private long totalDocuments;
        private boolean healthy;
        private LocalDateTime lastUpdated;
        private String error;
    }

    /**
     * Custom exception for search index operations
     */
    public static class SearchIndexException extends RuntimeException {
        public SearchIndexException(String message) {
            super(message);
        }

        public SearchIndexException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
