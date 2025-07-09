package com.proptech.realestate.model.dto.reso;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;
import java.time.LocalDate;

/**
 * RESO Office Resource - Compliant with RESO Data Dictionary 2.0
 * Represents real estate office/brokerage information
 */
@Data
@Builder
public class OfficeResource {
    
    // Core Office Information
    @JsonProperty("OfficeKey")
    private String officeKey; // UUID as string
    
    @JsonProperty("OfficeMlsId")
    private String officeMlsId; // MLS Office ID
    
    @JsonProperty("OfficeNationalAssociationId")
    private String officeNationalAssociationId;
    
    @JsonProperty("OfficeStateLicense")
    private String officeStateLicense;
    
    @JsonProperty("OfficeStateLicenseState")
    private String officeStateLicenseState;
    
    // Office Identity
    @JsonProperty("OfficeName")
    private String officeName;
    
    @JsonProperty("OfficeAKA")
    private String officeAKA; // Also Known As
    
    @JsonProperty("OfficeBranchType")
    private String officeBranchType; // Main, Branch, etc.
    
    @JsonProperty("OfficeType")
    private String officeType; // Brokerage, Franchise, etc.
    
    @JsonProperty("OfficeStatus")
    private String officeStatus; // Active, Inactive, etc.
    
    // Contact Information
    @JsonProperty("OfficeEmail")
    private String officeEmail;
    
    @JsonProperty("OfficePhone")
    private String officePhone;
    
    @JsonProperty("OfficePhoneExt")
    private String officePhoneExt;
    
    @JsonProperty("OfficeFax")
    private String officeFax;
    
    @JsonProperty("OfficeTollFreePhone")
    private String officeTollFreePhone;
    
    // Address Information
    @JsonProperty("OfficeAddress1")
    private String officeAddress1;
    
    @JsonProperty("OfficeAddress2")
    private String officeAddress2;
    
    @JsonProperty("OfficeCity")
    private String officeCity;
    
    @JsonProperty("OfficeStateOrProvince")
    private String officeStateOrProvince;
    
    @JsonProperty("OfficePostalCode")
    private String officePostalCode;
    
    @JsonProperty("OfficePostalCodePlus4")
    private String officePostalCodePlus4;
    
    @JsonProperty("OfficeCountry")
    private String officeCountry;
    
    @JsonProperty("OfficeCountyOrParish")
    private String officeCountyOrParish;
    
    // Geographic Information
    @JsonProperty("Latitude")
    private String latitude;
    
    @JsonProperty("Longitude")
    private String longitude;
    
    @JsonProperty("Directions")
    private String directions;
    
    // Corporate Information
    @JsonProperty("CorporateLicense")
    private String corporateLicense;
    
    @JsonProperty("MainOfficeKey")
    private String mainOfficeKey; // For branch offices
    
    @JsonProperty("MainOfficeMlsId")
    private String mainOfficeMlsId;
    
    @JsonProperty("ParentOfficeKey")
    private String parentOfficeKey;
    
    @JsonProperty("FranchiseAffiliation")
    private String franchiseAffiliation;
    
    // Web & Social Media
    @JsonProperty("OfficeURL")
    private String officeURL;
    
    @JsonProperty("SocialMediaType")
    private String socialMediaType;
    
    @JsonProperty("SocialMediaUrlOrId")
    private String socialMediaUrlOrId;
    
    // Association Information
    @JsonProperty("OfficeAOR")
    private String officeAOR; // Association of Realtors
    
    @JsonProperty("OfficeAORkey")
    private String officeAORkey;
    
    @JsonProperty("OfficeAORMlsId")
    private String officeAORMlsId;
    
    @JsonProperty("OfficeNrdsId")
    private String officeNrdsId;
    
    // Business Details
    @JsonProperty("OfficeManager")
    private String officeManager;
    
    @JsonProperty("OfficeManagerKey")
    private String officeManagerKey;
    
    @JsonProperty("OfficeManagerMlsId")
    private String officeManagerMlsId;
    
    @JsonProperty("OfficeAssistantName")
    private String officeAssistantName;
    
    @JsonProperty("OfficeAssistantPhone")
    private String officeAssistantPhone;
    
    @JsonProperty("OfficeAssistantPhoneExt")
    private String officeAssistantPhoneExt;
    
    @JsonProperty("OfficeAssistantEmail")
    private String officeAssistantEmail;
    
    // License Information
    @JsonProperty("OfficeStateLicenseExpirationDate")
    private LocalDate officeStateLicenseExpirationDate;
    
