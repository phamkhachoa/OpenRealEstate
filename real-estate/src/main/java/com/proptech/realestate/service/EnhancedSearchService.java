package com.proptech.realestate.service;

import com.proptech.realestate.model.document.ListingDocument;
import com.proptech.realestate.model.dto.SearchListingRequest;
import com.proptech.realestate.model.dto.SearchListingResponse;
import com.proptech.realestate.repository.ListingDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnhancedSearchService {

    private final ListingDocumentRepository documentRepository;
    private final SearchService searchService; // Fallback to database search
    private final QueryParsingService queryParsingService;

    /**
     * Enhanced search using Elasticsearch with full-text capabilities
     */
    public SearchListingResponse enhancedSearch(SearchListingRequest request) {
        try {
            log.info("Performing enhanced search with Elasticsearch");
            
            // If keyword exists, normalize it first
            if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
                String normalizedKeyword = queryParsingService.normalizeInput(request.getKeyword());
                request.setKeyword(normalizedKeyword);
                log.info("Normalized keyword: '{}' -> '{}'", request.getKeyword(), normalizedKeyword);
            }
            
            Pageable pageable = createPageable(request);
            Page<ListingDocument> documents;

            // Check if it's a geospatial search
            if (request.getLatitude() != null && request.getLongitude() != null && request.getRadius() != null) {
                documents = performGeospatialSearch(request, pageable);
            }
            // Check if it's advanced search with multiple criteria
            else if (hasMultipleCriteria(request)) {
                documents = performAdvancedSearch(request, pageable);
            }
            // Simple full-text search
            else if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
                documents = documentRepository.searchWithFullText(request.getKeyword(), pageable);
            }
            // Basic filter search
            else {
                documents = performBasicSearch(request, pageable);
            }

            return convertToResponse(documents);
            
        } catch (Exception e) {
            log.error("Elasticsearch search failed, falling back to database search", e);
            // Fallback to regular database search
            return searchService.search(request);
        }
    }
    
    /**
     * Natural language search with advanced query parsing
     */
    public SearchListingResponse searchWithNaturalLanguage(String naturalQuery) {
        try {
            log.info("Processing natural language query: '{}'", naturalQuery);
            
            // Step 1: Parse natural language query with advanced normalization
            QueryParsingService.ParsedQuery parsed = queryParsingService.parseNaturalLanguage(naturalQuery);
            log.info("Parsed query: {}", parsed);
            
            // Step 2: Convert to structured search request
            SearchListingRequest searchRequest = new SearchListingRequest();
            searchRequest.setKeyword(parsed.getKeyword());
            searchRequest.setMinBedrooms(parsed.getMinBedrooms());
            searchRequest.setMaxBedrooms(parsed.getMaxBedrooms());
            searchRequest.setMinBathrooms(parsed.getMinBathrooms());
            searchRequest.setMaxBathrooms(parsed.getMaxBathrooms());
            searchRequest.setMinPrice(parsed.getMinPrice());
            searchRequest.setMaxPrice(parsed.getMaxPrice());
            searchRequest.setMinArea(parsed.getMinArea());
            searchRequest.setMaxArea(parsed.getMaxArea());
            searchRequest.setPropertyType(parsed.getPropertyType());
            
            // Set location if available
            if (parsed.getLocationInfo() != null) {
                searchRequest.setLatitude(parsed.getLocationInfo().getLatitude());
                searchRequest.setLongitude(parsed.getLocationInfo().getLongitude());
                searchRequest.setRadius(parsed.getLocationInfo().getRadiusKm());
            }
            
            // Step 3: Execute enhanced search
            return enhancedSearch(searchRequest);
            
        } catch (Exception e) {
            log.error("Error in natural language search: {}", e.getMessage());
            return createEmptyResponse();
        }
    }

    /**
     * Autocomplete suggestions for search input
     */
    public List<String> getAutocompleteSuggestions(String input) {
        try {
            if (input == null || input.trim().length() < 2) {
                return List.of();
            }

            List<ListingDocument> documents = documentRepository.autocomplete(input.trim());
            
            return documents.stream()
                    .flatMap(doc -> List.of(
                            doc.getTitle(),
                            doc.getFullAddress(),
                            doc.getCity(),
                            doc.getDistrict()
                    ).stream())
                    .filter(text -> text != null && text.toLowerCase().contains(input.toLowerCase()))
                    .distinct()
                    .limit(10)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("Autocomplete search failed", e);
            return List.of();
        }
    }

    /**
     * Find similar listings based on a given listing
     */
    public SearchListingResponse findSimilarListings(String listingId, int size) {
        try {
            // Get the original listing first
            var originalOpt = documentRepository.findById(listingId);
            if (originalOpt.isEmpty()) {
                return createEmptyResponse();
            }

            ListingDocument original = originalOpt.get();
            
            // Calculate price range (±20%)
            BigDecimal priceRange = original.getPrice().multiply(BigDecimal.valueOf(0.2));
            BigDecimal minPrice = original.getPrice().subtract(priceRange);
            BigDecimal maxPrice = original.getPrice().add(priceRange);
            
            // Calculate area range (±30%)
            Double areaRange = original.getArea() * 0.3;
            Double minArea = original.getArea() - areaRange;
            Double maxArea = original.getArea() + areaRange;

            Pageable pageable = PageRequest.of(0, size);
            
            Page<ListingDocument> similarDocs = documentRepository.findSimilarListings(
                    listingId, minPrice, maxPrice, minArea, maxArea, 
                    original.getCategoryId(), pageable
            );

            return convertToResponse(similarDocs);
            
        } catch (Exception e) {
            log.error("Failed to find similar listings for: {}", listingId, e);
            return createEmptyResponse();
        }
    }

    /**
     * Search within a geographic area
     */
    public SearchListingResponse searchNearby(Double latitude, Double longitude, Double radiusKm, String query, int page, int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("_score").descending());
            
            Page<ListingDocument> documents = documentRepository.findWithinDistance(
                    query != null ? query : "*", radiusKm, latitude, longitude, pageable
            );

            return convertToResponse(documents);
            
        } catch (Exception e) {
            log.error("Geospatial search failed", e);
            return createEmptyResponse();
        }
    }

    /**
     * Get faceted search results (aggregations)
     */
    public SearchListingResponse.SearchMetadata getFacets(SearchListingRequest request) {
        // This would require custom Elasticsearch aggregation queries
        // For now, return basic metadata
        return SearchListingResponse.SearchMetadata.builder()
                .totalCount(0L)
                .pageSize(request.getSize() != null ? request.getSize() : 20)
                .currentPage(request.getPage() != null ? request.getPage() : 0)
                .totalPages(0)
                .build();
    }

    // Helper methods

    private Pageable createPageable(SearchListingRequest request) {
        int page = request.getPage() != null ? request.getPage() : 0;
        int size = request.getSize() != null ? request.getSize() : 20;
        
        // Default sort by relevance (_score), then by created date
        Sort sort = Sort.by("_score").descending()
                .and(Sort.by("createdAt").descending());
                
        if (request.getSortBy() != null) {
            Sort.Direction direction = "desc".equalsIgnoreCase(request.getSortDirection()) 
                    ? Sort.Direction.DESC : Sort.Direction.ASC;
            sort = Sort.by(direction, request.getSortBy());
        }
        
        return PageRequest.of(page, size, sort);
    }

    private Page<ListingDocument> performGeospatialSearch(SearchListingRequest request, Pageable pageable) {
        String query = request.getKeyword() != null ? request.getKeyword() : "*";
        return documentRepository.findWithinDistance(
                query, request.getRadius(), request.getLatitude(), request.getLongitude(), pageable
        );
    }

    private Page<ListingDocument> performAdvancedSearch(SearchListingRequest request, Pageable pageable) {
        String query = request.getKeyword() != null ? request.getKeyword() : "*";
        
        BigDecimal minPrice = request.getMinPrice() != null ? request.getMinPrice() : BigDecimal.ZERO;
        BigDecimal maxPrice = request.getMaxPrice() != null ? request.getMaxPrice() : BigDecimal.valueOf(Long.MAX_VALUE);
        
        Double minArea = request.getMinArea() != null ? request.getMinArea() : 0.0;
        Double maxArea = request.getMaxArea() != null ? request.getMaxArea() : Double.MAX_VALUE;
        
        return documentRepository.advancedSearch(query, minPrice, maxPrice, minArea, maxArea, pageable);
    }

    private Page<ListingDocument> performBasicSearch(SearchListingRequest request, Pageable pageable) {
        // Implement basic filtering based on simple criteria
        if (request.getListingType() != null) {
            return documentRepository.findByListingType(request.getListingType(), pageable);
        }
        if (request.getCategoryId() != null) {
            return documentRepository.findByCategoryId(request.getCategoryId(), pageable);
        }
        if (request.getLocation() != null) {
            return documentRepository.findByCityIgnoreCase(request.getLocation(), pageable);
        }
        
        // Default: return all active listings
        return documentRepository.findByStatus("ACTIVE", pageable);
    }

    private boolean hasMultipleCriteria(SearchListingRequest request) {
        int criteriaCount = 0;
        if (request.getMinPrice() != null || request.getMaxPrice() != null) criteriaCount++;
        if (request.getMinArea() != null || request.getMaxArea() != null) criteriaCount++;
        if (request.getMinBedrooms() != null) criteriaCount++;
        if (request.getListingType() != null) criteriaCount++;
        if (request.getCategoryId() != null) criteriaCount++;
        
        return criteriaCount >= 2;
    }

    private SearchListingResponse convertToResponse(Page<ListingDocument> documents) {
        List<SearchListingResponse.ListingSearchResult> results = documents.getContent().stream()
                .map(this::convertDocumentToResult)
                .collect(Collectors.toList());

        SearchListingResponse.SearchMetadata metadata = SearchListingResponse.SearchMetadata.builder()
                .totalCount(documents.getTotalElements())
                .pageSize(documents.getSize())
                .currentPage(documents.getNumber())
                .totalPages(documents.getTotalPages())
                .build();

        return SearchListingResponse.builder()
                .listings(results)
                .metadata(metadata)
                .build();
    }

    private SearchListingResponse.ListingSearchResult convertDocumentToResult(ListingDocument doc) {
        return SearchListingResponse.ListingSearchResult.builder()
                .id(doc.getId())
                .listingCode(doc.getListingCode())
                .title(doc.getTitle())
                .description(doc.getDescription())
                .price(doc.getPrice() != null ? doc.getPrice().toString() : null)
                .area(doc.getArea() != null ? doc.getArea().toString() : null)
                .numBedrooms(doc.getNumBedrooms())
                .numBathrooms(doc.getNumBathrooms())
                .address(doc.getFullAddress())
                .listingType(doc.getListingType())
                .status(doc.getStatus())
                .createdAt(doc.getCreatedAt())
                .categoryName(doc.getCategoryName())
                .pricePerSqm(doc.getPricePerSqm())
                .build();
    }

    private SearchListingResponse createEmptyResponse() {
        return SearchListingResponse.builder()
                .listings(List.of())
                .metadata(SearchListingResponse.SearchMetadata.builder()
                        .totalCount(0L)
                        .pageSize(20)
                        .currentPage(0)
                        .totalPages(0)
                        .build())
                .build();
    }
} 