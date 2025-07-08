package com.proptech.realestate.service;

import com.proptech.realestate.model.dto.SearchListingRequest;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final ListingRepository listingRepository;
    
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    
    public SearchListingResponse search(SearchListingRequest request) {
        long startTime = System.currentTimeMillis();
        
        log.info("Starting search with parameters: {}", request);
        
        // Validate and prepare pagination
        int page = request.getPage() != null ? Math.max(0, request.getPage()) : 0;
        int size = request.getSize() != null ? 
                   Math.min(Math.max(1, request.getSize()), MAX_PAGE_SIZE) : 
                   DEFAULT_PAGE_SIZE;
        
        // Prepare sorting
        Pageable pageable = createPageable(page, size, request.getSortBy(), request.getSortDirection());
        
        // Convert categoryId string to UUID if provided
        UUID categoryId = null;
        if (request.getCategoryId() != null && !request.getCategoryId().trim().isEmpty()) {
            try {
                categoryId = UUID.fromString(request.getCategoryId());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid category ID format: {}", request.getCategoryId());
            }
        }
        
        // Execute search
        Page<Listing> resultPage = listingRepository.searchListings(
                request.getKeyword(),
                request.getLocation(),
                request.getListingType(),
                request.getStatus(),
                categoryId,
                request.getMinPrice(),
                request.getMaxPrice(),
                request.getMinArea(),
                request.getMaxArea(),
                request.getMinBedrooms(),
                request.getMaxBedrooms(),
                request.getMinBathrooms(),
                request.getMaxBathrooms(),
                pageable
        );
        
        // Convert to DTOs
        List<SearchListingResponse.ListingSearchResult> searchResults = resultPage.getContent()
                .stream()
                .map(this::convertToSearchResult)
                .collect(Collectors.toList());
        
        // Build metadata
        long searchTime = System.currentTimeMillis() - startTime;
        SearchListingResponse.SearchMetadata metadata = SearchListingResponse.SearchMetadata.builder()
                .totalCount(resultPage.getTotalElements())
                .totalPages(resultPage.getTotalPages())
                .currentPage(resultPage.getNumber())
                .pageSize(resultPage.getSize())
                .hasNext(resultPage.hasNext())
                .hasPrevious(resultPage.hasPrevious())
                .sortBy(request.getSortBy())
                .sortDirection(request.getSortDirection())
                .searchTimeMs(searchTime)
                .build();
        
        log.info("Search completed in {} ms, found {} results", searchTime, resultPage.getTotalElements());
        
        return SearchListingResponse.builder()
                .listings(searchResults)
                .metadata(metadata)
                .build();
    }
    
    private Pageable createPageable(int page, int size, String sortBy, String sortDirection) {
        if (sortBy == null || sortBy.trim().isEmpty()) {
            sortBy = "createdAt";
        }
        
        Sort.Direction direction = Sort.Direction.DESC;
        if ("ASC".equalsIgnoreCase(sortDirection)) {
            direction = Sort.Direction.ASC;
        }
        
        // Validate sort field
        String finalSortBy = validateSortField(sortBy);
        
        Sort sort = Sort.by(direction, finalSortBy);
        return PageRequest.of(page, size, sort);
    }
    
    private String validateSortField(String sortBy) {
        // Allowed sort fields to prevent SQL injection
        switch (sortBy.toLowerCase()) {
            case "price":
                return "listPrice";
            case "area":
                return "livingArea";
            case "createdat":
            case "created_at":
                return "modificationTimestamp";
            case "updatedat":
            case "updated_at":
                return "modificationTimestamp";
            case "title":
                return "title";
            case "bedrooms":
            case "numbedrooms":
            case "num_bedrooms":
                return "bedroomsTotal";
            case "bathrooms":
            case "numbathrooms":
            case "num_bathrooms":
                return "bathroomsTotalInteger";
            default:
                return "modificationTimestamp";
        }
    }
    
    public SearchListingResponse.ListingSearchResult convertToSearchResult(Listing listing) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        // Calculate price per square meter
        String pricePerSqm = null;
        if (listing.getListPrice() != null && listing.getLivingArea() != null && listing.getLivingArea() > 0) {
            BigDecimal pricePerSqmValue = listing.getListPrice()
                    .divide(BigDecimal.valueOf(listing.getLivingArea()), 2, RoundingMode.HALF_UP);
            pricePerSqm = formatPrice(pricePerSqmValue) + "/m²";
        }
        
        return SearchListingResponse.ListingSearchResult.builder()
                .id(listing.getId().toString())
                .listingCode(listing.getListingId())
                .title(listing.getTitle())
                .description(truncateDescription(listing.getPublicRemarks()))
                .price(formatPrice(listing.getListPrice()))
                .area(listing.getLivingArea() != null ? listing.getLivingArea() + " m²" : null)
                .numBedrooms(listing.getBedroomsTotal())
                .numBathrooms(listing.getBathroomsTotalInteger())
                .listingType(listing.getPropertyType())
                .status(listing.getStandardStatus() != null ? listing.getStandardStatus().name() : null)
                .address(listing.getUnparsedAddress())
                .categoryName(listing.getPropertySubType())
                .createdAt(listing.getModificationTimestamp())
                .updatedAt(listing.getModificationTimestamp() != null ? listing.getModificationTimestamp().format(formatter) : null)
                .pricePerSqm(listing.getListPrice() != null && listing.getLivingArea() != null && listing.getLivingArea() > 0 ? 
                    listing.getListPrice().divide(BigDecimal.valueOf(listing.getLivingArea()), 2, RoundingMode.HALF_UP).doubleValue() : null)
                .build();
    }
    
    private String formatPrice(BigDecimal price) {
        if (price == null) {
            return null;
        }
        
        // Format with Vietnamese currency
        if (price.compareTo(BigDecimal.valueOf(1_000_000_000)) >= 0) {
            BigDecimal billions = price.divide(BigDecimal.valueOf(1_000_000_000), 1, RoundingMode.HALF_UP);
            return billions + " tỷ VNĐ";
        } else if (price.compareTo(BigDecimal.valueOf(1_000_000)) >= 0) {
            BigDecimal millions = price.divide(BigDecimal.valueOf(1_000_000), 0, RoundingMode.HALF_UP);
            return millions + " triệu VNĐ";
        } else {
            return price.toPlainString() + " VNĐ";
        }
    }
    
    private String truncateDescription(String description) {
        if (description == null) {
            return null;
        }
        
        final int MAX_LENGTH = 150;
        if (description.length() <= MAX_LENGTH) {
            return description;
        }
        
        return description.substring(0, MAX_LENGTH) + "...";
    }
} 