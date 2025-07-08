package com.proptech.realestate.service;

import com.proptech.realestate.model.dto.CreateListingRequest;
import com.proptech.realestate.model.entity.Listing;
import com.proptech.realestate.model.entity.ListingCategory;
import com.proptech.realestate.model.entity.User;
import com.proptech.realestate.repository.ListingCategoryRepository;
import com.proptech.realestate.repository.ListingRepository;
import com.proptech.realestate.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import com.proptech.realestate.model.entity.AttributeDefinition;
import com.proptech.realestate.model.entity.ListingAttribute;
import com.proptech.realestate.repository.AttributeDefinitionRepository;

@Service
@RequiredArgsConstructor
public class ListingService {

    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final ListingCategoryRepository listingCategoryRepository;
    private final AttributeDefinitionRepository attributeDefinitionRepository;

    @Transactional
    public Listing createListing(CreateListingRequest request) {
        // Find related entities
        User user = userRepository.findById(UUID.fromString(request.getUserId()))
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + request.getUserId()));

        ListingCategory category = listingCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + request.getCategoryId()));


        Listing listing = new Listing();

        // Basic mapping from DTO
        listing.setTitle(request.getTitle());
        listing.setDescription(request.getDescription());
        listing.setPrice(request.getPrice());
        listing.setArea(request.getArea());
        listing.setNumBedrooms(request.getNumBedrooms());
        listing.setNumBathrooms(request.getNumBathrooms());
        listing.setFullAddress(request.getFullAddress());
        
        // Set related entities
        listing.setUser(user);
        listing.setCategory(category);

        // --- Handle Dynamic Attributes ---
        if (request.getAttributes() != null) {
            for (Map.Entry<String, String> entry : request.getAttributes().entrySet()) {
                String attrCode = entry.getKey();
                String attrValue = entry.getValue();

                AttributeDefinition definition = attributeDefinitionRepository.findByCode(attrCode)
                        .orElseThrow(() -> new EntityNotFoundException("Attribute definition not found for code: " + attrCode));

                ListingAttribute listingAttribute = new ListingAttribute();
                
                // Set composite key values
                listingAttribute.getId().setListingId(listing.getId()); // This is null on creation, Hibernate handles it
                listingAttribute.getId().setAttributeId(definition.getId());

                // Set relationships
                listingAttribute.setListing(listing);
                listingAttribute.setDefinition(definition);
                listingAttribute.setValue(attrValue);

                listing.getAttributes().add(listingAttribute);
            }
        }

        // Set default/generated values
        listing.setListingCode(generateListingCode());
        listing.setStatus(Listing.ListingStatus.ACTIVE); // Default status
        listing.setListingType(Listing.ListingType.SALE); // Default type

        return listingRepository.save(listing);
    }

    private String generateListingCode() {
        // Simple random code generator, e.g., "PROP-123456"
        int number = java.util.concurrent.ThreadLocalRandom.current().nextInt(100000, 999999);
        return "PROP-" + number;
    }
} 
