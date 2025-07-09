package com.proptech.realestate.service;

import com.proptech.realestate.model.dto.reso.PropertyResource;
import com.proptech.realestate.model.dto.reso.MemberResource;
import com.proptech.realestate.model.dto.reso.OfficeResource;
import com.proptech.realestate.model.entity.Listing;
import com.proptech.realestate.model.entity.User;
import com.proptech.realestate.model.entity.Office;
import com.proptech.realestate.model.entity.MLSMembership;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for mapping internal entities to RESO-compliant DTOs
 * Ensures compliance with RESO Data Dictionary 2.0 standards
 */
@Service
public class ResoMappingService {
    
    /**
     * Maps internal Listing entity to RESO PropertyResource
     */
    public PropertyResource mapToPropertyResource(Listing listing) {
        if (listing == null) {
            return null;
        }
        
        PropertyResource.PropertyResourceBuilder builder = PropertyResource.builder()
            // Core Property Information
            .listingKey(listing.getId().toString())
            .listingId(listing.getMlsNumber())
            .listPrice(listing.getPrice())
            .originalListPrice(listing.getOriginalListPrice())
            
            // Property Type & Features
            .propertyType(mapPropertyType(listing.getCategory()))
            .propertySubType(mapPropertySubType(listing.getCategory()))
            .bedroomsTotal(listing.getNumBedrooms())
            .bathroomsTotalInteger(listing.getNumBathrooms())
            .livingArea(listing.getArea() != null ? BigDecimal.valueOf(listing.getArea()) : null)
            .livingAreaUnits("Square Feet")
            
            // Address Information
            .unparsedAddress(listing.getFullAddress())
            .city(listing.getCity())
            .stateOrProvince(listing.getState())
            .postalCode(listing.getZipCode())
            .country(listing.getCountry() != null ? listing.getCountry() : "US")
            
            // Geographic Information
            .latitude(listing.getLatitude() != null ? BigDecimal.valueOf(listing.getLatitude()) : null)
            .longitude(listing.getLongitude() != null ? BigDecimal.valueOf(listing.getLongitude()) : null)
            
            // Listing Information
            .standardStatus(mapStandardStatus(listing.getStatus()))
            .mlsStatus(listing.getStatus().name())
            .listingContractDate(listing.getCreatedAt())
            .onMarketDate(listing.getCreatedAt())
            .modificationTimestamp(listing.getUpdatedAt())
            
            // Property Description
            .publicRemarks(listing.getDescription())
            .yearBuilt(listing.getYearBuilt());
            
        // Add listing agent information if available
        if (listing.getListingAgent() != null) {
            User agent = listing.getListingAgent();
            builder
                .listAgentKey(agent.getId().toString())
                .listAgentMlsId(agent.getMlsId())
                .listAgentFirstName(agent.getFirstName())
                .listAgentLastName(agent.getLastName())
                .listAgentFullName(agent.getFirstName() + " " + agent.getLastName())
                .listAgentEmail(agent.getEmail())
                .listAgentDirectPhone(agent.getPhone());
        }
        
        // Add listing office information if available
        if (listing.getListingOffice() != null) {
            Office office = listing.getListingOffice();
            builder
                .listOfficeKey(office.getId().toString())
                .listOfficeMlsId(office.getOfficeMlsId())
                .listOfficeName(office.getOfficeName())
                .listOfficePhone(office.getPhone());
        }
        
        // Add selling agent/office if property is sold
        if (listing.getSellingAgent() != null) {
            User buyerAgent = listing.getSellingAgent();
            builder
                .buyerAgentKey(buyerAgent.getId().toString())
                .buyerAgentMlsId(buyerAgent.getMlsId())
                .buyerAgentFullName(buyerAgent.getFirstName() + " " + buyerAgent.getLastName());
        }
        
        if (listing.getSellingOffice() != null) {
            Office buyerOffice = listing.getSellingOffice();
            builder
                .buyerOfficeKey(buyerOffice.getId().toString())
                .buyerOfficeName(buyerOffice.getOfficeName());
        }
        
        // Add showing information
        if (listing.getShowingInstructions() != null && !listing.getShowingInstructions().isEmpty()) {
            // Get first showing instruction as default
            var showingInstruction = listing.getShowingInstructions().iterator().next();
            builder
                .showingContactType("Listing Agent")
                .showingContactName(showingInstruction.getContactName())
                .showingContactPhone(showingInstruction.getContactPhone())
                .showingInstructions(showingInstruction.getInstructions());
        }
        
        // Calculate days on market
        if (listing.getCreatedAt() != null) {
            long daysOnMarket = java.time.temporal.ChronoUnit.DAYS.between(
                listing.getCreatedAt().toLocalDate(), 
                LocalDateTime.now().toLocalDate()
            );
            builder.daysOnMarket((int) daysOnMarket);
        }
        
        // Add commission information if cooperation is allowed
        if (listing.getCommissionStructure() != null && listing.getIsSharedListing()) {
            var commission = listing.getCommissionStructure();
            if (commission.getSellingSidePercentage() != null) {
                builder
                    .buyerAgencyCompensation(commission.getSellingSidePercentage().toString() + "%")
                    .buyerAgencyCompensationType("Percentage");
            }
        }
        
        return builder.build();
    }
    
