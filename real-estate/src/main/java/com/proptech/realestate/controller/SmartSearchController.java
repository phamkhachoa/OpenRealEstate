package com.proptech.realestate.controller;

import com.proptech.realestate.model.dto.NLPQueryResult;
import com.proptech.realestate.model.dto.SearchListingRequest;
import com.proptech.realestate.model.dto.SearchListingResponse;
import com.proptech.realestate.service.ListingService;
import com.proptech.realestate.service.SmartElasticsearchService;
import com.proptech.realestate.service.SmartSearchQueryBuilder;
import com.proptech.realestate.service.VietnameseNLPService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Smart Search Controller with Vietnamese NLP + Elasticsearch Integration
 * Provides natural language search capabilities for real estate listings
 */
@RestController
@RequestMapping("/api/smart-search")
@RequiredArgsConstructor
@Slf4j
public class SmartSearchController {

    private final VietnameseNLPService nlpService;
    private final SmartElasticsearchService smartElasticsearchService;
    private final SmartSearchQueryBuilder queryBuilder;
    private final ListingService listingService; // Fallback

    /**
     * Natural language search endpoint with Elasticsearch integration
     * Accepts Vietnamese natural language queries and returns relevant listings
     */
    @PostMapping("/natural")
    public SmartSearchResponse naturalLanguageSearch(@RequestBody NaturalLanguageSearchRequest request) {
        
        log.info("Received natural language search query: {}", request.getQuery());
        
        try {
            // Check if NLP service is available
            if (!nlpService.isAvailable()) {
                log.error("NLP service is not available");
                return SmartSearchResponse.error("NLP service is currently unavailable");
            }
            
            // Process natural language query
            Optional<NLPQueryResult> nlpResultOpt = nlpService.processQuery(request.getQuery());
            
            if (nlpResultOpt.isEmpty()) {
                log.warn("Failed to process NLP query: {}", request.getQuery());
                return SmartSearchResponse.error("Unable to understand the query. Please try rephrasing.");
            }
            
            NLPQueryResult nlpResult = nlpResultOpt.get();
            
            // Check confidence score
            if (nlpResult.getConfidenceScore() < 0.3) {
                log.warn("Low confidence score for query: {} (score: {})", 
                    request.getQuery(), nlpResult.getConfidenceScore());
                return SmartSearchResponse.error("Query is too ambiguous. Please provide more specific details.");
            }
            
            // Execute smart Elasticsearch search
            int page = request.getPage() != null ? request.getPage() : 0;
            int size = request.getSize() != null ? request.getSize() : 20;
            
            SearchListingResponse searchResponse = smartElasticsearchService.smartSearch(
                request.getQuery(), page, size);
            
            // Build search request for reference (optional)
            SearchListingRequest searchRequest = queryBuilder.buildFromNLP(nlpResult);
            searchRequest.setPage(page);
            searchRequest.setSize(size);
            
            // Build smart search response
            SmartSearchResponse response = SmartSearchResponse.builder()
                .originalQuery(request.getQuery())
                .normalizedQuery(nlpResult.getNormalizedQuery())
                .extractedEntities(nlpResult.getExtractedEntities())
                .confidenceScore(nlpResult.getConfidenceScore())
                .searchRequest(searchRequest)
                .searchResults(searchResponse)
                .suggestions(generateSearchSuggestions(nlpResult, searchResponse))
                .searchEngine("ELASTICSEARCH_WITH_NLP")
                .success(true)
                .build();
            
            log.info("Smart Elasticsearch search completed: {} results found with confidence {}",
                searchResponse.getTotalElements(), nlpResult.getConfidenceScore());
                
            return response;
            
        } catch (Exception e) {
            log.error("Error processing natural language search", e);
            return SmartSearchResponse.error("An error occurred while processing your search");
        }
    }

