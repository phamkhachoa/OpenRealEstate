package com.proptech.realestate.service;

import com.proptech.realestate.model.dto.CreateListingRequest;
import com.proptech.realestate.model.entity.Listing;
import com.proptech.realestate.model.entity.User;
import com.proptech.realestate.model.enums.PropertyType;
import com.proptech.realestate.model.enums.StandardStatus;
import com.proptech.realestate.repository.ListingRepository;
import com.proptech.realestate.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListingService {

    private final ListingRepository listingRepository;
    private final UserRepository userRepository;

    @Transactional
    public Listing createListing(CreateListingRequest request) {
        User user = userRepository.findById(UUID.fromString(request.getUserId()))
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + request.getUserId()));

        Listing listing = new Listing();

        // Map from RESO-aligned DTO
        listing.setTitle(request.getTitle());
        listing.setPublicRemarks(request.getPublicRemarks());
        listing.setListPrice(request.getListPrice());
        listing.setLivingArea(request.getLivingArea());
        listing.setBedroomsTotal(request.getBedroomsTotal());
        listing.setBathroomsTotalInteger(request.getBathroomsTotalInteger());
        listing.setUnparsedAddress(request.getUnparsedAddress());
        listing.setLatitude(request.getLatitude());
        listing.setLongitude(request.getLongitude());
        listing.setAdditionalDetails(request.getAdditionalDetails());

        // Set relationships and enums
        listing.setUser(user);
        try {
            listing.setPropertyType(PropertyType.valueOf(request.getPropertyType()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid PropertyType: " + request.getPropertyType());
        }

        // Set default/generated values
        listing.setListingId(generateListingId());
        listing.setStandardStatus(StandardStatus.Active); // Default to Active
        listing.setModificationTimestamp(LocalDateTime.now());

        return listingRepository.save(listing);
    }

    private String generateListingId() {
        // Simple random code generator, e.g., "MLS-123456"
        int number = java.util.concurrent.ThreadLocalRandom.current().nextInt(100000, 999999);
        return "MLS-" + number;
    }
} 