    /**
     * Maps internal User entity (with MLS membership) to RESO MemberResource
     */
    public MemberResource mapToMemberResource(User user, MLSMembership membership) {
        if (user == null) {
            return null;
        }
        
        MemberResource.MemberResourceBuilder builder = MemberResource.builder()
            // Core Member Information
            .memberKey(user.getId().toString())
            .memberMlsId(user.getMlsId())
            .memberStateLicense(user.getLicenseNumber())
            .memberStateLicenseState(user.getLicenseState())
            
            // Personal Information
            .memberFirstName(user.getFirstName())
            .memberLastName(user.getLastName())
            .memberFullName(user.getFirstName() + " " + user.getLastName())
            .memberEmail(user.getEmail())
            .memberDirectPhone(user.getPhone())
            
            // Professional Information
            .memberType(mapMemberType(user.getUserType()))
            .originalEntryTimestamp(user.getCreatedAt())
            .modificationTimestamp(user.getUpdatedAt());
            
        // Add membership-specific information
        if (membership != null) {
            builder
                .memberStatus(mapMemberStatus(membership.getStatus()))
                .memberMlsAccessYN(membership.getCanViewAllListings())
                .memberStateLicenseExpirationDate(membership.getLicenseExpiryDate());
                
            // Add office information if available
            if (membership.getOffice() != null) {
                Office office = membership.getOffice();
                builder
                    .officeKey(office.getId().toString())
                    .officeMlsId(office.getOfficeMlsId())
                    .officeName(office.getOfficeName());
            }
        }
        
        return builder.build();
    }
    
    /**
     * Maps internal Office entity to RESO OfficeResource
     */
    public OfficeResource mapToOfficeResource(Office office) {
        if (office == null) {
            return null;
        }
        
        return OfficeResource.builder()
            // Core Office Information
            .officeKey(office.getId().toString())
            .officeMlsId(office.getOfficeMlsId())
            .officeStateLicense(office.getLicenseNumber())
            .officeStateLicenseState(office.getState())
            
            // Office Identity
            .officeName(office.getOfficeName())
            .officeType(mapOfficeType(office.getOfficeType()))
            .officeStatus("Active") // Assuming active if not deleted
            
            // Contact Information
            .officeEmail(office.getEmail())
            .officePhone(office.getPhone())
            .officeURL(office.getWebsite())
            
            // Address Information
            .officeAddress1(office.getStreetAddress())
            .officeCity(office.getCity())
            .officeStateOrProvince(office.getState())
            .officePostalCode(office.getZipCode())
            .officeCountry(office.getCountry())
            
            // Geographic Information
            .latitude(office.getLatitude() != null ? office.getLatitude().toString() : null)
            .longitude(office.getLongitude() != null ? office.getLongitude().toString() : null)
            
            // Corporate Information
            .franchiseAffiliation(office.getFranchiseName())
            
            // Statistics
            .officeAgentCount(office.getTotalAgents())
            
            // Dates
            .officeEstablishedDate(office.getEstablishedDate())
            .originalEntryTimestamp(office.getCreatedAt())
            .modificationTimestamp(office.getUpdatedAt())
            
            .build();
    }
    
    /**
     * Maps multiple listings to RESO PropertyResources
     */
    public List<PropertyResource> mapToPropertyResources(List<Listing> listings) {
        return listings.stream()
            .map(this::mapToPropertyResource)
            .collect(Collectors.toList());
    }
    
    /**
     * Maps multiple users/memberships to RESO MemberResources
     */
    public List<MemberResource> mapToMemberResources(List<MLSMembership> memberships) {
        return memberships.stream()
            .map(membership -> mapToMemberResource(membership.getUser(), membership))
            .collect(Collectors.toList());
    }
    
    /**
     * Maps multiple offices to RESO OfficeResources
     */
    public List<OfficeResource> mapToOfficeResources(List<Office> offices) {
        return offices.stream()
            .map(this::mapToOfficeResource)
            .collect(Collectors.toList());
    }
    
