package com.proptech.realestate.controller;

import com.proptech.realestate.service.DemoExceptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * Demo controller for testing the new ConfigurableException system
 * Provides various endpoints to trigger different exception scenarios
 */
@RestController
@RequestMapping("/api/demo/exceptions")
@RequiredArgsConstructor
@Slf4j
public class DemoExceptionController {

    private final DemoExceptionService demoService;

    /**
     * Test simple exception with message key only
     * GET /api/demo/exceptions/simple
     */
    @GetMapping("/simple")
    public String testSimpleException() {
        demoService.throwSimpleException();
        return "This should not be reached";
    }

    /**
     * Test exception with parameters
     * GET /api/demo/exceptions/with-params?propertyId=PROP123
     */
    @GetMapping("/with-params")
    public String testExceptionWithParameters(@RequestParam(defaultValue = "PROP123") String propertyId) {
        demoService.throwExceptionWithParameters(propertyId);
        return "This should not be reached";
    }

    /**
     * Test exception with custom HTTP status
     * GET /api/demo/exceptions/custom-status?email=invalid-email
     */
    @GetMapping("/custom-status")
    public String testExceptionWithCustomStatus(@RequestParam(defaultValue = "invalid-email") String email) {
        demoService.throwExceptionWithCustomStatus(email);
        return "This should not be reached";
    }

    /**
     * Test exception with builder pattern
     * GET /api/demo/exceptions/builder?resource=User&field=email&value=test@example.com
     */
    @GetMapping("/builder")
    public String testExceptionWithBuilder(
            @RequestParam(defaultValue = "User") String resource,
            @RequestParam(defaultValue = "email") String field,
            @RequestParam(defaultValue = "test@example.com") String value) {
        demoService.throwExceptionWithBuilder(resource, field, value);
        return "This should not be reached";
    }

    /**
     * Test static factory method - Not Found
     * GET /api/demo/exceptions/not-found?propertyId=PROP456
     */
    @GetMapping("/not-found")
    public String testNotFoundFactory(@RequestParam(defaultValue = "PROP456") String propertyId) {
        demoService.throwNotFoundUsingFactory(propertyId);
        return "This should not be reached";
    }

    /**
     * Test static factory method - Bad Request
     * GET /api/demo/exceptions/bad-request?price=invalid-price
     */
    @GetMapping("/bad-request")
    public String testBadRequestFactory(@RequestParam(defaultValue = "invalid-price") String price) {
        demoService.throwBadRequestUsingFactory(price);
        return "This should not be reached";
    }

    /**
     * Test static factory method - Unauthorized
     * GET /api/demo/exceptions/unauthorized
     */
    @GetMapping("/unauthorized")
    public String testUnauthorizedFactory() {
        demoService.throwUnauthorizedUsingFactory();
        return "This should not be reached";
    }

    /**
     * Test internal error with cause
     * GET /api/demo/exceptions/internal-error
     */
    @GetMapping("/internal-error")
    public String testInternalErrorWithCause() {
        demoService.throwInternalErrorWithCause();
        return "This should not be reached";
    }

    /**
     * Test property price validation
     * GET /api/demo/exceptions/validate-price?price=1500000
     */
    @GetMapping("/validate-price")
    public String testPropertyPriceValidation(@RequestParam(required = false) BigDecimal price) {
        demoService.validatePropertyPrice(price);
        return "Price validation passed: " + price;
    }

    /**
     * Test MLS operation with different scenarios
     * GET /api/demo/exceptions/mls?feedId=not-found
     * 
     * Available scenarios:
     * - not-found: Resource not found
     * - auth-failed: Authentication failed
     * - rate-limit: Rate limit exceeded
     * - data-error: Data format invalid
     * - system-error: System error with cause
     * - success: Normal operation (no exception)
     */
    @GetMapping("/mls")
    public String testMLSOperation(@RequestParam(defaultValue = "success") String feedId) {
        demoService.performMLSOperation(feedId);
        return "MLS operation completed successfully for feed: " + feedId;
    }

    /**
     * Test search operation
     * GET /api/demo/exceptions/search?query=property
     * 
     * Special queries that trigger errors:
     * - "" (empty): Required field missing
     * - "ab" (too short): Query too short
     * - "elasticsearch-down": Service unavailable
     * - "*" or short queries: Too many results
     */
    @GetMapping("/search")
    public String testSearchOperation(@RequestParam(defaultValue = "property") String query) {
        demoService.performSearch(query);
        return "Search completed successfully for query: " + query;
    }

