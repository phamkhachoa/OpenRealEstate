package com.proptech.realestate.repository;

import com.proptech.realestate.model.document.ListingDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ListingDocumentRepository extends ElasticsearchRepository<ListingDocument, String> {

    // Basic text search
    Page<ListingDocument> findBySearchTextContainingIgnoreCase(String searchText, Pageable pageable);

    // Find by status
    Page<ListingDocument> findByStatus(String status, Pageable pageable);

    // Find by listing type
    Page<ListingDocument> findByListingType(String listingType, Pageable pageable);

    // Find by category
    Page<ListingDocument> findByCategoryId(String categoryId, Pageable pageable);

    // Price range queries
    Page<ListingDocument> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    // Area range queries
    Page<ListingDocument> findByAreaBetween(Double minArea, Double maxArea, Pageable pageable);

    // Bedrooms queries
    Page<ListingDocument> findByNumBedroomsGreaterThanEqual(Integer minBedrooms, Pageable pageable);

    // City/District queries
    Page<ListingDocument> findByCityIgnoreCase(String city, Pageable pageable);
    Page<ListingDocument> findByDistrictIgnoreCase(String district, Pageable pageable);

    // Complex search query with multiple criteria
    @Query("""
        {
          "bool": {
            "must": [
              {
                "multi_match": {
                  "query": "?0",
                  "fields": ["searchText", "title^2", "description", "fullAddress^1.5"],
                  "fuzziness": "AUTO"
                }
              }
            ],
            "filter": [
              {
                "term": {
                  "status": "ACTIVE"
                }
              }
            ]
          }
        }
        """)
    Page<ListingDocument> searchWithFullText(String query, Pageable pageable);

    // Geospatial search - find within distance
    @Query("""
        {
          "bool": {
            "must": [
              {
                "geo_distance": {
                  "distance": "?1km",
                  "location": {
                    "lat": ?2,
                    "lon": ?3
                  }
                }
              }
            ],
            "filter": [
              {
                "term": {
                  "status": "ACTIVE"
                }
              }
            ]
          }
        }
        """)
    Page<ListingDocument> findWithinDistance(String query, Double distanceKm, Double lat, Double lon, Pageable pageable);

    // Advanced search with multiple filters
    @Query("""
        {
          "bool": {
            "must": [
              {
                "multi_match": {
                  "query": "?0",
                  "fields": ["searchText", "title^2", "description"],
                  "fuzziness": "AUTO"
                }
              }
            ],
            "filter": [
              {
                "term": {
                  "status": "ACTIVE"
                }
              },
              {
                "range": {
                  "price": {
                    "gte": ?1,
                    "lte": ?2
                  }
                }
              },
              {
                "range": {
                  "area": {
                    "gte": ?3,
                    "lte": ?4
                  }
                }
              }
            ]
          }
        }
        """)
    Page<ListingDocument> advancedSearch(
            String query,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Double minArea,
            Double maxArea,
            Pageable pageable
    );

    // Find similar listings based on price and area
    @Query("""
        {
          "bool": {
            "must_not": [
              {
                "term": {
                  "id": "?0"
                }
              }
            ],
            "should": [
              {
                "range": {
                  "price": {
                    "gte": ?1,
                    "lte": ?2
                  }
                }
              },
              {
                "range": {
                  "area": {
                    "gte": ?3,
                    "lte": ?4
                  }
                }
              },
              {
                "term": {
                  "categoryId": "?5"
                }
              }
            ],
            "filter": [
              {
                "term": {
                  "status": "ACTIVE"
                }
              }
            ]
          }
        }
        """)
    Page<ListingDocument> findSimilarListings(
            String excludeId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Double minArea,
            Double maxArea,
            String categoryId,
            Pageable pageable
    );

    // Autocomplete search for addresses and titles
    @Query("""
        {
          "bool": {
            "should": [
              {
                "match_phrase_prefix": {
                  "fullAddress": "?0"
                }
              },
              {
                "match_phrase_prefix": {
                  "title": "?0"
                }
              },
              {
                "match_phrase_prefix": {
                  "city": "?0"
                }
              },
              {
                "match_phrase_prefix": {
                  "district": "?0"
                }
              }
            ],
            "filter": [
              {
                "term": {
                  "status": "ACTIVE"
                }
              }
            ]
          }
        }
        """)
    List<ListingDocument> autocomplete(String prefix);
} 