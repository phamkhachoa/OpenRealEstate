package com.proptech.realestate.service;

import com.proptech.realestate.model.dto.NLPQueryResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Vietnamese NLP Service using UndertheSea library
 * Provides tokenization, NER, and entity extraction for real estate queries
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VietnameseNLPService {

    private final PythonExecutorService pythonExecutorService;

    /**
     * Process natural language query and extract structured information
     */
    public Optional<NLPQueryResult> processQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            log.warn("Empty or null query provided");
            return Optional.empty();
        }

        try {
            log.debug("Processing Vietnamese query: {}", query);
            
            // Execute Python NLP script
            PythonExecutorService.PythonExecutionResult result = 
                pythonExecutorService.executePythonScript(query.trim());
            
            if (!result.isSuccess()) {
                log.error("Python NLP processing failed: {}", result.getError());
                return Optional.empty();
            }
            
            // Parse JSON result
            NLPQueryResult nlpResult = pythonExecutorService.parseJsonResult(
                result.getOutput(), NLPQueryResult.class);
            
            log.debug("NLP processing completed with confidence: {}", 
                nlpResult.getConfidenceScore());
            
            return Optional.of(nlpResult);
            
        } catch (Exception e) {
            log.error("Error processing Vietnamese NLP query: {}", query, e);
            return Optional.empty();
        }
    }

    /**
     * Simple tokenization using UndertheSea
     */
    public List<String> tokenize(String text) {
        Optional<NLPQueryResult> result = processQuery(text);
        return result.map(NLPQueryResult::getTokens)
                    .orElse(List.of());
    }

    /**
     * Extract property type from text
     */
    public Optional<String> extractPropertyType(String text) {
        return processQuery(text)
                .map(NLPQueryResult::getExtractedEntities)
                .map(NLPQueryResult.ExtractedEntities::getPropertyType);
    }

    /**
     * Extract price range from text
     */
    public Optional<NLPQueryResult.PriceRange> extractPriceRange(String text) {
        return processQuery(text)
                .map(NLPQueryResult::getExtractedEntities)
                .map(NLPQueryResult.ExtractedEntities::getPriceRange);
    }

    /**
     * Extract location information from text
     */
    public Optional<NLPQueryResult.LocationInfo> extractLocation(String text) {
        return processQuery(text)
                .map(NLPQueryResult::getExtractedEntities)
                .map(NLPQueryResult.ExtractedEntities::getLocation);
    }

    /**
     * Extract number of bedrooms from text
     */
    public Optional<Integer> extractBedrooms(String text) {
        return processQuery(text)
                .map(NLPQueryResult::getExtractedEntities)
                .map(NLPQueryResult.ExtractedEntities::getBedrooms);
    }

    /**
     * Extract amenities from text
     */
    public List<String> extractAmenities(String text) {
        return processQuery(text)
                .map(NLPQueryResult::getExtractedEntities)
                .map(NLPQueryResult.ExtractedEntities::getAmenities)
                .orElse(List.of());
    }

    /**
     * Extract transaction type (buy, rent, invest) from text
     */
    public Optional<String> extractTransactionType(String text) {
        return processQuery(text)
                .map(NLPQueryResult::getExtractedEntities)
                .map(NLPQueryResult.ExtractedEntities::getTransactionType);
    }

    /**
     * Check if the NLP system is available and working
     */
    public boolean isAvailable() {
        return pythonExecutorService.checkPythonEnvironment();
    }

    /**
     * Get confidence score for a processed query
     */
    public double getConfidenceScore(String text) {
        return processQuery(text)
                .map(NLPQueryResult::getConfidenceScore)
                .orElse(0.0);
    }
} 