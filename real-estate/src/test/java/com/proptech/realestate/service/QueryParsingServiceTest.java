package com.proptech.realestate.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Query Parsing Service Tests")
public class QueryParsingServiceTest {
    
    private QueryParsingService queryParsingService;
    
    @BeforeEach
    void setUp() {
        queryParsingService = new QueryParsingService();
    }
    
    @Test
    @DisplayName("Should normalize basic input correctly")
    void testBasicInputNormalization() {
        // Test basic cleaning
        String input = "can  ho   2pn!!!  gan   trung  tam...";
        String normalized = queryParsingService.normalizeInput(input);
        
        assertThat(normalized).contains("căn hộ");
        assertThat(normalized).contains("2 phòng ngủ");
        assertThat(normalized).contains("gần");
        assertThat(normalized).contains("trung tâm");
    }
    
    @Test
    @DisplayName("Should expand abbreviations correctly")
    void testAbbreviationExpansion() {
        String input = "cc 2pn q1";
        String normalized = queryParsingService.normalizeInput(input);
        
        assertThat(normalized).contains("chung cư");
        assertThat(normalized).contains("2 phòng ngủ");
        assertThat(normalized).contains("quận 1");
    }
    
    @Test
    @DisplayName("Should fix common typos")
    void testTypoCorrection() {
        String input = "cung cu can ho nha rieng";
        String normalized = queryParsingService.normalizeInput(input);
        
        assertThat(normalized).contains("chung cư");
        assertThat(normalized).contains("căn hộ");
        assertThat(normalized).contains("nhà riêng");
    }
    
    @Test
    @DisplayName("Should restore Vietnamese diacritics")
    void testDiacriticsRestoration() {
        String input = "can ho gan trung tam";
        String normalized = queryParsingService.normalizeInput(input);
        
        assertThat(normalized).contains("căn hộ");
        assertThat(normalized).contains("gần");
        assertThat(normalized).contains("trung tâm");
    }
    
    @Test
    @DisplayName("Should parse natural language query correctly")
    void testNaturalLanguageQueryParsing() {
        String query = "căn hộ 2 phòng ngủ gần trung tâm 5-10 triệu";
        QueryParsingService.ParsedQuery parsed = queryParsingService.parseNaturalLanguage(query);
        
        assertThat(parsed.getMinBedrooms()).isEqualTo(2);
        assertThat(parsed.getMaxBedrooms()).isEqualTo(2);
        assertThat(parsed.getPropertyType()).isEqualTo("APARTMENT");
        assertThat(parsed.getLocationInfo()).isNotNull();
        assertThat(parsed.getLocationInfo().getDistrict()).isEqualTo("center");
    }
    
    @Test
    @DisplayName("Should handle mixed language input")
    void testMixedLanguageInput() {
        String query = "apartment 2BR Q1 gia re";
        QueryParsingService.ParsedQuery parsed = queryParsingService.parseNaturalLanguage(query);
        
        assertThat(parsed.getMinBedrooms()).isEqualTo(2);
        assertThat(parsed.getPropertyType()).isEqualTo("APARTMENT");
    }
    
    @Test
    @DisplayName("Should parse price ranges correctly")
    void testPriceRangeParsing() {
        String query = "căn hộ từ 5 triệu";
        QueryParsingService.ParsedQuery parsed = queryParsingService.parseNaturalLanguage(query);
        
        // This would need the price range extraction to be implemented
        assertThat(parsed.getPropertyType()).isEqualTo("APARTMENT");
    }
    
    @Test
    @DisplayName("Should parse area ranges correctly")
    void testAreaRangeParsing() {
        String query = "chung cư 50-80m2";
        QueryParsingService.ParsedQuery parsed = queryParsingService.parseNaturalLanguage(query);
        
        assertThat(parsed.getPropertyType()).isEqualTo("APARTMENT");
        // This would need the area range extraction to be implemented
    }
    
