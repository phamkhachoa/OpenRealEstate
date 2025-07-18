package com.proptech.realestate.service;

import com.proptech.realestate.model.document.ListingDocument;
import com.proptech.realestate.model.entity.Listing;
import com.proptech.realestate.model.entity.ListingAttribute;
import com.proptech.realestate.repository.ListingDocumentRepository;
import com.proptech.realestate.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchService {

    private final ListingDocumentRepository documentRepository;
    private final ListingRepository listingRepository;

    /**
     * Index a single listing to Elasticsearch
     */
    public void indexListing(Listing listing) {
        try {
            ListingDocument document = convertToDocument(listing);
            documentRepository.save(document);
            log.info("Successfully indexed listing: {}", listing.getId());
        } catch (Exception e) {
            log.error("Failed to index listing: {}", listing.getId(), e);
            throw new RuntimeException("Failed to index listing", e);
        }
    }

    /**
     * Delete listing from Elasticsearch
     */
    public void deleteListing(String listingId) {
        try {
            documentRepository.deleteById(listingId);
            log.info("Successfully deleted listing from index: {}", listingId);
        } catch (Exception e) {
            log.error("Failed to delete listing from index: {}", listingId, e);
        }
    }

    /**
     * Bulk index all active listings
     */
    @Transactional(readOnly = true)
    public void reindexAllListings() {
        log.info("Starting bulk reindex of all listings");
        
        try {
            // Clear existing index
            documentRepository.deleteAll();
            
            // Get all active listings from database
            List<Listing> listings = listingRepository.findAll();
            
            // Convert and index
            List<ListingDocument> documents = listings.stream()
                    .map(this::convertToDocument)
                    .collect(Collectors.toList());
            
            documentRepository.saveAll(documents);
            
            log.info("Successfully reindexed {} listings", documents.size());
        } catch (Exception e) {
            log.error("Failed to reindex listings", e);
            throw new RuntimeException("Failed to reindex listings", e);
        }
    }

    /**
     * Update listing in Elasticsearch
     */
    public void updateListing(Listing listing) {
        indexListing(listing); // Same as index since ES handles updates
    }

    /**
     * Check if listing exists in Elasticsearch
     */
    public boolean existsInIndex(String listingId) {
        return documentRepository.existsById(listingId);
    }

    /**
     * Get listing from Elasticsearch
     */
    public Optional<ListingDocument> getFromIndex(String listingId) {
        return documentRepository.findById(listingId);
    }

    /**
     * Convert JPA Listing entity to Elasticsearch document
     */
    private ListingDocument convertToDocument(Listing listing) {
        ListingDocument.ListingDocumentBuilder builder = ListingDocument.builder()
                .id(listing.getId().toString())
                .listingCode(listing.getListingId())
                .title(listing.getTitle())
                .description(listing.getPublicRemarks())
                .fullAddress(listing.getUnparsedAddress())
                .price(listing.getListPrice())
                .area(listing.getLivingArea())
                .numBedrooms(listing.getBedroomsTotal())
                .numBathrooms(listing.getBathroomsTotalInteger())
                .status(listing.getStandardStatus() != null ? listing.getStandardStatus().name() : null)
                .createdAt(listing.getModificationTimestamp())
                .updatedAt(listing.getModificationTimestamp());

        // Set property type information (simplified from RESO)
        if (listing.getPropertyType() != null) {
            builder.categoryName(listing.getPropertyType());
        }

        // Set user information
        if (listing.getUser() != null) {
            builder.userId(listing.getUser().getId().toString())
                   .userDisplayName(listing.getUser().getFullName());
            // Assume we have userType logic later
        }

        // Handle geolocation
        if (listing.getLatitude() != null && listing.getLongitude() != null) {
            builder.location(new GeoPoint(
                    listing.getLatitude().doubleValue(),
                    listing.getLongitude().doubleValue()));
        }

        // Convert additional details (JSONB) to dynamic attributes
        if (listing.getAmenities() != null && !listing.getAmenities().isEmpty()) {
            List<ListingDocument.DynamicAttribute> dynamicAttributes = listing.getAmenities().entrySet().stream()
                    .map(entry -> ListingDocument.DynamicAttribute.builder()
                            .name(entry.getKey())
                            .value(entry.getValue() != null ? entry.getValue().toString() : null)
                            .type("JSON") // Simplified type
                            .build())
                    .collect(Collectors.toList());
            builder.dynamicAttributes(dynamicAttributes);
        }

        ListingDocument document = builder.build();
        
        // Calculate derived fields
        document.calculatePricePerSqm();
        document.buildSearchText();
        
        return document;
    }

    /**
     * Convert ListingAttribute to DynamicAttribute
     */
    private ListingDocument.DynamicAttribute convertAttribute(ListingAttribute attribute) {
        return ListingDocument.DynamicAttribute.builder()
                .name(attribute.getDefinition().getName())
                .value(attribute.getValue())
                .type(attribute.getDefinition().getDataType().name())
                .build();
    }

    /**
     * Sync specific listing after database change
     */
    public void syncListingAfterChange(String listingId) {
        Optional<Listing> listingOpt = listingRepository.findById(java.util.UUID.fromString(listingId));
        
        if (listingOpt.isPresent()) {
            Listing listing = listingOpt.get();
            
            // Only index active listings
            if (listing.getStandardStatus() == com.proptech.realestate.model.enums.StandardStatus.Active) {
                indexListing(listing);
            } else {
                // Remove from index if not active
                deleteListing(listingId);
            }
        } else {
            // Listing deleted from database, remove from index
            deleteListing(listingId);
        }
    }

    /**
     * Get index statistics
     */
    public long getIndexedListingCount() {
        return documentRepository.count();
    }
} 