    /**
     * Maps RESO PropertyResource to internal Listing entity
     * Used for IDX feed processing
     */
    public Listing mapResoToProperty(PropertyResource resoProperty, String mlsId) {
        if (resoProperty == null) {
            return null;
        }

        Listing listing = new Listing();
        
        // Basic identification
        listing.setListingId(resoProperty.getListingId());
        listing.setMlsId(mlsId);
        
        // Price information
        listing.setListPrice(resoProperty.getListPrice());
        listing.setOriginalListPrice(resoProperty.getOriginalListPrice());
        
        // Property details
        listing.setPropertyType(resoProperty.getPropertyType());
        listing.setPropertySubType(resoProperty.getPropertySubType());
        listing.setBedroomsTotal(resoProperty.getBedroomsTotal());
        listing.setBathroomsTotalInteger(resoProperty.getBathroomsTotalInteger());
        
        // Area information
        if (resoProperty.getLivingArea() != null) {
            listing.setLivingArea(resoProperty.getLivingArea().doubleValue());
        }
        
        // Address information
        listing.setUnparsedAddress(resoProperty.getUnparsedAddress());
        listing.setCity(resoProperty.getCity());
        listing.setStateOrProvince(resoProperty.getStateOrProvince());
        listing.setPostalCode(resoProperty.getPostalCode());
        listing.setCountry(resoProperty.getCountry());
        
        // Geographic coordinates
        if (resoProperty.getLatitude() != null) {
            listing.setLatitude(resoProperty.getLatitude().doubleValue());
        }
        if (resoProperty.getLongitude() != null) {
            listing.setLongitude(resoProperty.getLongitude().doubleValue());
        }
        
        // Status information
        listing.setStandardStatus(resoProperty.getStandardStatus());
        listing.setMlsStatus(resoProperty.getMlsStatus());
        
        // Dates and timestamps
        listing.setListingContractDate(resoProperty.getListingContractDate());
        listing.setOnMarketDate(resoProperty.getOnMarketDate());
        listing.setModificationTimestamp(resoProperty.getModificationTimestamp());
        
        // Property description
        listing.setPublicRemarks(resoProperty.getPublicRemarks());
        listing.setPrivateRemarks(resoProperty.getPrivateRemarks());
        
        // Additional property details
        listing.setYearBuilt(resoProperty.getYearBuilt());
        listing.setDaysOnMarket(resoProperty.getDaysOnMarket());
        
        // Lot size information
        if (resoProperty.getLotSizeAcres() != null) {
            listing.setLotSizeAcres(resoProperty.getLotSizeAcres().doubleValue());
        }
        if (resoProperty.getLotSizeSquareFeet() != null) {
            listing.setLotSizeSquareFeet(resoProperty.getLotSizeSquareFeet().doubleValue());
        }
        
        // Set timestamps
        LocalDateTime now = LocalDateTime.now();
        if (listing.getCreatedAt() == null) {
            listing.setCreatedAt(now);
        }
        listing.setUpdatedAt(now);
        
        return listing;
    }

    // Helper mapping methods
    
    private String mapPropertyType(com.proptech.realestate.model.entity.ListingCategory category) {
        if (category == null) return "Residential";
        
        // Map internal category to RESO standard values
        return switch (category.getName().toLowerCase()) {
            case "house", "single family", "home" -> "Residential";
            case "apartment", "condo", "condominium" -> "Residential";
            case "commercial", "office", "retail" -> "Commercial";
            case "land", "lot" -> "Land";
            default -> "Residential";
        };
    }
    
    private String mapPropertySubType(com.proptech.realestate.model.entity.ListingCategory category) {
        if (category == null) return "Single Family Residence";
        
        return switch (category.getName().toLowerCase()) {
            case "house", "single family", "home" -> "Single Family Residence";
            case "apartment" -> "Apartment";
            case "condo", "condominium" -> "Condominium";
            case "townhouse" -> "Townhouse";
            case "commercial" -> "Commercial";
            case "office" -> "Office";
            case "retail" -> "Retail";
            case "land", "lot" -> "Vacant Land";
            default -> "Single Family Residence";
        };
    }
    
    private String mapStandardStatus(com.proptech.realestate.model.enums.ListingStatus status) {
        if (status == null) return "Active";
        
        return switch (status) {
            case ACTIVE -> "Active";
            case PENDING -> "Pending";
            case SOLD -> "Closed";
            case CANCELLED -> "Canceled";
            case EXPIRED -> "Expired";
            case WITHDRAWN -> "Withdrawn";
            default -> "Active";
        };
    }
    
    private String mapMemberType(com.proptech.realestate.model.enums.UserType userType) {
        if (userType == null) return "Agent";
        
        return switch (userType) {
            case AGENT -> "Agent";
            case BROKER -> "Broker";
            case ADMIN -> "Office Manager";
            default -> "Agent";
        };
    }
    
    private String mapMemberStatus(com.proptech.realestate.model.entity.MLSMembership.MembershipStatus status) {
        if (status == null) return "Active";
        
        return switch (status) {
            case ACTIVE -> "Active";
            case SUSPENDED -> "Suspended";
            case EXPIRED -> "Inactive";
            case TERMINATED -> "Inactive";
            default -> "Active";
        };
    }
    
    private String mapOfficeType(com.proptech.realestate.model.entity.Office.OfficeType officeType) {
        if (officeType == null) return "Brokerage";
        
        return switch (officeType) {
            case BROKERAGE -> "Brokerage";
            case FRANCHISE -> "Franchise";
            case BRANCH_OFFICE -> "Branch";
            default -> "Brokerage";
        };
    }
}