    // Operational Information
    @JsonProperty("OfficeHours")
    private String officeHours;
    
    @JsonProperty("OfficeTimeZone")
    private String officeTimeZone;
    
    @JsonProperty("OfficeDaysOfOperation")
    private String officeDaysOfOperation;
    
    @JsonProperty("OfficeLanguages")
    private String officeLanguages;
    
    // Marketing Information
    @JsonProperty("OfficeSlogan")
    private String officeSlogan;
    
    @JsonProperty("OfficeSpecialty")
    private String officeSpecialty;
    
    @JsonProperty("OfficeDescription")
    private String officeDescription;
    
    @JsonProperty("OfficeComments")
    private String officeComments;
    
    // Statistics
    @JsonProperty("OfficeAgentCount")
    private Integer officeAgentCount;
    
    @JsonProperty("OfficeActiveMemberCount")
    private Integer officeActiveMemberCount;
    
    @JsonProperty("OfficeInactiveMemberCount")
    private Integer officeInactiveMemberCount;
    
    // Dates
    @JsonProperty("OfficeEstablishedDate")
    private LocalDate officeEstablishedDate;
    
    @JsonProperty("OriginalEntryTimestamp")
    private LocalDateTime originalEntryTimestamp;
    
    @JsonProperty("ModificationTimestamp")
    private LocalDateTime modificationTimestamp;
    
    // Photo Information
    @JsonProperty("PhotosCount")
    private Integer photosCount;
    
    @JsonProperty("PhotosChangeTimestamp")
    private LocalDateTime photosChangeTimestamp;
    
    // Additional Business Information
    @JsonProperty("OfficeBrokerKey")
    private String officeBrokerKey;
    
    @JsonProperty("OfficeBrokerMlsId")
    private String officeBrokerMlsId;
    
    @JsonProperty("OfficeBrokerFirstName")
    private String officeBrokerFirstName;
    
    @JsonProperty("OfficeBrokerLastName")
    private String officeBrokerLastName;
    
    @JsonProperty("OfficeBrokerFullName")
    private String officeBrokerFullName;
    
    // MLS Access & Permissions
    @JsonProperty("OfficeMlsAccessYN")
    private Boolean officeMlsAccessYN;
    
    @JsonProperty("OfficeIdxYN")
    private Boolean officeIdxYN; // IDX participation
    
    @JsonProperty("OfficeVowYN")
    private Boolean officeVowYN; // VOW participation
    
    @JsonProperty("OfficeListingsDisplayed")
    private Boolean officeListingsDisplayed;
    
    // Service Areas
    @JsonProperty("OfficeServiceAreas")
    private String officeServiceAreas;
    
    @JsonProperty("OfficeZipCodes")
    private String officeZipCodes;
    
    @JsonProperty("OfficeCityList")
    private String officeCityList;
    
    @JsonProperty("OfficeCountyList")
    private String officeCountyList;
    
    // Technology & Systems
    @JsonProperty("OfficeSystemId")
    private String officeSystemId;
    
    @JsonProperty("OfficeSystemType")
    private String officeSystemType;
    
    @JsonProperty("OfficeEmailDomain")
    private String officeEmailDomain;
    
    // Financial Information (if applicable)
    @JsonProperty("OfficeAnnualRevenue")
    private String officeAnnualRevenue;
    
    @JsonProperty("OfficeTransactionVolume")
    private String officeTransactionVolume;
    
    // Certifications & Awards
    @JsonProperty("OfficeCertifications")
    private String officeCertifications;
    
    @JsonProperty("OfficeAwards")
    private String officeAwards;
    
    @JsonProperty("OfficeAccreditations")
    private String officeAccreditations;
    
    // Communication Preferences
    @JsonProperty("OfficePreferredCommunication")
    private String officePreferredCommunication;
    
    @JsonProperty("OfficeEmailOptIn")
    private Boolean officeEmailOptIn;
    
    @JsonProperty("OfficeSmsOptIn")
    private Boolean officeSmsOptIn;
    
    @JsonProperty("OfficeCallOptIn")
    private Boolean officeCallOptIn;
    
    @JsonProperty("OfficeMailOptIn")
    private Boolean officeMailOptIn;
    
    // Additional Metadata
    @JsonProperty("OfficeCarrierRoute")
    private String officeCarrierRoute;
    
    @JsonProperty("OfficeCensusTract")
    private String officeCensusTract;
    
    @JsonProperty("OfficeGeocodingAccuracy")
    private String officeGeocodingAccuracy;
    
    @JsonProperty("OfficeParcelNumber")
    private String officeParcelNumber;
}