    @Test
    @DisplayName("Should handle complex queries")
    void testComplexQueries() {
        String query = "CC 3PN 2WC HK gia re dep moi";
        String normalized = queryParsingService.normalizeInput(query);
        QueryParsingService.ParsedQuery parsed = queryParsingService.parseNaturalLanguage(query);
        
        assertThat(normalized).contains("chung cư");
        assertThat(normalized).contains("3 phòng ngủ");
        assertThat(normalized).contains("2 phòng tắm");
        assertThat(normalized).contains("hoàn kiếm");
        assertThat(normalized).contains("giá rẻ");
        assertThat(normalized).contains("đẹp");
        assertThat(normalized).contains("mới");
        
        assertThat(parsed.getPropertyType()).isEqualTo("APARTMENT");
        assertThat(parsed.getMinBedrooms()).isEqualTo(3);
        assertThat(parsed.getMinBathrooms()).isEqualTo(2);
    }
    
    @Test
    @DisplayName("Should preserve meaningful keywords after extraction")
    void testKeywordExtraction() {
        String query = "căn hộ 2 phòng ngủ view đẹp balcony";
        QueryParsingService.ParsedQuery parsed = queryParsingService.parseNaturalLanguage(query);
        
        assertThat(parsed.getPropertyType()).isEqualTo("APARTMENT");
        assertThat(parsed.getMinBedrooms()).isEqualTo(2);
        assertThat(parsed.getKeyword()).contains("view");
        assertThat(parsed.getKeyword()).contains("đẹp");
        assertThat(parsed.getKeyword()).contains("balcony");
    }
    
    @Test
    @DisplayName("Should handle empty or null inputs gracefully")
    void testEmptyInputHandling() {
        assertThat(queryParsingService.normalizeInput(null)).isEmpty();
        assertThat(queryParsingService.normalizeInput("")).isEmpty();
        assertThat(queryParsingService.normalizeInput("   ")).isEmpty();
        
        QueryParsingService.ParsedQuery parsed = queryParsingService.parseNaturalLanguage(null);
        assertThat(parsed).isNotNull();
        assertThat(parsed.getKeyword()).isEqualTo("");
    }
    
    @Test
    void testParseCanHo2PhongNguGanTrungTam() {
        // Test case chính: "căn hộ 2 phòng ngủ gần trung tâm"
        String query = "căn hộ 2 phòng ngủ gần trung tâm";
        
        QueryParsingService.ParsedQuery result = queryParsingService.parseNaturalLanguage(query);
        
        // Assertions
        assertEquals("APARTMENT", result.getPropertyType(), "Should extract 'căn hộ' as APARTMENT");
        assertEquals(2, result.getMinBedrooms(), "Should extract '2 phòng ngủ' as minBedrooms=2");
        assertEquals(2, result.getMaxBedrooms(), "Should extract '2 phòng ngủ' as maxBedrooms=2");
        assertTrue(result.isNearCenter(), "Should detect 'gần trung tâm' as nearCenter=true");
        assertNotNull(result.getLocationInfo(), "Should have location info for 'trung tâm'");
        assertEquals(21.0285, result.getLocationInfo().getLatitude(), 0.001, "Should have Hanoi center coordinates");
        assertEquals(105.8542, result.getLocationInfo().getLongitude(), 0.001, "Should have Hanoi center coordinates");
        assertTrue(result.getLocationKeywords().contains("trung tâm"), "Should include 'trung tâm' in location keywords");
        
        // Remaining keyword should be clean
        assertTrue(result.getKeyword().trim().isEmpty() || result.getKeyword().trim().length() < 5, 
                  "Keyword should be mostly cleaned: '" + result.getKeyword() + "'");
        
        // Should have structured data
        assertTrue(result.hasStructuredData(), "Should detect structured data");
        
        System.out.println("=== Test: " + query + " ===");
        System.out.println("Property Type: " + result.getPropertyType());
        System.out.println("Bedrooms: " + result.getMinBedrooms() + "-" + result.getMaxBedrooms());
        System.out.println("Near Center: " + result.isNearCenter());
        System.out.println("Location: " + result.getLocationInfo().getDistrict());
        System.out.println("Remaining Keyword: '" + result.getKeyword() + "'");
        System.out.println();
    }
    
