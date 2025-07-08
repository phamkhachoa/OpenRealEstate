package com.proptech.realestate.service;

import com.proptech.realestate.model.dto.SearchListingResponse;
import com.proptech.realestate.model.entity.Listing;
import com.proptech.realestate.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DynamicSearchService {

    private final EntityManager entityManager;
    private final SearchService searchService;
    
    /**
     * Advanced search with dynamic attributes using Criteria API
     */
    public SearchListingResponse searchWithDynamicAttributes(
            String keyword,
            Map<String, String> dynamicAttributes,
            int page,
            int size,
            String sortBy,
            String sortDirection) {
        
        long startTime = System.currentTimeMillis();
        
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Listing> query = cb.createQuery(Listing.class);
        Root<Listing> listing = query.from(Listing.class);
        
        List<Predicate> predicates = new ArrayList<>();
        
        // Basic keyword search
        if (keyword != null && !keyword.trim().isEmpty()) {
            String likePattern = "%" + keyword.toLowerCase() + "%";
            Predicate titlePredicate = cb.like(cb.lower(listing.get("title")), likePattern);
            Predicate descPredicate = cb.like(cb.lower(listing.get("description")), likePattern);
            predicates.add(cb.or(titlePredicate, descPredicate));
        }
        
        // Dynamic attributes search
        if (dynamicAttributes != null && !dynamicAttributes.isEmpty()) {
            for (Map.Entry<String, String> attribute : dynamicAttributes.entrySet()) {
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<Listing> subListing = subquery.from(Listing.class);
                Join<Object, Object> attributeJoin = subListing.join("attributes");
                Join<Object, Object> attributeDefJoin = attributeJoin.join("definition");
                
                subquery.select(subListing.get("id"))
                        .where(
                                cb.and(
                                        cb.equal(subListing.get("id"), listing.get("id")),
                                        cb.equal(attributeDefJoin.get("name"), attribute.getKey()),
                                        cb.equal(attributeJoin.get("value"), attribute.getValue())
                                )
                        );
                
                predicates.add(cb.exists(subquery));
            }
        }
        
        // Apply predicates
        if (!predicates.isEmpty()) {
            query.where(cb.and(predicates.toArray(new Predicate[0])));
        }
        
        // Sorting
        if (sortBy != null && !sortBy.trim().isEmpty()) {
            Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ? 
                    Sort.Direction.ASC : Sort.Direction.DESC;
            
            if (direction == Sort.Direction.ASC) {
                query.orderBy(cb.asc(listing.get(validateSortField(sortBy))));
            } else {
                query.orderBy(cb.desc(listing.get(validateSortField(sortBy))));
            }
        } else {
            query.orderBy(cb.desc(listing.get("createdAt")));
        }
        
        // Execute query with pagination
        TypedQuery<Listing> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult(page * size);
        typedQuery.setMaxResults(size);
        
        List<Listing> results = typedQuery.getResultList();
        
        // Count total results
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Listing> countRoot = countQuery.from(Listing.class);
        countQuery.select(cb.count(countRoot));
        
        // Apply same predicates for count
        List<Predicate> countPredicates = new ArrayList<>();
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            String likePattern = "%" + keyword.toLowerCase() + "%";
            Predicate titlePredicate = cb.like(cb.lower(countRoot.get("title")), likePattern);
            Predicate descPredicate = cb.like(cb.lower(countRoot.get("description")), likePattern);
            countPredicates.add(cb.or(titlePredicate, descPredicate));
        }
        
        if (dynamicAttributes != null && !dynamicAttributes.isEmpty()) {
            for (Map.Entry<String, String> attribute : dynamicAttributes.entrySet()) {
                Subquery<Long> subquery = countQuery.subquery(Long.class);
                Root<Listing> subListing = subquery.from(Listing.class);
                Join<Object, Object> attributeJoin = subListing.join("attributes");
                Join<Object, Object> attributeDefJoin = attributeJoin.join("definition");
                
                subquery.select(subListing.get("id"))
                        .where(
                                cb.and(
                                        cb.equal(subListing.get("id"), countRoot.get("id")),
                                        cb.equal(attributeDefJoin.get("name"), attribute.getKey()),
                                        cb.equal(attributeJoin.get("value"), attribute.getValue())
                                )
                        );
                
                countPredicates.add(cb.exists(subquery));
            }
        }
        
        if (!countPredicates.isEmpty()) {
            countQuery.where(cb.and(countPredicates.toArray(new Predicate[0])));
        }
        
        Long totalElements = entityManager.createQuery(countQuery).getSingleResult();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        
        // Convert to response using SearchService
        List<SearchListingResponse.ListingSearchResult> searchResults = results.stream()
                .map(searchService::convertToSearchResult)
                .toList();
        
        long searchTime = System.currentTimeMillis() - startTime;
        
        SearchListingResponse.SearchMetadata metadata = SearchListingResponse.SearchMetadata.builder()
                .totalCount(totalElements)
                .totalPages(totalPages)
                .currentPage(page)
                .pageSize(size)
                .hasNext(page < totalPages - 1)
                .hasPrevious(page > 0)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .searchTimeMs(searchTime)
                .build();
        
        log.info("Dynamic search completed in {} ms, found {} results", searchTime, totalElements);
        
        return SearchListingResponse.builder()
                .listings(searchResults)
                .metadata(metadata)
                .build();
    }
    
    private String validateSortField(String sortBy) {
        switch (sortBy.toLowerCase()) {
            case "price":
                return "price";
            case "area":
                return "area";
            case "createdat":
            case "created_at":
                return "createdAt";
            case "updatedat":
            case "updated_at":
                return "updatedAt";
            case "title":
                return "title";
            case "numbedrooms":
            case "num_bedrooms":
                return "numBedrooms";
            case "numbathrooms":
            case "num_bathrooms":
                return "numBathrooms";
            default:
                return "createdAt";
        }
    }
} 