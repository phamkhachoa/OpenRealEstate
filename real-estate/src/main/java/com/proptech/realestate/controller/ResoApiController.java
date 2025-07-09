package com.proptech.realestate.controller;

import com.proptech.realestate.model.dto.reso.PropertyResource;
import com.proptech.realestate.model.dto.reso.MemberResource;
import com.proptech.realestate.model.dto.reso.OfficeResource;
import com.proptech.realestate.service.ResoApiService;
import com.proptech.realestate.service.ODataQueryParser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * RESO Web API Controller - OData v4 Compliant
 * Implements RESO Data Dictionary 2.0 standards for MLS data access
 * 
 * Endpoints follow RESO Web API Core 2.0.0 specification:
 * - Property Resource: /Property
 * - Member Resource: /Member  
 * - Office Resource: /Office
 * - Media Resource: /Media
 */
@RestController
@RequestMapping("/reso/odata")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "RESO Web API", description = "RESO-compliant OData v4 endpoints for MLS data access")
public class ResoApiController {
    
    private final ResoApiService resoApiService;
    private final ODataQueryParser oDataQueryParser;
    
    /**
     * RESO Property Resource Endpoint
     * GET /reso/odata/Property
     * 
     * Supports all OData v4 query options:
     * - $filter: Filter properties by criteria
     * - $select: Select specific fields  
     * - $expand: Include related data
     * - $orderby: Sort results
     * - $top: Limit number of results
     * - $skip: Skip number of results for pagination
     * - $count: Include total count
     */
    @GetMapping(value = "/Property", produces = {MediaType.APPLICATION_JSON_VALUE, "application/json;odata.metadata=minimal"})
    @Operation(
        summary = "Get Property Resources", 
        description = "Retrieve property listings using RESO-compliant OData queries. Supports filtering, selection, ordering, and pagination.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved properties"),
            @ApiResponse(responseCode = "400", description = "Invalid OData query parameters"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing authentication"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
        }
    )
    @PreAuthorize("hasAuthority('MLS_API_ACCESS')")
    public ResponseEntity<Map<String, Object>> getProperties(
            @Parameter(description = "OData filter expression (e.g., 'ListPrice gt 500000 and City eq \\'Seattle\\'')")
            @RequestParam(value = "$filter", required = false) String filter,
            
            @Parameter(description = "Comma-separated list of fields to select (e.g., 'ListingKey,ListPrice,City')")
            @RequestParam(value = "$select", required = false) String select,
            
            @Parameter(description = "Related resources to expand (e.g., 'Media,ShowingInstructions')")
            @RequestParam(value = "$expand", required = false) String expand,
            
            @Parameter(description = "Order by expression (e.g., 'ListPrice desc,City asc')")
            @RequestParam(value = "$orderby", required = false) String orderby,
            
            @Parameter(description = "Maximum number of results to return (max 10000)")
            @RequestParam(value = "$top", required = false) Integer top,
            
            @Parameter(description = "Number of results to skip for pagination")
            @RequestParam(value = "$skip", required = false) Integer skip,
            
            @Parameter(description = "Response format (json, xml)")
            @RequestParam(value = "$format", required = false, defaultValue = "json") String format,
            
            @Parameter(description = "Include total count of matching records")
            @RequestParam(value = "$count", required = false, defaultValue = "false") Boolean count,
            
            @Parameter(description = "Include only results modified after this timestamp")
            @RequestParam(value = "$lastmod", required = false) String lastmod,
            
            HttpServletRequest request) {
        
        try {
            // Log the request for monitoring
            log.info("RESO Property request: filter={}, select={}, top={}, skip={}", filter, select, top, skip);
            
            // Parse OData query parameters
            ODataQueryParser.ODataQuery oDataQuery = oDataQueryParser.parseQuery(
                filter, select, expand, orderby, top, skip, format, count
            );
            
            // Apply default and maximum limits
            if (oDataQuery.getTop() == null) {
                oDataQuery.setTop(100); // Default page size
            }
            if (oDataQuery.getTop() > 10000) {
                oDataQuery.setTop(10000); // Maximum page size for performance
            }
            
            // Execute query
            Page<PropertyResource> results = resoApiService.getProperties(oDataQuery, lastmod);
            
            // Build OData response format
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("@odata.context", request.getRequestURL() + "/$metadata#Property");
            
            if (count) {
                response.put("@odata.count", results.getTotalElements());
            }
            
            // Add next link for pagination if there are more results
            if (results.hasNext()) {
                int nextSkip = (oDataQuery.getSkip() != null ? oDataQuery.getSkip() : 0) + oDataQuery.getTop();
                String nextUrl = buildNextUrl(request, nextSkip, oDataQuery.getTop());
                response.put("@odata.nextLink", nextUrl);
            }
            
            response.put("value", results.getContent());
            
            // Add custom headers
            HttpHeaders headers = new HttpHeaders();
            headers.add("OData-Version", "4.0");
            headers.add("X-RESO-Timestamp", LocalDateTime.now().toString());
            headers.add("X-Total-Count", String.valueOf(results.getTotalElements()));
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(response);
                
        } catch (IllegalArgumentException e) {
            log.warn("Invalid OData query: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(createErrorResponse("Invalid query parameters", e.getMessage()));
        } catch (Exception e) {
            log.error("Error processing RESO Property request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Internal server error", "An unexpected error occurred"));
        }
    }
    
    /**
     * RESO Property Resource by Key
     * GET /reso/odata/Property('key-value')
     */
    @GetMapping(value = "/Property('{key}')", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get Property by Key", description = "Retrieve a specific property by its unique key (UUID)")
    @PreAuthorize("hasAuthority('MLS_API_ACCESS')")
    public ResponseEntity<Map<String, Object>> getPropertyByKey(
            @PathVariable String key,
            @RequestParam(value = "$select", required = false) String select,
            @RequestParam(value = "$expand", required = false) String expand,
            HttpServletRequest request) {
        
        try {
            PropertyResource property = resoApiService.getPropertyByKey(key, select, expand);
            
            if (property == null) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("@odata.context", request.getRequestURL() + "/$metadata#Property/$entity");
            response.putAll(convertToMap(property));
            
            HttpHeaders headers = new HttpHeaders();
            headers.add("OData-Version", "4.0");
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(response);
                
        } catch (Exception e) {
            log.error("Error retrieving property by key: {}", key, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Internal server error", "Failed to retrieve property"));
        }
    }
    
    /**
     * RESO Member Resource Endpoint
     * GET /reso/odata/Member
     */
    @GetMapping(value = "/Member", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get Member Resources", description = "Retrieve MLS member information using OData queries")
    @PreAuthorize("hasAuthority('MLS_API_ACCESS') and hasAuthority('MEMBER_DATA_ACCESS')")
    public ResponseEntity<Map<String, Object>> getMembers(
            @RequestParam(value = "$filter", required = false) String filter,
            @RequestParam(value = "$select", required = false) String select,
            @RequestParam(value = "$expand", required = false) String expand,
            @RequestParam(value = "$orderby", required = false) String orderby,
            @RequestParam(value = "$top", required = false) Integer top,
            @RequestParam(value = "$skip", required = false) Integer skip,
            @RequestParam(value = "$count", required = false, defaultValue = "false") Boolean count,
            HttpServletRequest request) {
        
        try {
            ODataQueryParser.ODataQuery oDataQuery = oDataQueryParser.parseQuery(
                filter, select, expand, orderby, top, skip, "json", count
            );
            
            // Apply limits for member data
            if (oDataQuery.getTop() == null) {
                oDataQuery.setTop(50); // Smaller default for member data
            }
            if (oDataQuery.getTop() > 1000) {
                oDataQuery.setTop(1000); // Smaller max for privacy
            }
            
            Page<MemberResource> results = resoApiService.getMembers(oDataQuery);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("@odata.context", request.getRequestURL() + "/$metadata#Member");
            
            if (count) {
                response.put("@odata.count", results.getTotalElements());
            }
            
            if (results.hasNext()) {
                int nextSkip = (oDataQuery.getSkip() != null ? oDataQuery.getSkip() : 0) + oDataQuery.getTop();
                String nextUrl = buildNextUrl(request, nextSkip, oDataQuery.getTop());
                response.put("@odata.nextLink", nextUrl);
            }
            
            response.put("value", results.getContent());
            
            HttpHeaders headers = new HttpHeaders();
            headers.add("OData-Version", "4.0");
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(response);
                
        } catch (Exception e) {
            log.error("Error processing RESO Member request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Internal server error", "Failed to retrieve members"));
        }
    }
    
    /**
     * RESO Office Resource Endpoint
     * GET /reso/odata/Office
     */
    @GetMapping(value = "/Office", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get Office Resources", description = "Retrieve real estate office information using OData queries")
    @PreAuthorize("hasAuthority('MLS_API_ACCESS')")
    public ResponseEntity<Map<String, Object>> getOffices(
            @RequestParam(value = "$filter", required = false) String filter,
            @RequestParam(value = "$select", required = false) String select,
            @RequestParam(value = "$expand", required = false) String expand,
            @RequestParam(value = "$orderby", required = false) String orderby,
            @RequestParam(value = "$top", required = false) Integer top,
            @RequestParam(value = "$skip", required = false) Integer skip,
            @RequestParam(value = "$count", required = false, defaultValue = "false") Boolean count,
            HttpServletRequest request) {
        
        try {
            ODataQueryParser.ODataQuery oDataQuery = oDataQueryParser.parseQuery(
                filter, select, expand, orderby, top, skip, "json", count
            );
            
            if (oDataQuery.getTop() == null) {
                oDataQuery.setTop(100);
            }
            if (oDataQuery.getTop() > 5000) {
                oDataQuery.setTop(5000);
            }
            
            Page<OfficeResource> results = resoApiService.getOffices(oDataQuery);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("@odata.context", request.getRequestURL() + "/$metadata#Office");
            
            if (count) {
                response.put("@odata.count", results.getTotalElements());
            }
            
            if (results.hasNext()) {
                int nextSkip = (oDataQuery.getSkip() != null ? oDataQuery.getSkip() : 0) + oDataQuery.getTop();
                String nextUrl = buildNextUrl(request, nextSkip, oDataQuery.getTop());
                response.put("@odata.nextLink", nextUrl);
            }
            
            response.put("value", results.getContent());
            
            HttpHeaders headers = new HttpHeaders();
            headers.add("OData-Version", "4.0");
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(response);
                
        } catch (Exception e) {
            log.error("Error processing RESO Office request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Internal server error", "Failed to retrieve offices"));
        }
    }
    
    /**
     * RESO Metadata Document
     * GET /reso/odata/$metadata
     */
    @GetMapping(value = "/$metadata", produces = "application/xml")
    @Operation(summary = "Get RESO Metadata", description = "Retrieve RESO Data Dictionary metadata document")
    public ResponseEntity<String> getMetadata() {
        try {
            String metadata = resoApiService.getMetadataDocument();
            
            HttpHeaders headers = new HttpHeaders();
            headers.add("OData-Version", "4.0");
            headers.setContentType(MediaType.APPLICATION_XML);
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(metadata);
                
        } catch (Exception e) {
            log.error("Error generating metadata document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * RESO Service Document
     * GET /reso/odata/
     */
    @GetMapping(value = {"", "/"}, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get RESO Service Document", description = "Retrieve available RESO resources and endpoints")
    public ResponseEntity<Map<String, Object>> getServiceDocument(HttpServletRequest request) {
        Map<String, Object> serviceDocument = new LinkedHashMap<>();
        serviceDocument.put("@odata.context", request.getRequestURL() + "$metadata");
        
        Map<String, String>[] resources = new Map[] {
            Map.of("name", "Property", "kind", "EntitySet", "url", "Property"),
            Map.of("name", "Member", "kind", "EntitySet", "url", "Member"),
            Map.of("name", "Office", "kind", "EntitySet", "url", "Office"),
            Map.of("name", "Media", "kind", "EntitySet", "url", "Media")
        };
        
        serviceDocument.put("value", resources);
        
        HttpHeaders headers = new HttpHeaders();
        headers.add("OData-Version", "4.0");
        
        return ResponseEntity.ok()
            .headers(headers)
            .body(serviceDocument);
    }
    
    // Helper methods
    
    private String buildNextUrl(HttpServletRequest request, int skip, int top) {
        StringBuilder url = new StringBuilder();
        url.append(request.getRequestURL());
        url.append("?$skip=").append(skip);
        url.append("&$top=").append(top);
        
        // Add other query parameters if present
        String queryString = request.getQueryString();
        if (queryString != null) {
            String[] params = queryString.split("&");
            for (String param : params) {
                if (!param.startsWith("$skip=") && !param.startsWith("$top=")) {
                    url.append("&").append(param);
                }
            }
        }
        
        return url.toString();
    }
    
    private Map<String, Object> createErrorResponse(String error, String message) {
        Map<String, Object> errorResponse = new LinkedHashMap<>();
        Map<String, Object> errorDetails = new LinkedHashMap<>();
        errorDetails.put("code", error);
        errorDetails.put("message", message);
        errorResponse.put("error", errorDetails);
        return errorResponse;
    }
    
    private Map<String, Object> convertToMap(Object object) {
        // Simple conversion - in production, use Jackson ObjectMapper
        // for proper JSON serialization
        Map<String, Object> map = new LinkedHashMap<>();
        // Implementation would use reflection or ObjectMapper
        // This is a placeholder for the actual implementation
        return map;
    }
}
