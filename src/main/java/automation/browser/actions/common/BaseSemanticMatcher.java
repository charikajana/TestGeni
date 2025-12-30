package automation.browser.actions.common;

import automation.intelligence.IntentAnalyzer;
import automation.intelligence.MatchingHistory;
import automation.intelligence.StepIntent;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import automation.utils.LoggerUtil;
import java.util.*;

/**
 * Base class for semantic matchers.
 * Contains shared scoring logic that all action-specific matchers can use.
 */
public abstract class BaseSemanticMatcher {
    
    protected static final LoggerUtil logger = LoggerUtil.getLogger(BaseSemanticMatcher.class);
    protected MatchingHistory history;
    
    public BaseSemanticMatcher() {
        this.history = MatchingHistory.getInstance();
    }
    
    /**
     * Main entry point for semantic matching.
     * Subclasses can override to add action-specific logic.
     */
    public abstract Locator findBestMatch(Page page, StepIntent intent);
    
    /**
     * Calculate text similarity score (0.0 to 1.0)
     */
    protected double scoreTextSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) return 0.0;
        
        String t1 = text1.toLowerCase().trim();
        String t2 = text2.toLowerCase().trim();
        
        if (t1.equals(t2)) return 1.0;
        if (t1.contains(t2) || t2.contains(t1)) return 0.8;
        
        // Simple word overlap scoring
        Set<String> words1 = new HashSet<>(Arrays.asList(t1.split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(t2.split("\\s+")));
        
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);
        
        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
    
    /**
     * Score type similarity (0.0 to 1.0)
     */
    protected double scoreTypeSimilarity(String actualType, String expectedType) {
        if (actualType == null || expectedType == null) return 0.0;
        
        String actual = actualType.toLowerCase();
        String expected = expectedType.toLowerCase();
        
        if (actual.equals(expected)) return 1.0;
        if (actual.contains(expected) || expected.contains(actual)) return 0.7;
        
        // Field types are flexible
        if (expected.equals("field") && (actual.equals("input") || actual.equals("textarea"))) {
            return 0.9;
        }
        
        return 0.0;
    }
    
    /**
     * Score action-type affinity (0.0 to 1.0)
     * How well does this element type match the action being performed?
     */
    protected double scoreActionTypeAffinity(String elementType, IntentAnalyzer.ActionType actionType) {
        if (elementType == null || actionType == null) return 0.5;
        
        String type = elementType.toLowerCase();
        
        switch (actionType) {
            case CLICK:
                if (type.equals("button") || type.equals("link")) return 1.0;
                if (type.contains("button") || type.contains("link")) return 0.8;
                return 0.5;
                
            case FILL:
                if (type.equals("input") || type.equals("textarea") || type.equals("field")) return 1.0;
                if (type.contains("input") || type.contains("field")) return 0.8;
                return 0.3;
                
            case VERIFY:
                return 0.7; // Most elements can be verified
                
            default:
                return 0.5;
        }
    }
    
    /**
     * Score visual attributes match (0.0 to 1.0)
     * Placeholder - can be enhanced with actual visual scoring
     */
    protected double scoreVisualMatch(ScoredElement candidate, StepIntent intent) {
        return 0.0; // Base implementation - override in subclasses if needed
    }
    
    /**
     * Score spatial relationships match (0.0 to 1.0)
     * Placeholder - can be enhanced with actual spatial scoring
     */
    protected double scoreSpatialMatch(ScoredElement candidate, StepIntent intent) {
        return 0.0; // Base implementation - override in subclasses if needed
    }
    
    /**
     * Helper class to store elements with their scores
     */
    public static class ScoredElement {
        private final Locator locator;
        private final String type;
        private String text;
        
        public ScoredElement(Locator locator, String type) {
            this.locator = locator;
            this.type = type;
        }
        
        public Locator getLocator() { return locator; }
        public String getType() { return type; }
        
        public String getText() {
            if (text == null) {
                try {
                    text = locator.textContent();
                } catch (Exception e) {
                    text = "";
                }
            }
            return text;
        }
    }
}
