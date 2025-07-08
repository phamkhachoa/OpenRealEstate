package com.proptech.realestate.controller;

import com.proptech.realestate.model.dto.SearchListingRequest;
import com.proptech.realestate.model.dto.SearchListingResponse;
import com.proptech.realestate.service.EnhancedSearchService;
import com.proptech.realestate.service.ElasticsearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/enhanced-search")
@RequiredArgsConstructor
public class EnhancedSearchController {

    private final EnhancedSearchService enhancedSearchService;
    private final ElasticsearchService elasticsearchService;

    /**
     * Enhanced search with Elasticsearch full-text capabilities
     */
    @PostMapping("/listings")
    public ResponseEntity<SearchListingResponse> enhancedSearch(@RequestBody SearchListingRequest request) {
        SearchListingResponse response = enhancedSearchService.enhancedSearch(request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET endpoint for simple enhanced search
     */
    @GetMapping("/listings")
    public ResponseEntity<SearchListingResponse> enhancedSearchGet(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String listingType,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String minPrice,
            @RequestParam(required = false) String maxPrice,
            @RequestParam(required = false) String minArea,
            @RequestParam(required = false) String maxArea,
            @RequestParam(required = false) Integer minBedrooms,
            @RequestParam(required = false) Integer maxBedrooms,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        SearchListingRequest request = SearchListingRequest.builder()
                .keyword(keyword)
                .location(location)
                .listingType(listingType)
                .categoryId(categoryId)
                .minPrice(minPrice != null ? new java.math.BigDecimal(minPrice) : null)
                .maxPrice(maxPrice != null ? new java.math.BigDecimal(maxPrice) : null)
                .minArea(minArea != null ? Double.valueOf(minArea) : null)
                .maxArea(maxArea != null ? Double.valueOf(maxArea) : null)
                .minBedrooms(minBedrooms)
                .maxBedrooms(maxBedrooms)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        SearchListingResponse response = enhancedSearchService.enhancedSearch(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Geospatial search - find listings within a radius
     */
    @GetMapping("/listings/nearby")
    public ResponseEntity<SearchListingResponse> searchNearby(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "5.0") Double radiusKm,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {

        SearchListingResponse response = enhancedSearchService.searchNearby(
                latitude, longitude, radiusKm, query, page, size
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Find similar listings
     */
    @GetMapping("/listings/{listingId}/similar")
    public ResponseEntity<SearchListingResponse> findSimilarListings(
            @PathVariable String listingId,
            @RequestParam(defaultValue = "10") Integer size) {

        SearchListingResponse response = enhancedSearchService.findSimilarListings(listingId, size);
        return ResponseEntity.ok(response);
    }

    /**
     * Natural language search with advanced query parsing
     */
    @GetMapping("/natural")
    public ResponseEntity<SearchListingResponse> searchNaturalLanguage(
            @RequestParam String query) {

        SearchListingResponse response = enhancedSearchService.searchWithNaturalLanguage(query);
        return ResponseEntity.ok(response);
    }

    /**
     * Natural language search with POST method
     */
    @PostMapping("/natural")
    public ResponseEntity<SearchListingResponse> searchNaturalLanguagePost(
            @RequestBody Map<String, String> request) {

        String query = request.get("query");
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        SearchListingResponse response = enhancedSearchService.searchWithNaturalLanguage(query);
        return ResponseEntity.ok(response);
    }

    /**
     * Autocomplete suggestions
     */
    @GetMapping("/autocomplete")
    public ResponseEntity<List<String>> getAutocompleteSuggestions(
            @RequestParam String query) {

        List<String> suggestions = enhancedSearchService.getAutocompleteSuggestions(query);
        return ResponseEntity.ok(suggestions);
    }

    /**
     * Get search facets/aggregations
     */
    @PostMapping("/facets")
    public ResponseEntity<SearchListingResponse.SearchMetadata> getFacets(
            @RequestBody SearchListingRequest request) {

        SearchListingResponse.SearchMetadata metadata = enhancedSearchService.getFacets(request);
        return ResponseEntity.ok(metadata);
    }

    /**
     * Admin endpoints for index management
     */
    @PostMapping("/admin/reindex")
    public ResponseEntity<Map<String, Object>> reindexAllListings() {
        try {
            elasticsearchService.reindexAllListings();
            long count = elasticsearchService.getIndexedListingCount();
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Reindexing completed successfully",
                    "indexedCount", count
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Reindexing failed: " + e.getMessage()
            ));
        }
    }

    /**
     * Get index statistics
     */
    @GetMapping("/admin/stats")
    public ResponseEntity<Map<String, Object>> getIndexStats() {
        long indexedCount = elasticsearchService.getIndexedListingCount();
        
        return ResponseEntity.ok(Map.of(
                "indexedListings", indexedCount,
                "indexName", "real_estate_listings"
        ));
    }

    /**
     * Health check for Elasticsearch
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            long count = elasticsearchService.getIndexedListingCount();
            return ResponseEntity.ok(Map.of(
                    "status", "healthy",
                    "indexedListings", count
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "status", "unhealthy",
                    "error", e.getMessage()
            ));
        }
    }
} 