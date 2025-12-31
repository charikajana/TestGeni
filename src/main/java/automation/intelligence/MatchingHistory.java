package automation.intelligence;

import automation.utils.LoggerUtil;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Learning system that tracks successful matches and improves scoring over time.
 * 
 * Features:
 * - Records successful element matches
 * - Tracks scoring patterns
 * - Adjusts future scoring based on history
 * - Persists learning data to disk
 * - Thread-safe for concurrent execution
 */
public class MatchingHistory {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(MatchingHistory.class);
    private static MatchingHistory instance;
    
    // Learning data storage
    private Map<String, MatchRecord> successfulMatches;
    private Map<String, Double> actionTypeWeights;
    private Map<String, Double> elementTypeWeights;
    
    // Configuration
    private static final String HISTORY_FILE = "CacheLocatorRepository/matching_history.dat";
    private static final int MAX_HISTORY_SIZE = 1000;
    private static final double LEARNING_RATE = 0.1;  // How quickly to adapt
    
    private MatchingHistory() {
        this.successfulMatches = new ConcurrentHashMap<>();
        this.actionTypeWeights = new ConcurrentHashMap<>();
        this.elementTypeWeights = new ConcurrentHashMap<>();
        
        // Initialize default weights
        initializeDefaultWeights();
        
        // Load previous learning data
        loadHistory();
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized MatchingHistory getInstance() {
        if (instance == null) {
            instance = new MatchingHistory();
        }
        return instance;
    }
    
    /**
     * Record a successful match
     */
    public void recordSuccess(String stepDescription, String elementText, 
                              String elementType, String actionType, double matchScore) {
        try {
            String key = generateKey(stepDescription, elementText);
            
            MatchRecord record = successfulMatches.computeIfAbsent(key, 
                k -> new MatchRecord(stepDescription, elementText, elementType, actionType));
            
            record.incrementSuccessCount();
            record.updateAverageScore(matchScore);
            
            // Update weights based on this success
            updateWeights(elementType, actionType, matchScore);
            
            logger.debug("Recorded successful match: {} → {} (score: {})", 
                stepDescription, elementText, matchScore);
            
            // Periodically save history
            if (successfulMatches.size() % 10 == 0) {
                saveHistory();
            }
            
        } catch (Exception e) {
            logger.debug("Error recording success: {}", e.getMessage());
        }
    }
    
    /**
     * Get scoring boost based on history
     */
    public double getScoringBoost(String stepDescription, String elementText, 
                                  String elementType, String actionType) {
        double boost = 0.0;
        
        // Check if we've seen this exact match before
        String key = generateKey(stepDescription, elementText);
        MatchRecord record = successfulMatches.get(key);
        
        if (record != null && record.getSuccessCount() > 0) {
            // Give strong boost for exact match history
            boost += record.getAverageScore() * 0.2 * Math.min(record.getSuccessCount(), 5);
            logger.debug("Exact history boost for '{}' → {}: +{}", stepDescription, elementText, boost);
        }
        
        // GENERALIZED LEARNING: Boost based on element type success patterns
        // This transfers learning from "full name" to "email", "address", etc.
        Double elementWeight = elementTypeWeights.get(elementType);
        if (elementWeight != null && elementWeight > 1.0) {
            double typeBoost = (elementWeight - 1.0) * 10.0;  // Increased from 5.0 to 10.0
            boost += typeBoost;
            logger.debug("Element type learning boost for '{}' type: +{}", elementType, typeBoost);
        }
        
        // GENERALIZED LEARNING: Boost based on action type patterns
        // If this action type has been successful before, boost it
        Double actionWeight = actionTypeWeights.get(actionType);
        if (actionWeight != null && actionWeight > 1.0) {
            double actionBoost = (actionWeight - 1.0) * 5.0;
            boost += actionBoost;
            logger.debug("Action type learning boost for '{}' action: +{}", actionType, actionBoost);
        }
        
        // CROSS-PATTERN LEARNING: Boost elements of same type that worked for same action
        // E.g., if "input + fill" worked before, boost all "input + fill" combinations
        int successfulSameTypeActionCount = countSuccessfulMatchesByTypeAndAction(elementType, actionType);
        if (successfulSameTypeActionCount > 0) {
            double patternBoost = Math.min(successfulSameTypeActionCount * 2.0, 10.0);
            boost += patternBoost;
            logger.debug("Cross-pattern boost ({} successful {} + {} matches): +{}", 
                successfulSameTypeActionCount, elementType, actionType, patternBoost);
        }
        
        return Math.min(boost, 25.0);  // Cap boost at 25 points (increased from 20)
    }
    
    /**
     * Count successful matches for a given element type and action type combination
     */
    private int countSuccessfulMatchesByTypeAndAction(String elementType, String actionType) {
        if (elementType == null || actionType == null) {
            return 0;
        }
        
        int count = 0;
        for (MatchRecord record : successfulMatches.values()) {
            if (elementType.equalsIgnoreCase(record.elementType) && 
                actionType.equalsIgnoreCase(record.actionType) &&
                record.getSuccessCount() > 0) {
                count += record.getSuccessCount();
            }
        }
        return count;
    }
    
    /**
     * Get learned weight for element type
     */
    public double getElementTypeWeight(String elementType) {
        return elementTypeWeights.getOrDefault(elementType, 1.0);
    }
    
    /**
     * Get learned weight for action type
     */
    public double getActionTypeWeight(String actionType) {
        return actionTypeWeights.getOrDefault(actionType, 1.0);
    }
    
    /**
     * Update weights based on successful match
     */
    private void updateWeights(String elementType, String actionType, double matchScore) {
        // Update element type weight  
        if (elementType != null) {
            double currentWeight = elementTypeWeights.getOrDefault(elementType, 1.0);
            double newWeight = currentWeight + (matchScore / 100.0 * LEARNING_RATE);
            elementTypeWeights.put(elementType, Math.min(newWeight, 2.0));  // Cap at 2.0
        }
        
        // Update action type weight
        if (actionType != null) {
            double currentWeight = actionTypeWeights.getOrDefault(actionType, 1.0);
            double newWeight = currentWeight + (matchScore / 100.0 * LEARNING_RATE);
            actionTypeWeights.put(actionType, Math.min(newWeight, 2.0));  // Cap at 2.0
        }
    }
    
    /**
     * Initialize default weights
     */
    private void initializeDefaultWeights() {
        // Element types
        elementTypeWeights.put("button", 1.0);
        elementTypeWeights.put("input", 1.0);
        elementTypeWeights.put("a", 1.0);
        elementTypeWeights.put("select", 1.0);
        
        // Action types
        actionTypeWeights.put("click", 1.0);
        actionTypeWeights.put("fill", 1.0);
        actionTypeWeights.put("verify", 1.0);
        actionTypeWeights.put("select", 1.0);
    }
    
    /**
     * Generate unique key for match
     */
    private String generateKey(String step, String element) {
        return (step + "|" + element).toLowerCase().replaceAll("\\s+", "_");
    }
    
    /**
     * Save learning history to disk
     */
    public void saveHistory() {
        try {
            File file = new File(HISTORY_FILE);
            file.getParentFile().mkdirs();
            
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
                oos.writeObject(successfulMatches);
                oos.writeObject(actionTypeWeights);
                oos.writeObject(elementTypeWeights);
            }
            
            logger.debug("Saved matching history ({} records)", successfulMatches.size());
            
        } catch (Exception e) {
            logger.debug("Could not save history: {}", e.getMessage());
        }
    }
    