    /**
     * Fallback natural language search using database
     * Used when Elasticsearch is unavailable
     */
    @PostMapping("/natural-fallback")
    public SmartSearchResponse naturalLanguageSearchFallback(@RequestBody NaturalLanguageSearchRequest request) {
        
        log.info("Received fallback natural language search query: {}", request.getQuery());
        
        try {
            // Check if NLP service is available
            if (!nlpService.isAvailable()) {
                log.error("NLP service is not available");
                return SmartSearchResponse.error("NLP service is currently unavailable");
            }
            
            // Process natural language query
            Optional<NLPQueryResult> nlpResultOpt = nlpService.processQuery(request.getQuery());
            
            if (nlpResultOpt.isEmpty()) {
                log.warn("Failed to process NLP query: {}", request.getQuery());
                return SmartSearchResponse.error("Unable to understand the query. Please try rephrasing.");
            }
            
            NLPQueryResult nlpResult = nlpResultOpt.get();
            
            // Check confidence score
            if (nlpResult.getConfidenceScore() < 0.3) {
                log.warn("Low confidence score for query: {} (score: {})", 
                    request.getQuery(), nlpResult.getConfidenceScore());
                return SmartSearchResponse.error("Query is too ambiguous. Please provide more specific details.");
            }
            
            // Build structured search query
            SearchListingRequest searchRequest = queryBuilder.buildFromNLP(nlpResult);
            searchRequest = queryBuilder.validateAndOptimize(searchRequest);
            
            // Set pagination if provided
            if (request.getPage() != null) {
                searchRequest.setPage(request.getPage());
            }
            if (request.getSize() != null) {
                searchRequest.setSize(request.getSize());
            }
            
            // Execute database search
            SearchListingResponse searchResponse = listingService.searchListings(searchRequest);
            
            // Build smart search response
            SmartSearchResponse response = SmartSearchResponse.builder()
                .originalQuery(request.getQuery())
                .normalizedQuery(nlpResult.getNormalizedQuery())
                .extractedEntities(nlpResult.getExtractedEntities())
                .confidenceScore(nlpResult.getConfidenceScore())
                .searchRequest(searchRequest)
                .searchResults(searchResponse)
                .suggestions(generateSearchSuggestions(nlpResult, searchResponse))
                .searchEngine("DATABASE_WITH_NLP")
                .success(true)
                .build();
            
            log.info("Fallback natural language search completed: {} results found with confidence {}",
                searchResponse.getTotalElements(), nlpResult.getConfidenceScore());
                
            return response;
            
        } catch (Exception e) {
            log.error("Error processing fallback natural language search", e);
            return SmartSearchResponse.error("An error occurred while processing your search");
        }
    }

    /**
     * Get search suggestions based on partial query
     */
    @GetMapping("/suggestions")
    public Map<String, Object> getSearchSuggestions(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int limit) {
        
        try {
            Map<String, Object> suggestions = new HashMap<>();
            
            // Extract partial entities from query
            Optional<NLPQueryResult> nlpResult = nlpService.processQuery(query);
            
            if (nlpResult.isPresent()) {
                NLPQueryResult.ExtractedEntities entities = nlpResult.get().getExtractedEntities();
                
                // Property type suggestions
                if (entities.getPropertyType() == null) {
                    suggestions.put("property_types", getPropertyTypeSuggestions(query));
                }
                
                // Location suggestions
                if (entities.getLocation() == null) {
                    suggestions.put("locations", getLocationSuggestions(query));
                }
                
                // Price range suggestions
                if (entities.getPriceRange() == null) {
                    suggestions.put("price_ranges", getPriceRangeSuggestions());
                }
                
                // Amenity suggestions
                suggestions.put("amenities", getAmenitySuggestions(query));
            }
            
            return suggestions;
            
        } catch (Exception e) {
            log.error("Error generating search suggestions", e);
            return Map.of("error", "Unable to generate suggestions");
        }
    }

    /**
     * Analyze query without performing search
     */
    @PostMapping("/analyze")
    public NLPQueryResult analyzeQuery(@RequestBody AnalyzeQueryRequest request) {
        try {
            Optional<NLPQueryResult> result = nlpService.processQuery(request.getQuery());
            return result.orElse(null);
            
        } catch (Exception e) {
            log.error("Error analyzing query", e);
            return null;
        }
    }