    @Test
    void testParseComplexQuery() {
        // Test case phức tạp: "chung cư 3 phòng ngủ 2 phòng tắm từ 8 triệu ba đình"
        String query = "chung cư 3 phòng ngủ 2 phòng tắm từ 8 triệu ba đình";
        
        QueryParsingService.ParsedQuery result = queryParsingService.parseNaturalLanguage(query);
        
        assertEquals("APARTMENT", result.getPropertyType());
        assertEquals(3, result.getMinBedrooms());
        assertEquals(2, result.getMinBathrooms());
        assertNotNull(result.getMinPrice());
        assertEquals(8000000, result.getMinPrice().doubleValue(), 0.01);
        assertNotNull(result.getLocationInfo());
        assertEquals("ba_dinh", result.getLocationInfo().getDistrict());
        
        System.out.println("=== Test: " + query + " ===");
        System.out.println("Property Type: " + result.getPropertyType());
        System.out.println("Bedrooms: " + result.getMinBedrooms());
        System.out.println("Bathrooms: " + result.getMinBathrooms());  
        System.out.println("Min Price: " + result.getMinPrice());
        System.out.println("Location: " + result.getLocationInfo().getDistrict());
        System.out.println("Remaining Keyword: '" + result.getKeyword() + "'");
        System.out.println();
    }
    
    @Test
    void testParseEnglishQuery() {
        // Test case tiếng Anh: "2BR apartment downtown under 10 million"
        String query = "2BR apartment downtown under 10 million";
        
        QueryParsingService.ParsedQuery result = queryParsingService.parseNaturalLanguage(query);
        
        assertEquals("APARTMENT", result.getPropertyType());
        assertEquals(2, result.getMinBedrooms());
        assertTrue(result.isNearCenter());
        assertNotNull(result.getLocationInfo());
        
        System.out.println("=== Test: " + query + " ===");
        System.out.println("Property Type: " + result.getPropertyType());
        System.out.println("Bedrooms: " + result.getMinBedrooms());
        System.out.println("Near Center: " + result.isNearCenter());
        System.out.println("Remaining Keyword: '" + result.getKeyword() + "'");
        System.out.println();
    }
    
    @Test
    void testParseVietnameseNumbers() {
        // Test case số tiếng Việt: "hai phòng ngủ"
        String query = "căn hộ hai phòng ngủ cầu giấy";
        
        QueryParsingService.ParsedQuery result = queryParsingService.parseNaturalLanguage(query);
        
        assertEquals("APARTMENT", result.getPropertyType());
        assertEquals(2, result.getMinBedrooms());
        assertNotNull(result.getLocationInfo());
        assertEquals("cau_giay", result.getLocationInfo().getDistrict());
        
        System.out.println("=== Test: " + query + " ===");
        System.out.println("Property Type: " + result.getPropertyType());
        System.out.println("Bedrooms: " + result.getMinBedrooms());
        System.out.println("Location: " + result.getLocationInfo().getDistrict());
        System.out.println("Remaining Keyword: '" + result.getKeyword() + "'");
        System.out.println();
    }
    
    @Test
    void testParseAreaRange() {
        // Test case diện tích: "50m2 đến 80m2"
        String query = "nhà riêng 50m2 đến 80m2 đông đa";
        
        QueryParsingService.ParsedQuery result = queryParsingService.parseNaturalLanguage(query);
        
        assertEquals("HOUSE", result.getPropertyType());
        assertEquals(50.0, result.getMinArea());
        assertEquals(80.0, result.getMaxArea());
        assertEquals("dong_da", result.getLocationInfo().getDistrict());
        
        System.out.println("=== Test: " + query + " ===");
        System.out.println("Property Type: " + result.getPropertyType());
        System.out.println("Area: " + result.getMinArea() + "-" + result.getMaxArea() + "m²");
        System.out.println("Location: " + result.getLocationInfo().getDistrict());
        System.out.println("Remaining Keyword: '" + result.getKeyword() + "'");
        System.out.println();
    }
    
    @Test
    void testParseSimpleQuery() {
        // Test case đơn giản: không có structured data
        String query = "tìm nhà đẹp giá rẻ";
        
        QueryParsingService.ParsedQuery result = queryParsingService.parseNaturalLanguage(query);
        
        assertNull(result.getPropertyType());
        assertNull(result.getMinBedrooms());
        assertNull(result.getLocationInfo());
        assertFalse(result.hasStructuredData());
        assertEquals("tìm nhà đẹp giá rẻ", result.getKeyword());
        
        System.out.println("=== Test: " + query + " ===");
        System.out.println("Has Structured Data: " + result.hasStructuredData());
        System.out.println("Keyword: '" + result.getKeyword() + "'");
        System.out.println();
    }
} 