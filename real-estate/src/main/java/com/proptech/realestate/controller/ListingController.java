package com.proptech.realestate.controller;

import com.proptech.realestate.model.dto.CreateListingRequest;
import com.proptech.realestate.model.dto.SearchListingRequest;
import com.proptech.realestate.model.dto.SearchListingResponse;
import com.proptech.realestate.model.entity.Listing;
import com.proptech.realestate.service.ListingService;
import com.proptech.realestate.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/listings")
@RequiredArgsConstructor
public class ListingController {

    private final ListingService listingService;
    private final SearchService searchService;

    @PostMapping
    public ResponseEntity<Listing> createListing(@RequestBody CreateListingRequest request) {
        Listing createdListing = listingService.createListing(request);
        return new ResponseEntity<>(createdListing, HttpStatus.CREATED);
    }
    
    @GetMapping("/search")
    public ResponseEntity<SearchListingResponse> searchListings(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String listingType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String minPrice,
            @RequestParam(required = false) String maxPrice,
            @RequestParam(required = false) String minArea,
            @RequestParam(required = false) String maxArea,
            @RequestParam(required = false) Integer minBedrooms,
            @RequestParam(required = false) Integer maxBedrooms,
            @RequestParam(required = false) Integer minBathrooms,
            @RequestParam(required = false) Integer maxBathrooms,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size
    ) {
        SearchListingRequest searchRequest = SearchListingRequest.builder()
                .keyword(keyword)
                .location(location)
                .listingType(listingType)
                .status(status)
                .categoryId(categoryId)
                .minPrice(parsePrice(minPrice))
                .maxPrice(parsePrice(maxPrice))
                .minArea(parseArea(minArea))
                .maxArea(parseArea(maxArea))
                .minBedrooms(minBedrooms)
                .maxBedrooms(maxBedrooms)
                .minBathrooms(minBathrooms)
                .maxBathrooms(maxBathrooms)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .page(page)
                .size(size)
                .build();
        
        SearchListingResponse response = searchService.search(searchRequest);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/search")
    public ResponseEntity<SearchListingResponse> searchListingsAdvanced(@RequestBody SearchListingRequest request) {
        SearchListingResponse response = searchService.search(request);
        return ResponseEntity.ok(response);
    }
    
    private java.math.BigDecimal parsePrice(String priceStr) {
        if (priceStr == null || priceStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Remove common formatting characters
            String cleaned = priceStr.replaceAll("[,\\s]", "");
            return new java.math.BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private Double parseArea(String areaStr) {
        if (areaStr == null || areaStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Remove common formatting characters
            String cleaned = areaStr.replaceAll("[,\\s]", "");
            return Double.valueOf(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }
} 