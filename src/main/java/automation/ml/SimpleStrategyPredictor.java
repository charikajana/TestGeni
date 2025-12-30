package automation.ml;

import automation.utils.LoggerUtil;

import java.util.*;

/**
 * Simple rule-based locator strategy predictor.
 * A lightweight alternative to ML that uses heuristics and patterns.
 * 
 * This provides immediate value while ML model is being developed.
 * Can be replaced with full ML model later.
 * 
 * Strategy selection based on:
 * - Page domain patterns
 * - Element name characteristics  
 * - Element type
 * - Historical patterns from cache
 * 
 * @author Chari
 * @version 1.0
 */
public class SimpleStrategyPredictor {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(SimpleStrategyPredictor.class);
    
    // Pattern-based rules
    private final Map<String, String> domainPreferences = new HashMap<>();
    private final Map<String, Integer> strategyScores = new HashMap<>();
    
    public SimpleStrategyPredictor() {
        initializeRules();
    }
    
    /**
     * Initialize heuristic rules
     */
    private void initializeRules() {
        // Modern apps often use data-testid
        domainPreferences.put("react", "data-testid");
        domainPreferences.put("next", "data-testid");
        domainPreferences.put("vue", "data-testid");
        
        // Testing sites use stable IDs
        domainPreferences.put("demoqa", "id");
        domainPreferences.put("demo", "id");
        domainPreferences.put("test", "id");
        
        // Default strategy scores (higher = prefer)
        strategyScores.put("id", 100);
        strategyScores.put("data-testid", 90);
        strategyScores.put("name", 80);
        strategyScores.put("aria-label", 70);
        strategyScores.put("placeholder", 60);
        strategyScores.put("css-class", 40);
        strategyScores.put("xpath", 30);
    }
    
    /**
     * Predict best strategy based on context
     */
    public List<String> predictStrategies(String pageDomain, String elementName, String elementType) {
        Map<String, Integer> scores = new HashMap<>(strategyScores);
        
        // Adjust scores based on context
        
        // 1. Domain-based preferences
        for (Map.Entry<String, String> pref : domainPreferences.entrySet()) {
            if (pageDomain.toLowerCase().contains(pref.getKey())) {
                scores.put(pref.getValue(), scores.getOrDefault(pref.getValue(), 50) + 20);
            }
        }
        
        // 2. Element type specific
        if ("input".equals(elementType)) {
            scores.put("name", scores.get("name") + 15);
            scores.put("placeholder", scores.get("placeholder") + 15);
        } else if ("button".equals(elementType)) {
            scores.put("id", scores.get("id") + 10);
            scores.put("text", scores.getOrDefault("text", 50) + 20);
        }
        
        // 3. Element name patterns
        if (elementName != null) {
            if (elementName.matches(".*\\d+.*")) {
                // Has numbers - likely dynamic, prefer stable strategies
                scores.put("data-testid", scores.get("data-testid") + 15);
                scores.put("aria-label", scores.get("aria-label") + 10);
            }
            
            if (elementName.contains(" ")) {
                // Multi-word - text matching might work
                scores.put("text", scores.getOrDefault("text", 50) + 10);
                scores.put("aria-label", scores.get("aria-label") + 10);
            }
        }
        
        // Sort by score (descending)
        return scores.entrySet().stream()
            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
            .map(Map.Entry::getKey)
            .toList();
    }
    
    /**
     * Get best single strategy
     */
    public String predictBestStrategy(String pageDomain, String elementName, String elementType) {
        List<String> strategies = predictStrategies(pageDomain, elementName, elementType);
        return strategies.isEmpty() ? "id" : strategies.get(0);
    }
    
    /**
     * Learn from successful match (for future ML)
     */
    public void recordSuccess(String pageDomain, String elementName, 
                             String elementType, String successfulStrategy) {
        // For now, just log - later this can update ML model
        logger.debug("Success: {} / {} ({}) → {}", 
            pageDomain, elementName, elementType, successfulStrategy);
    }
}
