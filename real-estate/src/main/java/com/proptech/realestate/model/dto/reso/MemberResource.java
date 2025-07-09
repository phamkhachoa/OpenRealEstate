package com.proptech.realestate.model.dto.reso;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;
import java.time.LocalDate;

/**
 * RESO Member Resource - Compliant with RESO Data Dictionary 2.0
 * Represents MLS member information
 */
@Data
@Builder
public class MemberResource {
    
    // Core Member Information
    @JsonProperty("MemberKey")
    private String memberKey; // UUID as string
    
    @JsonProperty("MemberMlsId")
    private String memberMlsId; // MLS ID Number
    
    @JsonProperty("MemberNationalAssociationId")
    private String memberNationalAssociationId; // NAR ID
    
    @JsonProperty("MemberStateLicense")
    private String memberStateLicense;
    
    @JsonProperty("MemberStateLicenseState")
    private String memberStateLicenseState;
    
    @JsonProperty("MemberDesignation")
    private String memberDesignation; // CRS, GRI, ABR, etc.
    
    // Personal Information
    @JsonProperty("MemberFirstName")
    private String memberFirstName;
    
    @JsonProperty("MemberMiddleName")
    private String memberMiddleName;
    
    @JsonProperty("MemberLastName")
    private String memberLastName;
    
    @JsonProperty("MemberFullName")
    private String memberFullName;
    
    @JsonProperty("MemberNickname")
    private String memberNickname;
    
    @JsonProperty("MemberNameSuffix")
    private String memberNameSuffix; // Jr., Sr., III, etc.
    
    // Contact Information
    @JsonProperty("MemberEmail")
    private String memberEmail;
    
    @JsonProperty("MemberPreferredPhone")
    private String memberPreferredPhone;
    
    @JsonProperty("MemberDirectPhone")
    private String memberDirectPhone;
    
    @JsonProperty("MemberHomePhone")
    private String memberHomePhone;
    
    @JsonProperty("MemberMobilePhone")
    private String memberMobilePhone;
    
    @JsonProperty("MemberOfficePhone")
    private String memberOfficePhone;
    
    @JsonProperty("MemberOfficePhoneExt")
    private String memberOfficePhoneExt;
    
    @JsonProperty("MemberFax")
    private String memberFax;
    
    @JsonProperty("MemberTollFreePhone")
    private String memberTollFreePhone;
    
    @JsonProperty("MemberVoiceMail")
    private String memberVoiceMail;
    
    @JsonProperty("MemberVoiceMailExt")
    private String memberVoiceMailExt;
    
    // Professional Information
    @JsonProperty("MemberType")
    private String memberType; // Agent, Broker, Office Manager, etc.
    
    @JsonProperty("MemberStatus")
    private String memberStatus; // Active, Inactive, Suspended, etc.
    
    @JsonProperty("MemberAOR")
    private String memberAOR; // Association of Realtors
    
    @JsonProperty("MemberAORMlsId")
    private String memberAORMlsId;
    
    @JsonProperty("MemberAORkey")
    private String memberAORkey;
    
    // Office Association
    @JsonProperty("OfficeKey")
    private String officeKey;
    
    @JsonProperty("OfficeMlsId")
    private String officeMlsId;
    
    @JsonProperty("OfficeName")
    private String officeName;
    
    // Address Information
    @JsonProperty("MemberAddress1")
    private String memberAddress1;
    
    @JsonProperty("MemberAddress2")
    private String memberAddress2;
    
    @JsonProperty("MemberCity")
    private String memberCity;
    
    @JsonProperty("MemberStateOrProvince")
    private String memberStateOrProvince;
    
    @JsonProperty("MemberPostalCode")
    private String memberPostalCode;
    
    @JsonProperty("MemberPostalCodePlus4")
    private String memberPostalCodePlus4;
    
    @JsonProperty("MemberCountry")
    private String memberCountry;
    
    @JsonProperty("MemberCarrierRoute")
    private String memberCarrierRoute;
    
    // Professional Details
    @JsonProperty("MemberLanguages")
    private String memberLanguages;
    
    @JsonProperty("MemberAssociationComments")
    private String memberAssociationComments;
    
    @JsonProperty("MemberPersonalComments")
    private String memberPersonalComments;
    
    @JsonProperty("MemberPreferredPhoneExt")
    private String memberPreferredPhoneExt;
    