    /**
     * Health check for NLP and Elasticsearch services
     */
    @GetMapping("/health")
    public Map<String, Object> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        
        boolean nlpAvailable = nlpService.isAvailable();
        health.put("nlp_service", nlpAvailable ? "UP" : "DOWN");
        
        // Test Elasticsearch connectivity
        boolean elasticsearchAvailable = testElasticsearchHealth();
        health.put("elasticsearch", elasticsearchAvailable ? "UP" : "DOWN");
        
        boolean overallHealthy = nlpAvailable && elasticsearchAvailable;
        health.put("status", overallHealthy ? "HEALTHY" : "DEGRADED");
        
        if (nlpAvailable) {
            // Test with a simple query
            double testConfidence = nlpService.getConfidenceScore("căn hộ 2 phòng ngủ");
            health.put("test_confidence", testConfidence);
        }
        
        return health;
    }

    /**
     * Test Elasticsearch connectivity
     */
    private boolean testElasticsearchHealth() {
        try {
            // Try a simple search to test Elasticsearch
            smartElasticsearchService.smartSearch("test", 0, 1);
            return true;
        } catch (Exception e) {
            log.warn("Elasticsearch health check failed", e);
            return false;
        }
    }

    /**
     * Generate search suggestions based on NLP results and search response
     */
    private Map<String, Object> generateSearchSuggestions(NLPQueryResult nlpResult, 
                                                         SearchListingResponse searchResponse) {
        Map<String, Object> suggestions = new HashMap<>();
        
        // If no results found, suggest related searches
        if (searchResponse.getTotalElements() == 0) {
            suggestions.put("no_results_suggestions", generateNoResultsSuggestions(nlpResult));
        }
        
        // If too many results, suggest refinements
        if (searchResponse.getTotalElements() > 100) {
            suggestions.put("refinement_suggestions", generateRefinementSuggestions(nlpResult));
        }
        
        // Suggest similar price ranges
        if (nlpResult.getExtractedEntities().getPriceRange() != null) {
            suggestions.put("similar_price_ranges", generateSimilarPriceRanges(nlpResult.getExtractedEntities().getPriceRange()));
        }
        
        // Add search engine info
        suggestions.put("search_engine", searchResponse.getSearchType());
        
        return suggestions;
    }

    private String[] generateNoResultsSuggestions(NLPQueryResult nlpResult) {
        return new String[]{
            "Thử mở rộng khu vực tìm kiếm",
            "Xem xét các loại hình bất động sản tương tự",
            "Điều chỉnh mức giá",
            "Giảm số phòng ngủ yêu cầu"
        };
    }

    private String[] generateRefinementSuggestions(NLPQueryResult nlpResult) {
        return new String[]{
            "Chỉ định khu vực cụ thể hơn",
            "Đặt khoảng giá chính xác hơn",
            "Thêm yêu cầu về tiện ích",
            "Chọn loại hình bất động sản cụ thể"
        };
    }

    private String[] generateSimilarPriceRanges(NLPQueryResult.PriceRange priceRange) {
        return new String[]{
            "Dưới 3 tỷ",
            "3-5 tỷ", 
            "5-7 tỷ",
            "Trên 7 tỷ"
        };
    }

    private String[] getPropertyTypeSuggestions(String query) {
        return new String[]{"căn hộ", "nhà riêng", "biệt thự", "shophouse", "đất nền"};
    }

    private String[] getLocationSuggestions(String query) {
        return new String[]{"Quận 1", "Quận 7", "Thủ Đức", "Bình Thạnh", "Phú Nhuận"};
    }

    private String[] getPriceRangeSuggestions() {
        return new String[]{"Dưới 2 tỷ", "2-3 tỷ", "3-5 tỷ", "5-10 tỷ", "Trên 10 tỷ"};
    }

    private String[] getAmenitySuggestions(String query) {
        return new String[]{"gần trường học", "hồ bơi", "gym", "view sông", "thang máy"};
    }

    // DTOs for request/response
    public static class NaturalLanguageSearchRequest {
        private String query;
        private Integer page;
        private Integer size;
        
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        public Integer getPage() { return page; }
        public void setPage(Integer page) { this.page = page; }
        public Integer getSize() { return size; }
        public void setSize(Integer size) { this.size = size; }
    }

    public static class AnalyzeQueryRequest {
        private String query;
        
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
    }

    public static class SmartSearchResponse {
        private String originalQuery;
        private String normalizedQuery;
        private NLPQueryResult.ExtractedEntities extractedEntities;
        private Double confidenceScore;
        private SearchListingRequest searchRequest;
        private SearchListingResponse searchResults;
        private Map<String, Object> suggestions;
        private String searchEngine;
        private boolean success;
        private String error;

        public static SmartSearchResponseBuilder builder() {
            return new SmartSearchResponseBuilder();
        }

        public static SmartSearchResponse error(String error) {
            SmartSearchResponse response = new SmartSearchResponse();
            response.success = false;
            response.error = error;
            return response;
        }

        // Getters and setters
        public String getOriginalQuery() { return originalQuery; }
        public void setOriginalQuery(String originalQuery) { this.originalQuery = originalQuery; }
        public String getNormalizedQuery() { return normalizedQuery; }
        public void setNormalizedQuery(String normalizedQuery) { this.normalizedQuery = normalizedQuery; }
        public NLPQueryResult.ExtractedEntities getExtractedEntities() { return extractedEntities; }
        public void setExtractedEntities(NLPQueryResult.ExtractedEntities extractedEntities) { this.extractedEntities = extractedEntities; }
        public Double getConfidenceScore() { return confidenceScore; }
        public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }
        public SearchListingRequest getSearchRequest() { return searchRequest; }
        public void setSearchRequest(SearchListingRequest searchRequest) { this.searchRequest = searchRequest; }
        public SearchListingResponse getSearchResults() { return searchResults; }
        public void setSearchResults(SearchListingResponse searchResults) { this.searchResults = searchResults; }
        public Map<String, Object> getSuggestions() { return suggestions; }
        public void setSuggestions(Map<String, Object> suggestions) { this.suggestions = suggestions; }
        public String getSearchEngine() { return searchEngine; }
        public void setSearchEngine(String searchEngine) { this.searchEngine = searchEngine; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }

        public static class SmartSearchResponseBuilder {
            private final SmartSearchResponse response = new SmartSearchResponse();

            public SmartSearchResponseBuilder originalQuery(String originalQuery) {
                response.originalQuery = originalQuery;
                return this;
            }

            public SmartSearchResponseBuilder normalizedQuery(String normalizedQuery) {
                response.normalizedQuery = normalizedQuery;
                return this;
            }

            public SmartSearchResponseBuilder extractedEntities(NLPQueryResult.ExtractedEntities extractedEntities) {
                response.extractedEntities = extractedEntities;
                return this;
            }

            public SmartSearchResponseBuilder confidenceScore(Double confidenceScore) {
                response.confidenceScore = confidenceScore;
                return this;
            }

            public SmartSearchResponseBuilder searchRequest(SearchListingRequest searchRequest) {
                response.searchRequest = searchRequest;
                return this;
            }

            public SmartSearchResponseBuilder searchResults(SearchListingResponse searchResults) {
                response.searchResults = searchResults;
                return this;
            }

            public SmartSearchResponseBuilder suggestions(Map<String, Object> suggestions) {
                response.suggestions = suggestions;
                return this;
            }

            public SmartSearchResponseBuilder searchEngine(String searchEngine) {
                response.searchEngine = searchEngine;
                return this;
            }

            public SmartSearchResponseBuilder success(boolean success) {
                response.success = success;
                return this;
            }

            public SmartSearchResponse build() {
                return response;
            }
        }
    }
} 