package com.proptech.realestate.service;

import com.proptech.realestate.model.dto.reso.PropertyResource;
import com.proptech.realestate.model.dto.reso.MemberResource;
import com.proptech.realestate.model.dto.reso.OfficeResource;
import com.proptech.realestate.model.entity.Listing;
import com.proptech.realestate.model.entity.MLSMembership;
import com.proptech.realestate.model.entity.Office;
import com.proptech.realestate.repository.ListingRepository;
import com.proptech.realestate.repository.MLSMembershipRepository;
import com.proptech.realestate.repository.OfficeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service implementation for RESO Web API operations
 * Handles business logic for RESO-compliant data access
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ResoApiService {
    
    private final ListingRepository listingRepository;
    private final MLSMembershipRepository membershipRepository;
    private final OfficeRepository officeRepository;
    private final ResoMappingService resoMappingService;
    private final ODataQueryParser oDataQueryParser;
    
    /**
     * Get properties with OData query support
     */
    public Page<PropertyResource> getProperties(ODataQueryParser.ODataQuery oDataQuery, String lastModified) {
        try {
            // Build JPA Specification from OData filter
            Specification<Listing> spec = buildPropertySpecification(oDataQuery, lastModified);
            
            // Convert OData query to Spring Data Pageable
            Pageable pageable = oDataQuery.toPageable();
            
            // Execute query
            Page<Listing> listingPage = listingRepository.findAll(spec, pageable);
            
            // Map to RESO PropertyResource
            List<PropertyResource> propertyResources = resoMappingService.mapToPropertyResources(listingPage.getContent());
            
            return new PageImpl<>(propertyResources, pageable, listingPage.getTotalElements());
            
        } catch (Exception e) {
            log.error("Error retrieving properties with OData query", e);
            throw new RuntimeException("Failed to retrieve properties", e);
        }
    }
    
    /**
     * Get specific property by key
     */
    public PropertyResource getPropertyByKey(String key, String select, String expand) {
        try {
            UUID listingId = UUID.fromString(key);
            
            Listing listing = listingRepository.findById(listingId)
                .orElse(null);
                
            if (listing == null) {
                return null;
            }
            
            PropertyResource propertyResource = resoMappingService.mapToPropertyResource(listing);
            
            // Apply $select and $expand if specified
            return applySelectAndExpand(propertyResource, select, expand);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid property key format: {}", key);
            return null;
        } catch (Exception e) {
            log.error("Error retrieving property by key: {}", key, e);
            throw new RuntimeException("Failed to retrieve property", e);
        }
    }
    
    /**
     * Get members with OData query support
     */
    public Page<MemberResource> getMembers(ODataQueryParser.ODataQuery oDataQuery) {
        try {
            // Build specification for member queries
            Specification<MLSMembership> spec = buildMemberSpecification(oDataQuery);
            
            Pageable pageable = oDataQuery.toPageable();
            
            // Execute query
            Page<MLSMembership> membershipPage = membershipRepository.findAll(spec, pageable);
            
            // Map to RESO MemberResource
            List<MemberResource> memberResources = resoMappingService.mapToMemberResources(membershipPage.getContent());
            
            return new PageImpl<>(memberResources, pageable, membershipPage.getTotalElements());
            
        } catch (Exception e) {
            log.error("Error retrieving members with OData query", e);
            throw new RuntimeException("Failed to retrieve members", e);
        }
    }
    
    /**
     * Get offices with OData query support
     */
    public Page<OfficeResource> getOffices(ODataQueryParser.ODataQuery oDataQuery) {
        try {
            // Build specification for office queries
            Specification<Office> spec = buildOfficeSpecification(oDataQuery);
            
            Pageable pageable = oDataQuery.toPageable();
            
            // Execute query
            Page<Office> officePage = officeRepository.findAll(spec, pageable);
            
            // Map to RESO OfficeResource
            List<OfficeResource> officeResources = resoMappingService.mapToOfficeResources(officePage.getContent());
            
            return new PageImpl<>(officeResources, pageable, officePage.getTotalElements());
            
        } catch (Exception e) {
            log.error("Error retrieving offices with OData query", e);
            throw new RuntimeException("Failed to retrieve offices", e);
        }
    }
    
    /**
     * Generate RESO metadata document
     */
    public String getMetadataDocument() {
        // Generate EDMX metadata document for RESO resources
        // This would typically be generated from RESO Data Dictionary
        
        StringBuilder metadata = new StringBuilder();
        metadata.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        metadata.append("<edmx:Edmx xmlns:edmx=\"http://docs.oasis-open.org/odata/ns/edmx\" Version=\"4.0\">\n");
        metadata.append("  <edmx:DataServices>\n");
        metadata.append("    <Schema xmlns=\"http://docs.oasis-open.org/odata/ns/edm\" Namespace=\"org.reso.metadata\">\n");
        
        // Property EntityType
        metadata.append("      <EntityType Name=\"Property\">\n");
        metadata.append("        <Key><PropertyRef Name=\"ListingKey\"/></Key>\n");
        metadata.append("        <Property Name=\"ListingKey\" Type=\"Edm.String\" Nullable=\"false\"/>\n");
        metadata.append("        <Property Name=\"ListingId\" Type=\"Edm.String\"/>\n");
        metadata.append("        <Property Name=\"ListPrice\" Type=\"Edm.Decimal\"/>\n");
        metadata.append("        <Property Name=\"BedroomsTotal\" Type=\"Edm.Int32\"/>\n");
        metadata.append("        <Property Name=\"BathroomsTotalInteger\" Type=\"Edm.Int32\"/>\n");
        metadata.append("        <Property Name=\"LivingArea\" Type=\"Edm.Decimal\"/>\n");
        metadata.append("        <Property Name=\"City\" Type=\"Edm.String\"/>\n");
        metadata.append("        <Property Name=\"StateOrProvince\" Type=\"Edm.String\"/>\n");
        metadata.append("        <Property Name=\"PostalCode\" Type=\"Edm.String\"/>\n");
        metadata.append("        <Property Name=\"StandardStatus\" Type=\"Edm.String\"/>\n");
        metadata.append("        <Property Name=\"PublicRemarks\" Type=\"Edm.String\"/>\n");
        metadata.append("        <Property Name=\"ModificationTimestamp\" Type=\"Edm.DateTimeOffset\"/>\n");
        metadata.append("      </EntityType>\n");
        
        // Member EntityType
        metadata.append("      <EntityType Name=\"Member\">\n");
        metadata.append("        <Key><PropertyRef Name=\"MemberKey\"/></Key>\n");
        metadata.append("        <Property Name=\"MemberKey\" Type=\"Edm.String\" Nullable=\"false\"/>\n");
        metadata.append("        <Property Name=\"MemberMlsId\" Type=\"Edm.String\"/>\n");
        metadata.append("        <Property Name=\"MemberFirstName\" Type=\"Edm.String\"/>\n");
        metadata.append("        <Property Name=\"MemberLastName\" Type=\"Edm.String\"/>\n");
        metadata.append("        <Property Name=\"MemberEmail\" Type=\"Edm.String\"/>\n");
        metadata.append("        <Property Name=\"MemberDirectPhone\" Type=\"Edm.String\"/>\n");
        metadata.append("        <Property Name=\"MemberType\" Type=\"Edm.String\"/>\n");
        metadata.append("        <Property Name=\"MemberStatus\" Type=\"Edm.String\"/>\n");
        metadata.append("        <Property Name=\"ModificationTimestamp\" Type=\"Edm.DateTimeOffset\"/>\n");
        metadata.append("      </EntityType>\n");
        
        // Office EntityType
        metadata.append("      <EntityType Name=\"Office\">\n");
        metadata.append("        <Key><PropertyRef Name=\"OfficeKey\"/></Key>\n");
        metadata.append("        <Property Name=\"OfficeKey\" Type=\"Edm.String\" Nullable=\"false\"/>\n");
        metadata.append("        <Property Name=\"OfficeMlsId\" Type=\"Edm.String\"/>\n");
        metadata.append("        <Property Name=\"OfficeName\" Type=\"Edm.String\"/>\n");
        metadata.append("        <Property Name=\"OfficePhone\" Type=\"Edm.String\"/>\n");
        metadata.append("        <Property Name=\"OfficeEmail\" Type=\"Edm.String\"/>\n");
        metadata.append("        <Property Name=\"OfficeCity\" Type=\"Edm.String\"/>\n");
        metadata.append("        <Property Name=\"OfficeStateOrProvince\" Type=\"Edm.String\"/>\n");
        metadata.append("        <Property Name=\"ModificationTimestamp\" Type=\"Edm.DateTimeOffset\"/>\n");
        metadata.append("      </EntityType>\n");
        
        // EntityContainer
        metadata.append("      <EntityContainer Name=\"Container\">\n");
        metadata.append("        <EntitySet Name=\"Property\" EntityType=\"org.reso.metadata.Property\"/>\n");
        metadata.append("        <EntitySet Name=\"Member\" EntityType=\"org.reso.metadata.Member\"/>\n");
        metadata.append("        <EntitySet Name=\"Office\" EntityType=\"org.reso.metadata.Office\"/>\n");
        metadata.append("      </EntityContainer>\n");
        
        metadata.append("    </Schema>\n");
        metadata.append("  </edmx:DataServices>\n");
        metadata.append("</edmx:Edmx>\n");
        
        return metadata.toString();
    }
    
    // Private helper methods
    
    /**
     * Build JPA Specification from OData filter for Property queries
     */
    private Specification<Listing> buildPropertySpecification(ODataQueryParser.ODataQuery oDataQuery, String lastModified) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Apply OData filter
            if (oDataQuery.getFilter() != null) {
                Predicate filterPredicate = buildPropertyFilterPredicate(oDataQuery.getFilter(), root, criteriaBuilder);
                if (filterPredicate != null) {
                    predicates.add(filterPredicate);
                }
            }
            
            // Apply last modified filter
            if (lastModified != null && !lastModified.trim().isEmpty()) {
                try {
                    LocalDateTime lastModDateTime = LocalDateTime.parse(lastModified, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    predicates.add(criteriaBuilder.greaterThan(root.get("updatedAt"), lastModDateTime));
                } catch (Exception e) {
                    log.warn("Invalid lastModified timestamp format: {}", lastModified);
                }
            }
            
            // Only return active listings by default
            predicates.add(criteriaBuilder.notEqual(root.get("status"), "DELETED"));
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
    
    /**
     * Build JPA Specification from OData filter for Member queries
     */
    private Specification<MLSMembership> buildMemberSpecification(ODataQueryParser.ODataQuery oDataQuery) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Apply OData filter
            if (oDataQuery.getFilter() != null) {
                Predicate filterPredicate = buildMemberFilterPredicate(oDataQuery.getFilter(), root, criteriaBuilder);
                if (filterPredicate != null) {
                    predicates.add(filterPredicate);
                }
            }
            
            // Only return active memberships by default
            predicates.add(criteriaBuilder.equal(root.get("status"), "ACTIVE"));
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
    
    /**
     * Build JPA Specification from OData filter for Office queries
     */
    private Specification<Office> buildOfficeSpecification(ODataQueryParser.ODataQuery oDataQuery) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Apply OData filter
            if (oDataQuery.getFilter() != null) {
                Predicate filterPredicate = buildOfficeFilterPredicate(oDataQuery.getFilter(), root, criteriaBuilder);
                if (filterPredicate != null) {
                    predicates.add(filterPredicate);
                }
            }
            
            // Only return active offices by default
            predicates.add(criteriaBuilder.equal(root.get("isActive"), true));
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
    
    /**
     * Build filter predicate for Property queries
     */
    private Predicate buildPropertyFilterPredicate(ODataQueryParser.FilterExpression filter, Root<Listing> root, CriteriaBuilder cb) {
        if (filter == null) {
            return null;
        }
        
        // Handle logical expressions (AND, OR)
        if (filter.isLogicalExpression()) {
            Predicate leftPredicate = buildPropertyFilterPredicate(filter.getLeft(), root, cb);
            Predicate rightPredicate = buildPropertyFilterPredicate(filter.getRight(), root, cb);
            
            if (leftPredicate == null || rightPredicate == null) {
                return leftPredicate != null ? leftPredicate : rightPredicate;
            }
            
            return "AND".equals(filter.getLogicalOperator()) ?
                cb.and(leftPredicate, rightPredicate) :
                cb.or(leftPredicate, rightPredicate);
        }
        
        // Handle function expressions
        if (filter.isFunctionExpression()) {
            return buildPropertyFunctionPredicate(filter, root, cb);
        }
        
        // Handle simple comparison expressions
        String field = oDataQueryParser.mapResoFieldToInternal(filter.getField());
        Path<Object> fieldPath = getFieldPath(root, field);
        Object value = filter.getValue();
        
        if (fieldPath == null || value == null) {
            return null;
        }
        
        return switch (filter.getOperator()) {
            case "=" -> cb.equal(fieldPath, value);
            case "!=" -> cb.notEqual(fieldPath, value);
            case ">" -> cb.greaterThan((Path<Comparable>) fieldPath, (Comparable) value);
            case ">=" -> cb.greaterThanOrEqualTo((Path<Comparable>) fieldPath, (Comparable) value);
            case "<" -> cb.lessThan((Path<Comparable>) fieldPath, (Comparable) value);
            case "<=" -> cb.lessThanOrEqualTo((Path<Comparable>) fieldPath, (Comparable) value);
            default -> null;
        };
    }
    
    /**
     * Build function predicate for Property queries (contains, startswith, etc.)
     */
    private Predicate buildPropertyFunctionPredicate(ODataQueryParser.FilterExpression filter, Root<Listing> root, CriteriaBuilder cb) {
        String function = filter.getFunction().toLowerCase();
        List<Object> args = filter.getFunctionArgs();
        
        if (args == null || args.isEmpty()) {
            return null;
        }
        
        String field = oDataQueryParser.mapResoFieldToInternal(args.get(0).toString());
        Path<String> fieldPath = root.get(field);
        
        return switch (function) {
            case "contains" -> {
                if (args.size() >= 2) {
                    yield cb.like(cb.lower(fieldPath), "%" + args.get(1).toString().toLowerCase() + "%");
                }
                yield null;
            }
            case "startswith" -> {
                if (args.size() >= 2) {
                    yield cb.like(cb.lower(fieldPath), args.get(1).toString().toLowerCase() + "%");
                }
                yield null;
            }
            case "endswith" -> {
                if (args.size() >= 2) {
                    yield cb.like(cb.lower(fieldPath), "%" + args.get(1).toString().toLowerCase());
                }
                yield null;
            }
            default -> null;
        };
    }
    
    /**
     * Similar methods for Member and Office filter predicates would be implemented here
     */
    private Predicate buildMemberFilterPredicate(ODataQueryParser.FilterExpression filter, Root<MLSMembership> root, CriteriaBuilder cb) {
        // Implementation similar to buildPropertyFilterPredicate but for MLSMembership entity
        // This would map RESO Member fields to internal MLSMembership fields
        return null; // Placeholder
    }
    
    private Predicate buildOfficeFilterPredicate(ODataQueryParser.FilterExpression filter, Root<Office> root, CriteriaBuilder cb) {
        // Implementation similar to buildPropertyFilterPredicate but for Office entity
        // This would map RESO Office fields to internal Office fields
        return null; // Placeholder
    }
    
    /**
     * Get field path with support for nested properties
     */
    private Path<Object> getFieldPath(Root<?> root, String field) {
        try {
            if (field.contains(".")) {
                // Handle nested properties (e.g., "listingAgent.firstName")
                String[] parts = field.split("\\.");
                Path<Object> path = root.get(parts[0]);
                for (int i = 1; i < parts.length; i++) {
                    path = path.get(parts[i]);
                }
                return path;
            } else {
                // Simple property
                return root.get(field);
            }
        } catch (Exception e) {
            log.warn("Invalid field path: {}", field);
            return null;
        }
    }
    
    /**
     * Apply $select and $expand parameters to resource
     */
    private PropertyResource applySelectAndExpand(PropertyResource resource, String select, String expand) {
        // Implementation would filter fields based on $select parameter
        // and include related data based on $expand parameter
        // For now, return the resource as-is
        return resource;
    }
}
