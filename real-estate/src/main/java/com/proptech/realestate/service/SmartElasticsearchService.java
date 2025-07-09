package com.proptech.realestate.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.proptech.realestate.model.document.ListingDocument;
import com.proptech.realestate.model.dto.NLPQueryResult;
import com.proptech.realestate.model.dto.SearchListingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Smart Elasticsearch Service with NLP Integration
 * Combines Vietnamese NLP results with Elasticsearch advanced search capabilities
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SmartElasticsearchService {

    private final ElasticsearchClient elasticsearchClient;
    private final VietnameseNLPService nlpService;

    /**
     * Execute smart search using NLP-enhanced Elasticsearch query
     */
    public SearchListingResponse smartSearch(String naturalQuery, int page, int size) {
        try {
            log.info("Executing smart search for: {}", naturalQuery);
            
            // Process natural language query
            var nlpResultOpt = nlpService.processQuery(naturalQuery);
            if (nlpResultOpt.isEmpty()) {
                log.warn("NLP processing failed for query: {}", naturalQuery);
                return executeBasicTextSearch(naturalQuery, page, size);
            }
            
            NLPQueryResult nlpResult = nlpResultOpt.get();
            log.debug("NLP confidence score: {}", nlpResult.getConfidenceScore());
            
            // Build smart Elasticsearch query
            Query smartQuery = buildSmartQuery(nlpResult, naturalQuery);
            
            // Execute search with custom scoring
            SearchRequest searchRequest = SearchRequest.of(s -> s
                .index("listings")
                .query(smartQuery)
                .from(page * size)
                .size(size)
                .sort(sort -> sort
                    .score(sc -> sc.order(co.elastic.clients.elasticsearch._types.SortOrder.Desc))
                )
                .highlight(h -> h
                    .fields("title", f -> f)
                    .fields("description", f -> f)
                    .fields("fullAddress", f -> f)
                )
            );
            
            SearchResponse<ListingDocument> response = elasticsearchClient.search(searchRequest, ListingDocument.class);
            
            return convertToSearchResponse(response, nlpResult);
            
        } catch (Exception e) {
            log.error("Error executing smart search", e);
            // Fallback to basic search
            return executeBasicTextSearch(naturalQuery, page, size);
        }
    }

    /**
     * Build smart Elasticsearch query combining NLP results with advanced search
     */
    private Query buildSmartQuery(NLPQueryResult nlpResult, String originalQuery) {
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();
        
        NLPQueryResult.ExtractedEntities entities = nlpResult.getExtractedEntities();
        
        // 1. Property Type Filter (exact match)
        if (entities.getPropertyType() != null) {
            boolQuery.filter(TermQuery.of(t -> t
                .field("propertyType")
                .value(entities.getPropertyType())
            )._toQuery());
            log.debug("Added property type filter: {}", entities.getPropertyType());
        }
        
        // 2. Price Range Filter
        if (entities.getPriceRange() != null) {
            addPriceRangeQuery(boolQuery, entities.getPriceRange());
        }
        
        // 3. Area Range Filter  
        if (entities.getAreaRange() != null) {
            addAreaRangeQuery(boolQuery, entities.getAreaRange());
        }
        
        // 4. Bedrooms/Bathrooms Filter
        if (entities.getBedrooms() != null) {
            boolQuery.filter(TermQuery.of(t -> t
                .field("numBedrooms")
                .value(entities.getBedrooms())
            )._toQuery());
        }
        
        if (entities.getBathrooms() != null) {
            boolQuery.filter(TermQuery.of(t -> t
                .field("numBathrooms") 
                .value(entities.getBathrooms())
            )._toQuery());
        }
        
        // 5. Location Search (fuzzy + boost)
        addLocationSearch(boolQuery, entities.getLocation(), originalQuery);
        
        // 6. Amenities Search (multi-match)
        addAmenitiesSearch(boolQuery, entities.getAmenities());
        
        // 7. Full-text Search (boosted by confidence)
        addFullTextSearch(boolQuery, nlpResult);
        
        return boolQuery.build()._toQuery();
    }

    /**
     * Add price range query to bool query
     */
    private void addPriceRangeQuery(BoolQuery.Builder boolQuery, NLPQueryResult.PriceRange priceRange) {
        RangeQuery.Builder rangeQuery = new RangeQuery.Builder().field("price");
        
        String operator = priceRange.getOperator();
        if ("lt".equals(operator) && priceRange.getMaxPrice() != null) {
            rangeQuery.lt(co.elastic.clients.json.JsonData.of(priceRange.getMaxPrice()));
        } else if ("gt".equals(operator) && priceRange.getMinPrice() != null) {
            rangeQuery.gt(co.elastic.clients.json.JsonData.of(priceRange.getMinPrice()));
        } else if ("range".equals(operator)) {
            if (priceRange.getMinPrice() != null) {
                rangeQuery.gte(co.elastic.clients.json.JsonData.of(priceRange.getMinPrice()));
            }
            if (priceRange.getMaxPrice() != null) {
                rangeQuery.lte(co.elastic.clients.json.JsonData.of(priceRange.getMaxPrice()));
            }
        } else if ("eq".equals(operator) && priceRange.getExactPrice() != null) {
            // Exact price with ±20% tolerance
            long exactPrice = priceRange.getExactPrice();
            long minPrice = (long) (exactPrice * 0.8);
            long maxPrice = (long) (exactPrice * 1.2);
            rangeQuery.gte(co.elastic.clients.json.JsonData.of(minPrice))
                     .lte(co.elastic.clients.json.JsonData.of(maxPrice));
        }
        
        boolQuery.filter(rangeQuery.build()._toQuery());
        log.debug("Added price range filter: {}", priceRange);
    }

    /**
     * Add area range query to bool query
     */
    private void addAreaRangeQuery(BoolQuery.Builder boolQuery, NLPQueryResult.AreaRange areaRange) {
        RangeQuery.Builder rangeQuery = new RangeQuery.Builder().field("area");
        
        String operator = areaRange.getOperator();
        if ("lt".equals(operator) && areaRange.getMaxArea() != null) {
            rangeQuery.lt(co.elastic.clients.json.JsonData.of(areaRange.getMaxArea()));
        } else if ("gt".equals(operator) && areaRange.getMinArea() != null) {
            rangeQuery.gt(co.elastic.clients.json.JsonData.of(areaRange.getMinArea()));
        } else if ("range".equals(operator)) {
            if (areaRange.getMinArea() != null) {
                rangeQuery.gte(co.elastic.clients.json.JsonData.of(areaRange.getMinArea()));
            }
            if (areaRange.getMaxArea() != null) {
                rangeQuery.lte(co.elastic.clients.json.JsonData.of(areaRange.getMaxArea()));
            }
        } else if ("eq".equals(operator) && areaRange.getExactArea() != null) {
            // Exact area with ±15% tolerance
            double exactArea = areaRange.getExactArea();
            double minArea = exactArea * 0.85;
            double maxArea = exactArea * 1.15;
            rangeQuery.gte(co.elastic.clients.json.JsonData.of(minArea))
                     .lte(co.elastic.clients.json.JsonData.of(maxArea));
        }
        
        boolQuery.filter(rangeQuery.build()._toQuery());
        log.debug("Added area range filter: {}", areaRange);
    }

    /**
     * Add location search with fuzzy matching and boosting
     */
    private void addLocationSearch(BoolQuery.Builder boolQuery, 
                                 NLPQueryResult.LocationInfo location, 
                                 String originalQuery) {
        if (location == null) return;
        
        BoolQuery.Builder locationQuery = new BoolQuery.Builder();
        
        // District search (high priority)
        if (location.getDistrict() != null) {
            locationQuery.should(FuzzyQuery.of(f -> f
                .field("fullAddress")
                .value(location.getDistrict())
                .boost(3.0f)
                .fuzziness("AUTO")
            )._toQuery());
        }
        
        // Ward search (medium priority)
        if (location.getWard() != null) {
            locationQuery.should(FuzzyQuery.of(f -> f
                .field("fullAddress")
                .value(location.getWard())
                .boost(2.0f)
                .fuzziness("AUTO")
            )._toQuery());
        }
        
        // Area/landmark search (medium priority)
        if (location.getArea() != null) {
            locationQuery.should(MultiMatchQuery.of(m -> m
                .query(location.getArea())
                .fields("fullAddress", "description")
                .boost(2.0f)
                .fuzziness("AUTO")
            )._toQuery());
        }
        
        // NER location (if available)
        if (location.getNerLocation() != null) {
            locationQuery.should(MultiMatchQuery.of(m -> m
                .query(location.getNerLocation())
                .fields("fullAddress", "description")
                .boost(1.5f)
                .fuzziness("AUTO")
            )._toQuery());
        }
        
        if (!locationQuery.build().should().isEmpty()) {
            boolQuery.must(locationQuery.build()._toQuery());
            log.debug("Added location search: {}", location);
        }
    }

    /**
     * Add amenities search with keyword matching
     */
    private void addAmenitiesSearch(BoolQuery.Builder boolQuery, List<String> amenities) {
        if (amenities == null || amenities.isEmpty()) return;
        
        BoolQuery.Builder amenitiesQuery = new BoolQuery.Builder();
        
        for (String amenity : amenities) {
            String keyword = convertAmenityToVietnamese(amenity);
            if (keyword != null) {
                amenitiesQuery.should(MultiMatchQuery.of(m -> m
                    .query(keyword)
                    .fields("description", "title", "fullAddress")
                    .boost(1.5f)
                    .fuzziness("AUTO")
                )._toQuery());
            }
        }
        
        if (!amenitiesQuery.build().should().isEmpty()) {
            boolQuery.must(amenitiesQuery.build()._toQuery());
            log.debug("Added amenities search: {}", amenities);
        }
    }

    /**
     * Add full-text search with confidence-based boosting
     */
    private void addFullTextSearch(BoolQuery.Builder boolQuery, NLPQueryResult nlpResult) {
        // Use normalized query for better matching
        String searchText = nlpResult.getNormalizedQuery();
        
        // Boost based on NLP confidence
        float boost = (float) (nlpResult.getConfidenceScore() * 2.0); // 0.6 confidence = 1.2x boost
        
        MultiMatchQuery fullTextQuery = MultiMatchQuery.of(m -> m
            .query(searchText)
            .fields("title^3", "description^2", "fullAddress^1.5") // Field-specific boosting
            .boost(boost)
            .fuzziness("AUTO")
            .type(TextQueryType.BestFields)
        );
        
        boolQuery.should(fullTextQuery._toQuery());
        log.debug("Added full-text search with boost {}: {}", boost, searchText);
    }

    /**
     * Execute basic text search as fallback
     */
    private SearchListingResponse executeBasicTextSearch(String query, int page, int size) {
        try {
            MultiMatchQuery basicQuery = MultiMatchQuery.of(m -> m
                .query(query)
                .fields("title", "description", "fullAddress")
                .fuzziness("AUTO")
            );
            
            SearchRequest searchRequest = SearchRequest.of(s -> s
                .index("listings")
                .query(basicQuery._toQuery())
                .from(page * size)
                .size(size)
            );
            
            SearchResponse<ListingDocument> response = elasticsearchClient.search(searchRequest, ListingDocument.class);
            return convertToSearchResponse(response, null);
            
        } catch (Exception e) {
            log.error("Error executing basic text search", e);
            return SearchListingResponse.empty();
        }
    }

    /**
     * Convert Elasticsearch response to SearchListingResponse
     */
    private SearchListingResponse convertToSearchResponse(SearchResponse<ListingDocument> esResponse, 
                                                        NLPQueryResult nlpResult) {
        List<SearchListingResponse.ListingSearchResult> results = esResponse.hits().hits().stream()
            .map(hit -> {
                ListingDocument doc = hit.source();
                if (doc == null) return null;
                
                return SearchListingResponse.ListingSearchResult.builder()
                    .id(doc.getId())
                    .listingCode(doc.getListingCode())
                    .title(doc.getTitle())
                    .description(truncateDescription(doc.getDescription()))
                    .price(formatPrice(doc.getPrice()))
                    .area(doc.getArea() != null ? doc.getArea() + " m²" : null)
                    .numBedrooms(doc.getNumBedrooms())
                    .numBathrooms(doc.getNumBathrooms())
                    .fullAddress(doc.getFullAddress())
                    .propertyType(doc.getPropertyType())
                    .score(hit.score() != null ? hit.score().doubleValue() : 0.0)
                    .highlights(hit.highlight())
                    .build();
            })
            .filter(result -> result != null)
            .collect(Collectors.toList());
        
        return SearchListingResponse.builder()
            .results(results)
            .totalElements((int) esResponse.hits().total().value())
            .totalPages((int) Math.ceil(esResponse.hits().total().value() / (double) results.size()))
            .nlpConfidence(nlpResult != null ? nlpResult.getConfidenceScore() : null)
            .searchType("SMART_ELASTICSEARCH")
            .build();
    }

    /**
     * Convert amenity codes to Vietnamese keywords for search
     */
    private String convertAmenityToVietnamese(String amenityCode) {
        switch (amenityCode) {
            case "near_school": return "trường học";
            case "near_hospital": return "bệnh viện";
            case "near_market": return "chợ";
            case "near_supermarket": return "siêu thị";
            case "near_park": return "công viên";
            case "river_view": return "view sông";
            case "ocean_view": return "view biển";
            case "swimming_pool": return "hồ bơi";
            case "gym": return "gym phòng tập";
            case "security": return "bảo vệ an ninh";
            case "elevator": return "thang máy";
            case "parking": return "chỗ đậu xe";
            case "garage": return "gara nhà để xe";
            case "garden": return "sân vườn";
            case "balcony": return "ban công";
            default: return null;
        }
    }

    private String truncateDescription(String description) {
        if (description == null || description.length() <= 200) {
            return description;
        }
        return description.substring(0, 200) + "...";
    }

    private String formatPrice(BigDecimal price) {
        if (price == null) return null;
        
        long priceValue = price.longValue();
        if (priceValue >= 1_000_000_000) {
            return String.format("%.1f tỷ", priceValue / 1_000_000_000.0);
        } else if (priceValue >= 1_000_000) {
            return String.format("%.0f triệu", priceValue / 1_000_000.0);
        } else {
            return String.format("%,d VNĐ", priceValue);
        }
    }
} 