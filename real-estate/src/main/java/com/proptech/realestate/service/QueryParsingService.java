package com.proptech.realestate.service;

import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class QueryParsingService {
    
    // Location mapping for Vietnamese cities/districts
    private static final Map<String, LocationInfo> LOCATION_MAPPING = new HashMap<>() {{
        put("trung tâm", new LocationInfo(21.0285, 105.8542, "center", 3.0));
        put("downtown", new LocationInfo(21.0285, 105.8542, "center", 3.0));
        put("hoan kiem", new LocationInfo(21.0285, 105.8542, "hoan_kiem", 2.0));
        put("ba dinh", new LocationInfo(21.0382, 105.8442, "ba_dinh", 5.0));
        put("cau giay", new LocationInfo(21.0328, 105.7938, "cau_giay", 8.0));
        put("dong da", new LocationInfo(21.0108, 105.8291, "dong_da", 6.0));
        put("hai ba trung", new LocationInfo(21.0031, 105.8606, "hai_ba_trung", 7.0));
    }};
    
    // Property type mapping
    private static final Map<String, String> PROPERTY_TYPE_MAPPING = new HashMap<>() {{
        put("căn hộ", "APARTMENT");
        put("can ho", "APARTMENT"); 
        put("apartment", "APARTMENT");
        put("chung cư", "APARTMENT");
        put("chung cu", "APARTMENT"); // Common typo
        put("cung cu", "APARTMENT");  // Common typo
        put("nhà riêng", "HOUSE");
        put("nha rieng", "HOUSE");
        put("house", "HOUSE");
        put("villa", "VILLA");
        put("biệt thự", "VILLA");
        put("biet thu", "VILLA");
        put("office", "OFFICE");
        put("văn phòng", "OFFICE");
        put("van phong", "OFFICE");
        // Abbreviations
        put("cc", "APARTMENT");
        put("ch", "APARTMENT");
        put("nr", "HOUSE");
        put("bt", "VILLA");
        put("vp", "OFFICE");
    }};
    
    public ParsedQuery parseNaturalLanguage(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ParsedQuery();
        }
        
        // Step 1: Advanced input normalization
        String normalizedQuery = normalizeInput(query);
        ParsedQuery parsed = new ParsedQuery();
        
        // Step 2: Extract structured data
        normalizedQuery = extractBedrooms(normalizedQuery, parsed);
        normalizedQuery = extractBathrooms(normalizedQuery, parsed);
        normalizedQuery = extractPriceRange(normalizedQuery, parsed);
        normalizedQuery = extractAreaRange(normalizedQuery, parsed);
        normalizedQuery = extractLocation(normalizedQuery, parsed);
        normalizedQuery = extractPropertyType(normalizedQuery, parsed);
        
        // Step 3: Remaining text becomes keyword
        parsed.setKeyword(normalizedQuery.replaceAll("\\s+", " ").trim());
        
        return parsed;
    }
    
    /**
     * Advanced input normalization with multiple strategies
     */
    public String normalizeInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }
        
        String normalized = input.toLowerCase().trim();
        
        // Step 1: Basic cleaning
        normalized = basicCleaning(normalized);
        
        // Step 2: Expand abbreviations
        normalized = expandAbbreviations(normalized);
        
        // Step 3: Fix common typos
        normalized = fixCommonTypos(normalized);
        
        // Step 4: Restore diacritics
        normalized = restoreDiacritics(normalized);
        
        // Step 5: Standardize format
        normalized = standardizeFormat(normalized);
        
        return normalized;
    }
    
    private String basicCleaning(String text) {
        return text.replaceAll("\\s+", " ")
                  .replaceAll("[.,;:!?]+", " ")
                  .replaceAll("[-_]+", " ")
                  .trim();
    }
    
    private String expandAbbreviations(String text) {
        Map<String, String> abbreviations = new HashMap<>() {{
            put("2pn", "2 phòng ngủ");
            put("3pn", "3 phòng ngủ");
            put("1pn", "1 phòng ngủ");
            put("4pn", "4 phòng ngủ");
            put("2wc", "2 phòng tắm");
            put("3wc", "3 phòng tắm");
            put("1wc", "1 phòng tắm");
            put("2br", "2 phòng ngủ");
            put("3br", "3 phòng ngủ");
            put("1br", "1 phòng ngủ");
            put("cc", "chung cư");
            put("ch", "căn hộ");
            put("nr", "nhà riêng");
            put("bt", "biệt thự");
            put("vp", "văn phòng");
            put("q1", "quận 1");
            put("q2", "quận 2");
            put("q3", "quận 3");
            put("q7", "quận 7");
            put("q10", "quận 10");
            put("hk", "hoàn kiếm");
            put("bd", "ba đình");
            put("cg", "cầu giấy");
            put("dd", "đống đa");
            put("hbt", "hai bà trưng");
            put("m2", "mét vuông");
            put("sqm", "mét vuông");
            put("tr", "triệu");
            put("ty", "tỷ");
        }};
        
        String result = text;
        for (Map.Entry<String, String> entry : abbreviations.entrySet()) {
            result = result.replaceAll("\\b" + entry.getKey() + "\\b", entry.getValue());
        }
        return result;
    }
    
    private String fixCommonTypos(String text) {
        Map<String, String> typoCorrections = new HashMap<>() {{
            put("cung cu", "chung cư");
            put("chung cu", "chung cư");
            put("can ho", "căn hộ");
            put("nha rieng", "nhà riêng");
            put("biet thu", "biệt thự");
            put("van phong", "văn phòng");
            put("phong ngu", "phòng ngủ");
            put("phong tam", "phòng tắm");
            put("met vuong", "mét vuông");
            put("hoan kiem", "hoàn kiếm");
            put("ba dinh", "ba đình");
            put("cau giay", "cầu giấy");
            put("dong da", "đống đa");
            put("hai ba trung", "hai bà trưng");
            put("gia re", "giá rẻ");
            put("dep", "đẹp");
            put("moi", "mới");
            put("gan", "gần");
        }};
        
        String result = text;
        for (Map.Entry<String, String> entry : typoCorrections.entrySet()) {
            result = result.replaceAll("\\b" + entry.getKey() + "\\b", entry.getValue());
        }
        return result;
    }
    
    private String restoreDiacritics(String text) {
        Map<String, String> diacriticsMap = new HashMap<>() {{
            put("can ho", "căn hộ");
            put("chung cu", "chung cư");
            put("nha rieng", "nhà riêng");
            put("biet thu", "biệt thự");
            put("van phong", "văn phòng");
            put("phong ngu", "phòng ngủ");
            put("phong tam", "phòng tắm");
            put("met vuong", "mét vuông");
            put("trung tam", "trung tâm");
            put("hoan kiem", "hoàn kiếm");
            put("ba dinh", "ba đình");
            put("cau giay", "cầu giấy");
            put("dong da", "đống đa");
            put("hai ba trung", "hai bà trưng");
            put("gia re", "giá rẻ");
            put("dep", "đẹp");
            put("moi", "mới");
            put("gan", "gần");
            put("duong", "đường");
            put("khu", "khu");
            put("quan", "quận");
            put("phuong", "phường");
        }};
        
        String result = text;
        for (Map.Entry<String, String> entry : diacriticsMap.entrySet()) {
            result = result.replaceAll("\\b" + entry.getKey() + "\\b", entry.getValue());
        }
        return result;
    }
    
    private String standardizeFormat(String text) {
        return text.replaceAll("\\s+", " ")
                  .replaceAll("\\s*-\\s*", " đến ")
                  .replaceAll("từ\\s+", "")
                  .replaceAll("\\s+đến\\s+", " - ")
                  .trim();
    }
    
    private String extractBedrooms(String query, ParsedQuery parsed) {
        // Patterns for bedroom extraction
        String[] patterns = {
            "(\\d+)\\s*phòng\\s*ngủ",
            "(\\d+)\\s*pn",
            "(\\d+)\\s*br",
            "(\\d+)\\s*bedroom",
            "(hai|ba|bốn|năm)\\s*phòng\\s*ngủ"
        };
        
        for (String patternStr : patterns) {
            Pattern pattern = Pattern.compile(patternStr);
            Matcher matcher = pattern.matcher(query);
            if (matcher.find()) {
                String bedroomStr = matcher.group(1);
                
                // Convert Vietnamese numbers
                int bedrooms = convertVietnameseNumber(bedroomStr);
                if (bedrooms > 0) {
                    parsed.setMinBedrooms(bedrooms);
                    parsed.setMaxBedrooms(bedrooms);
                    query = query.replaceAll(patternStr, " ");
                    break;
                }
            }
        }
        
        return query;
    }
    
    private String extractBathrooms(String query, ParsedQuery parsed) {
        String[] patterns = {
            "(\\d+)\\s*phòng\\s*tắm",
            "(\\d+)\\s*toilet",
            "(\\d+)\\s*wc"
        };
        
        for (String patternStr : patterns) {
            Pattern pattern = Pattern.compile(patternStr);
            Matcher matcher = pattern.matcher(query);
            if (matcher.find()) {
                int bathrooms = Integer.parseInt(matcher.group(1));
                parsed.setMinBathrooms(bathrooms);
                parsed.setMaxBathrooms(bathrooms);
                query = query.replaceAll(patternStr, " ");
                break;
            }
        }
        
        return query;
    }
    
    private String extractPriceRange(String query, ParsedQuery parsed) {
        // Patterns for price extraction
        String[] patterns = {
            "(\\d+(?:\\.\\d+)?)\\s*(?:triệu|tr)\\s*(?:đến|->|-)\\s*(\\d+(?:\\.\\d+)?)\\s*(?:triệu|tr)",
            "(?:từ|from)\\s*(\\d+(?:\\.\\d+)?)\\s*(?:triệu|tr)",
            "(?:dưới|under)\\s*(\\d+(?:\\.\\d+)?)\\s*(?:triệu|tr)",
            "(?:trên|over)\\s*(\\d+(?:\\.\\d+)?)\\s*(?:triệu|tr)"
        };
        
        Pattern rangePattern = Pattern.compile(patterns[0]);
        Matcher rangeMatcher = rangePattern.matcher(query);
        if (rangeMatcher.find()) {
            double minPrice = Double.parseDouble(rangeMatcher.group(1)) * 1_000_000;
            double maxPrice = Double.parseDouble(rangeMatcher.group(2)) * 1_000_000;
            parsed.setMinPrice(java.math.BigDecimal.valueOf(minPrice));
            parsed.setMaxPrice(java.math.BigDecimal.valueOf(maxPrice));
            query = query.replaceAll(patterns[0], " ");
        }
        
        return query;
    }
    
    private String extractAreaRange(String query, ParsedQuery parsed) {
        String[] patterns = {
            "(\\d+)\\s*(?:m2|m²|sqm)\\s*(?:đến|->|-)\\s*(\\d+)\\s*(?:m2|m²|sqm)",
            "(?:từ|from)\\s*(\\d+)\\s*(?:m2|m²|sqm)",
            "(?:dưới|under)\\s*(\\d+)\\s*(?:m2|m²|sqm)",
            "(?:trên|over)\\s*(\\d+)\\s*(?:m2|m²|sqm)"
        };
        
        Pattern rangePattern = Pattern.compile(patterns[0]);
        Matcher rangeMatcher = rangePattern.matcher(query);
        if (rangeMatcher.find()) {
            double minArea = Double.parseDouble(rangeMatcher.group(1));
            double maxArea = Double.parseDouble(rangeMatcher.group(2));
            parsed.setMinArea(minArea);
            parsed.setMaxArea(maxArea);
            query = query.replaceAll(patterns[0], " ");
        }
        
        return query;
    }
    
    private String extractLocation(String query, ParsedQuery parsed) {
        for (Map.Entry<String, LocationInfo> entry : LOCATION_MAPPING.entrySet()) {
            String locationKey = entry.getKey();
            LocationInfo locationInfo = entry.getValue();
            
            if (query.contains(locationKey) || query.contains("gần " + locationKey)) {
                parsed.setLocationInfo(locationInfo);
                parsed.getLocationKeywords().add(locationKey);
                
                // Special handling for "near center" queries
                if (locationKey.contains("trung tâm") || locationKey.contains("downtown")) {
                    parsed.setNearCenter(true);
                }
                
                query = query.replaceAll("(?:gần\\s+)?" + Pattern.quote(locationKey), " ");
                break;
            }
        }
        
        return query;
    }
    
    private String extractPropertyType(String query, ParsedQuery parsed) {
        for (Map.Entry<String, String> entry : PROPERTY_TYPE_MAPPING.entrySet()) {
            String typeKey = entry.getKey();
            String typeValue = entry.getValue();
            
            if (query.contains(typeKey)) {
                parsed.setPropertyType(typeValue);
                query = query.replaceAll(Pattern.quote(typeKey), " ");
                break;
            }
        }
        
        return query;
    }
    
    private int convertVietnameseNumber(String numberStr) {
        Map<String, Integer> vietnameseNumbers = Map.of(
            "một", 1, "hai", 2, "ba", 3, "bốn", 4, "năm", 5,
            "sáu", 6, "bảy", 7, "tám", 8, "chín", 9, "mười", 10
        );
        
        if (vietnameseNumbers.containsKey(numberStr)) {
            return vietnameseNumbers.get(numberStr);
        }
        
        try {
            return Integer.parseInt(numberStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    @Data
    public static class ParsedQuery {
        private String keyword = "";
        private Integer minBedrooms;
        private Integer maxBedrooms;
        private Integer minBathrooms;
        private Integer maxBathrooms;
        private java.math.BigDecimal minPrice;
        private java.math.BigDecimal maxPrice;
        private Double minArea;
        private Double maxArea;
        private String propertyType;
        private LocationInfo locationInfo;
        private List<String> locationKeywords = new ArrayList<>();
        private boolean nearCenter = false;
        
        public boolean hasStructuredData() {
            return minBedrooms != null || minBathrooms != null || 
                   minPrice != null || minArea != null || 
                   propertyType != null || locationInfo != null;
        }
    }
    
    @Data
    public static class LocationInfo {
        private final double latitude;
        private final double longitude;
        private final String district;
        private final double radiusKm;
        
        public LocationInfo(double latitude, double longitude, String district, double radiusKm) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.district = district;
            this.radiusKm = radiusKm;
        }
    }
} 