    /**
     * Load learning history from disk
     */
    @SuppressWarnings("unchecked")
    private void loadHistory() {
        try {
            File file = new File(HISTORY_FILE);
            if (!file.exists()) {
                logger.debug("No history file found, starting fresh");
                return;
            }
            
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                successfulMatches = (Map<String, MatchRecord>) ois.readObject();
                actionTypeWeights = (Map<String, Double>) ois.readObject();
                elementTypeWeights = (Map<String, Double>) ois.readObject();
            }
            
            logger.info("Loaded matching history ({} records)", successfulMatches.size());
            
        } catch (Exception e) {
            logger.debug("Could not load history: {}", e.getMessage());
        }
    }
    
    /**
     * Get statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total_records", successfulMatches.size());
        stats.put("element_types_learned", elementTypeWeights.size());
        stats.put("action_types_learned", actionTypeWeights.size());
        
        // Top matched elements
        List<MatchRecord> topMatches = successfulMatches.values().stream()
            .sorted((a, b) -> Integer.compare(b.getSuccessCount(), a.getSuccessCount()))
            .limit(10)
            .toList();
        stats.put("top_matches", topMatches);
        
        return stats;
    }
    
    /**
     * Clear all learning data
     */
    public void clear() {
        successfulMatches.clear();
        actionTypeWeights.clear();
        elementTypeWeights.clear();
        initializeDefaultWeights();
        logger.info("Cleared all matching history");
    }
    
    /**
     * Record for a successful match
     */
    public static class MatchRecord implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private String stepDescription;
        private String elementText;
        private String elementType;
        private String actionType;
        private int successCount;
        private double averageScore;
        
        public MatchRecord(String stepDescription, String elementText, 
                          String elementType, String actionType) {
            this.stepDescription = stepDescription;
            this.elementText = elementText;
            this.elementType = elementType;
            this.actionType = actionType;
            this.successCount = 0;
            this.averageScore = 0.0;
        }
        
        public void incrementSuccessCount() {
            this.successCount++;
        }
        
        public void updateAverageScore(double newScore) {
            this.averageScore = (this.averageScore * (successCount - 1) + newScore) / successCount;
        }
        
        public int getSuccessCount() { return successCount; }
        public double getAverageScore() { return averageScore; }
        
        @Override
        public String toString() {
            return String.format("%s → %s (%s, score: %.1f, count: %d)",
                stepDescription, elementText, elementType, averageScore, successCount);
        }
    }
}