    @JsonProperty("MemberAssistantName")
    private String memberAssistantName;
    
    @JsonProperty("MemberAssistantPhone")
    private String memberAssistantPhone;
    
    @JsonProperty("MemberAssistantPhoneExt")
    private String memberAssistantPhoneExt;
    
    // Social Media & Web Presence
    @JsonProperty("SocialMediaType")
    private String socialMediaType;
    
    @JsonProperty("SocialMediaUrlOrId")
    private String socialMediaUrlOrId;
    
    @JsonProperty("MemberURL")
    private String memberURL;
    
    @JsonProperty("MemberVideoURL")
    private String memberVideoURL;
    
    // Professional Specialties
    @JsonProperty("MemberSpecialty")
    private String memberSpecialty;
    
    @JsonProperty("MemberOtherPhone")
    private String memberOtherPhone;
    
    @JsonProperty("MemberOtherPhoneExt")
    private String memberOtherPhoneExt;
    
    // License Information
    @JsonProperty("MemberStateLicenseExpirationDate")
    private LocalDate memberStateLicenseExpirationDate;
    
    @JsonProperty("MemberNrdsId")
    private String memberNrdsId; // National REALTOR Database System ID
    
    // Membership Dates
    @JsonProperty("OriginalEntryTimestamp")
    private LocalDateTime originalEntryTimestamp;
    
    @JsonProperty("ModificationTimestamp")
    private LocalDateTime modificationTimestamp;
    
    @JsonProperty("MemberMlsAccessYN")
    private Boolean memberMlsAccessYN;
    
    @JsonProperty("MemberStatus")
    private String membershipStatus; // Active, Inactive, Suspended
    
    // Photo Information
    @JsonProperty("PhotosCount")
    private Integer photosCount;
    
    @JsonProperty("PhotosChangeTimestamp")
    private LocalDateTime photosChangeTimestamp;
    
    // Additional Information
    @JsonProperty("MemberLoginId")
    private String memberLoginId;
    
    @JsonProperty("MemberPassword")
    private String memberPassword; // Usually not exposed in feeds
    
    @JsonProperty("MemberPasswordExpiration")
    private LocalDateTime memberPasswordExpiration;
    
    @JsonProperty("MemberLastLoginTimestamp")
    private LocalDateTime memberLastLoginTimestamp;
    
    @JsonProperty("MemberTotalLogins")
    private Integer memberTotalLogins;
    
    // Business Information
    @JsonProperty("MemberGenerationOptInYN")
    private Boolean memberGenerationOptInYN;
    
    @JsonProperty("MemberMlsSecurityClass")
    private String memberMlsSecurityClass;
    
    @JsonProperty("MemberSystemRole")
    private String memberSystemRole;
    
    // Geographic Information
    @JsonProperty("MemberCountyOrParish")
    private String memberCountyOrParish;
    
    @JsonProperty("MemberDirections")
    private String memberDirections;
    
    // Additional Professional Information
    @JsonProperty("MemberIsRealtor")
    private Boolean memberIsRealtor;
    
    @JsonProperty("MemberRealtorAssociationKey")
    private String memberRealtorAssociationKey;
    
    @JsonProperty("MemberYearsOfExperience")
    private Integer memberYearsOfExperience;
    
    @JsonProperty("MemberCertifications")
    private String memberCertifications;
    
    @JsonProperty("MemberEducation")
    private String memberEducation;
    
    @JsonProperty("MemberAwards")
    private String memberAwards;
    
    @JsonProperty("MemberBio")
    private String memberBio;
    
    @JsonProperty("MemberSlogan")
    private String memberSlogan;
    
    // Communication Preferences
    @JsonProperty("MemberPreferredCommunication")
    private String memberPreferredCommunication;
    
    @JsonProperty("MemberEmailOptIn")
    private Boolean memberEmailOptIn;
    
    @JsonProperty("MemberSmsOptIn")
    private Boolean memberSmsOptIn;
    
    @JsonProperty("MemberCallOptIn")
    private Boolean memberCallOptIn;
    
    @JsonProperty("MemberMailOptIn")
    private Boolean memberMailOptIn;
    
    // Marketing Information
    @JsonProperty("MemberMarketingBudget")
    private String memberMarketingBudget;
    
    @JsonProperty("MemberMarketingArea")
    private String memberMarketingArea;
    
    @JsonProperty("MemberTargetMarket")
    private String memberTargetMarket;
}
