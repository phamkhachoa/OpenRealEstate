package com.proptech.realestate.model.dto.reso;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * RESO Property Resource - Compliant with RESO Data Dictionary 2.0
 * https://www.reso.org/data-dictionary-2-0/
 */
@Data
@Builder
public class PropertyResource {
    
    // Core Property Information (RESO Standard Fields)
    @JsonProperty("ListingKey")
    private String listingKey; // UUID as string
    
    @JsonProperty("ListingId") 
    private String listingId; // MLS Number
    
    @JsonProperty("ListPrice")
    private BigDecimal listPrice;
    
    @JsonProperty("OriginalListPrice")
    private BigDecimal originalListPrice;
    
    @JsonProperty("ListPriceLow")
    private BigDecimal listPriceLow;
    
    @JsonProperty("ListPriceHigh") 
    private BigDecimal listPriceHigh;
    
    // Property Type & Subtype
    @JsonProperty("PropertyType")
    private String propertyType; // Residential, Commercial, Land, etc.
    
    @JsonProperty("PropertySubType")
    private String propertySubType; // Single Family, Condo, Townhouse, etc.
    
    @JsonProperty("PropertySubTypeAdditional")
    private String propertySubTypeAdditional;
    
    // Property Features
    @JsonProperty("BedroomsTotal")
    private Integer bedroomsTotal;
    
    @JsonProperty("BathroomsTotalInteger")
    private Integer bathroomsTotalInteger;
    
    @JsonProperty("BathroomsFull")
    private Integer bathroomsFull;
    
    @JsonProperty("BathroomsHalf")
    private Integer bathroomsHalf;
    
    @JsonProperty("BathroomsPartial")
    private Integer bathroomsPartial;
    
    // Square Footage & Lot Size
    @JsonProperty("LivingArea")
    private BigDecimal livingArea;
    
    @JsonProperty("LivingAreaUnits")
    private String livingAreaUnits; // "Square Feet", "Square Meters"
    
    @JsonProperty("LotSizeSquareFeet")
    private BigDecimal lotSizeSquareFeet;
    
    @JsonProperty("LotSizeAcres")
    private BigDecimal lotSizeAcres;
    
    @JsonProperty("LotSizeArea")
    private BigDecimal lotSizeArea;
    
    @JsonProperty("LotSizeUnits")
    private String lotSizeUnits;
    
    // Address Information
    @JsonProperty("UnparsedAddress")
    private String unparsedAddress;
    
    @JsonProperty("StreetNumber")
    private String streetNumber;
    
    @JsonProperty("StreetDirPrefix")
    private String streetDirPrefix;
    
    @JsonProperty("StreetName")
    private String streetName;
    
    @JsonProperty("StreetSuffix")
    private String streetSuffix;
    
    @JsonProperty("StreetDirSuffix")
    private String streetDirSuffix;
    
    @JsonProperty("UnitNumber")
    private String unitNumber;
    
    @JsonProperty("City")
    private String city;
    
    @JsonProperty("StateOrProvince")
    private String stateOrProvince;
    
    @JsonProperty("PostalCode")
    private String postalCode;
    
    @JsonProperty("PostalCodePlus4")
    private String postalCodePlus4;
    
    @JsonProperty("Country")
    private String country;
    
    // Geographic Information
    @JsonProperty("Latitude")
    private BigDecimal latitude;
    
    @JsonProperty("Longitude")
    private BigDecimal longitude;
    
    @JsonProperty("Directions")
    private String directions;
    
    // Listing Information
    @JsonProperty("StandardStatus")
    private String standardStatus; // Active, Pending, Closed, Expired, etc.
    
    @JsonProperty("MlsStatus")
    private String mlsStatus;
    
    @JsonProperty("ListingContractDate")
    private LocalDateTime listingContractDate;
    
    @JsonProperty("OnMarketDate")
    private LocalDateTime onMarketDate;
    
    @JsonProperty("OffMarketDate")
    private LocalDateTime offMarketDate;
    
    @JsonProperty("ExpirationDate")
    private LocalDateTime expirationDate;
    
    @JsonProperty("DaysOnMarket")
    private Integer daysOnMarket;
    
    @JsonProperty("CumulativeDaysOnMarket")
    private Integer cumulativeDaysOnMarket;
    
    // Agent & Office Information
    @JsonProperty("ListAgentKey")
    private String listAgentKey;
    
    @JsonProperty("ListAgentMlsId")
    private String listAgentMlsId;
    
    @JsonProperty("ListAgentFirstName")
    private String listAgentFirstName;
    
    @JsonProperty("ListAgentLastName")
    private String listAgentLastName;
    
    @JsonProperty("ListAgentFullName")
    private String listAgentFullName;
    