    /**
     * Test file upload validation
     * POST /api/demo/exceptions/upload
     */
    @PostMapping("/upload")
    public String testFileUploadValidation(
            @RequestParam(defaultValue = "document.pdf") String fileName,
            @RequestParam(defaultValue = "1048576") long fileSize,
            @RequestParam(defaultValue = "application/pdf") String contentType) {
        
        demoService.validateFileUpload(fileName, fileSize, contentType);
        return String.format("File validation passed: %s (size: %d, type: %s)", 
            fileName, fileSize, contentType);
    }

    /**
     * Test random exception scenario
     * GET /api/demo/exceptions/random
     */
    @GetMapping("/random")
    public String testRandomException() {
        demoService.throwRandomException();
        return "Random exception test completed (no exception thrown)";
    }

    /**
     * Test all exception scenarios sequentially
     * GET /api/demo/exceptions/test-all
     */
    @GetMapping("/test-all")
    public String testAllScenarios() {
        log.info("Testing all exception scenarios...");
        
        try {
            demoService.throwSimpleException();
        } catch (Exception e) {
            log.info("Caught simple exception: {}", e.getMessage());
        }
        
        try {
            demoService.throwExceptionWithParameters("TEST-PROP");
        } catch (Exception e) {
            log.info("Caught exception with parameters: {}", e.getMessage());
        }
        
        try {
            demoService.throwNotFoundUsingFactory("TEST-USER");
        } catch (Exception e) {
            log.info("Caught not found exception: {}", e.getMessage());
        }
        
        try {
            demoService.performMLSOperation("rate-limit");
        } catch (Exception e) {
            log.info("Caught MLS exception: {}", e.getMessage());
        }
        
        try {
            demoService.validatePropertyPrice(BigDecimal.valueOf(-1000));
        } catch (Exception e) {
            log.info("Caught price validation exception: {}", e.getMessage());
        }
        
        return "All exception scenarios tested successfully. Check logs for details.";
    }

    /**
     * Generate documentation for available test endpoints
     * GET /api/demo/exceptions/help
     */
    @GetMapping("/help")
    public String getHelp() {
        return """
            ConfigurableException Demo Endpoints:
            
            1. GET /api/demo/exceptions/simple
               - Tests simple exception with message key only
            
            2. GET /api/demo/exceptions/with-params?propertyId=PROP123
               - Tests exception with parameters
            
            3. GET /api/demo/exceptions/custom-status?email=invalid-email
               - Tests exception with custom HTTP status
            
            4. GET /api/demo/exceptions/builder?resource=User&field=email&value=test@example.com
               - Tests exception with builder pattern
            
            5. GET /api/demo/exceptions/not-found?propertyId=PROP456
               - Tests static factory method - Not Found
            
            6. GET /api/demo/exceptions/bad-request?price=invalid-price
               - Tests static factory method - Bad Request
            
            7. GET /api/demo/exceptions/unauthorized
               - Tests static factory method - Unauthorized
            
            8. GET /api/demo/exceptions/internal-error
               - Tests internal error with cause
            
            9. GET /api/demo/exceptions/validate-price?price=1500000
               - Tests property price validation (try negative or null values)
            
            10. GET /api/demo/exceptions/mls?feedId=not-found
                - Tests MLS operations (feedId: not-found, auth-failed, rate-limit, data-error, system-error)
            
            11. GET /api/demo/exceptions/search?query=property
                - Tests search operations (try: empty, "ab", "elasticsearch-down", "*")
            
            12. POST /api/demo/exceptions/upload?fileName=document.pdf&fileSize=1048576&contentType=application/pdf
                - Tests file upload validation
            
            13. GET /api/demo/exceptions/random
                - Tests random exception scenario
            
            14. GET /api/demo/exceptions/test-all
                - Tests all scenarios sequentially (safe - catches exceptions)
            
            Configuration:
            - Stack traces can be controlled via application.yml
            - Messages are internationalized (supports Vietnamese via Accept-Language header)
            - Request IDs are generated for tracing
            - Detailed error information is configurable
            """;
    }
} 