    @JsonProperty("ListAgentEmail")
    private String listAgentEmail;
    
    @JsonProperty("ListAgentDirectPhone")
    private String listAgentDirectPhone;
    
    @JsonProperty("ListOfficeKey")
    private String listOfficeKey;
    
    @JsonProperty("ListOfficeMlsId")
    private String listOfficeMlsId;
    
    @JsonProperty("ListOfficeName")
    private String listOfficeName;
    
    @JsonProperty("ListOfficePhone")
    private String listOfficePhone;
    
    // Selling Agent & Office (when sold)
    @JsonProperty("BuyerAgentKey")
    private String buyerAgentKey;
    
    @JsonProperty("BuyerAgentMlsId")
    private String buyerAgentMlsId;
    
    @JsonProperty("BuyerAgentFullName")
    private String buyerAgentFullName;
    
    @JsonProperty("BuyerOfficeKey")
    private String buyerOfficeKey;
    
    @JsonProperty("BuyerOfficeName")
    private String buyerOfficeName;
    
    // Property Description
    @JsonProperty("PublicRemarks")
    private String publicRemarks;
    
    @JsonProperty("PrivateRemarks")
    private String privateRemarks;
    
    @JsonProperty("Directions")
    private String directionsToProperty;
    
    // Property Details
    @JsonProperty("YearBuilt")
    private Integer yearBuilt;
    
    @JsonProperty("PropertyAge")
    private Integer propertyAge;
    
    @JsonProperty("ArchitecturalStyle")
    private String architecturalStyle;
    
    @JsonProperty("BuildingFeatures")
    private String buildingFeatures;
    
    @JsonProperty("Cooling")
    private String cooling;
    
    @JsonProperty("Heating")
    private String heating;
    
    @JsonProperty("ParkingFeatures")
    private String parkingFeatures;
    
    @JsonProperty("ParkingTotal")
    private Integer parkingTotal;
    
    @JsonProperty("GarageSpaces")
    private Integer garageSpaces;
    
    // Financial Information
    @JsonProperty("TaxAnnualAmount")
    private BigDecimal taxAnnualAmount;
    
    @JsonProperty("TaxYear")
    private Integer taxYear;
    
    @JsonProperty("AssociationFee")
    private BigDecimal associationFee;
    
    @JsonProperty("AssociationFeeFrequency")
    private String associationFeeFrequency;
    
    // School Information
    @JsonProperty("ElementarySchool")
    private String elementarySchool;
    
    @JsonProperty("MiddleOrJuniorSchool")
    private String middleOrJuniorSchool;
    
    @JsonProperty("HighSchool")
    private String highSchool;
    
    @JsonProperty("SchoolDistrict")
    private String schoolDistrict;
    
    // Utilities
    @JsonProperty("Electric")
    private String electric;
    
    @JsonProperty("Gas")
    private String gas;
    
    @JsonProperty("Water")
    private String water;
    
    @JsonProperty("Sewer")
    private String sewer;
    
    // Timestamp fields
    @JsonProperty("ModificationTimestamp")
    private LocalDateTime modificationTimestamp;
    
    @JsonProperty("PhotosCount")
    private Integer photosCount;
    
    @JsonProperty("PhotosChangeTimestamp")
    private LocalDateTime photosChangeTimestamp;
    
    @JsonProperty("VideosCount")
    private Integer videosCount;
    
    @JsonProperty("VirtualTourURLUnbranded")
    private String virtualTourUrl;
    
    // Additional RESO fields can be added as needed
    // Based on specific MLS requirements and RESO Data Dictionary updates
    
    // Showing Information
    @JsonProperty("ShowingContactType")
    private String showingContactType;
    
    @JsonProperty("ShowingContactName")
    private String showingContactName;
    
    @JsonProperty("ShowingContactPhone")
    private String showingContactPhone;
    
    @JsonProperty("ShowingInstructions")
    private String showingInstructions;
    
    // Commission Information (if allowed in feed)
    @JsonProperty("BuyerAgencyCompensation")
    private String buyerAgencyCompensation;
    
    @JsonProperty("BuyerAgencyCompensationType")
    private String buyerAgencyCompensationType;
    
    @JsonProperty("SubAgencyCompensation")
    private String subAgencyCompensation;
    
    @JsonProperty("SubAgencyCompensationType")
    private String subAgencyCompensationType;
    
    // MLS-specific fields
    @JsonProperty("MLSAreaMajor")
    private String mlsAreaMajor;
    
    @JsonProperty("CountyOrParish")
    private String countyOrParish;
    
    @JsonProperty("Township")
    private String township;
    
    @JsonProperty("Subdivision")
    private String subdivision;
}
