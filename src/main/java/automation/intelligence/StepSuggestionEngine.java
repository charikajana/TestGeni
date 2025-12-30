package automation.intelligence;

import automation.utils.LoggerUtil;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides intelligent suggestions for rewriting unsupported steps
 * to match framework patterns.
 * 
 * Like "Did you mean...?" for test steps!
 */
public class StepSuggestionEngine {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(StepSuggestionEngine.class);
    
    /**
     * Generate suggestions for how to rewrite a step to match framework patterns
     * 
     * @param originalStep The step that doesn't match any pattern
     * @return List of suggested alternative phrasings (sorted by confidence)
     */
    public List<StepSuggestion> generateSuggestions(String originalStep) {
        List<StepSuggestion> suggestions = new ArrayList<>();
        
        if (originalStep == null || originalStep.trim().isEmpty()) {
            return suggestions;
        }
        
        String step = originalStep.toLowerCase().trim();
        
        // Detect what the user is trying to do
        if (detectClickIntent(step)) {
            suggestions.addAll(generateClickSuggestions(originalStep, step));
        }
        
        if (detectFillIntent(step)) {
            suggestions.addAll(generateFillSuggestions(originalStep, step));
        }
        
        if (detectSelectIntent(step)) {
            suggestions.addAll(generateSelectSuggestions(originalStep, step));
        }
        
        if (detectNavigateIntent(step)) {
            suggestions.addAll(generateNavigateSuggestions(originalStep, step));
        }
        
        if (detectVerifyIntent(step)) {
            suggestions.addAll(generateVerifySuggestions(originalStep, step));
        }
        
        // Sort by confidence (highest first)
        suggestions.sort((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()));
        
        // Return top 5
        return suggestions.subList(0, Math.min(5, suggestions.size()));
    }
    
    // ==========================================
    // INTENT DETECTION
    // ==========================================
    
    private boolean detectClickIntent(String step) {
        return step.matches(".*(click|press|tap|hit|push|select).*") ||
               step.matches(".*(button|link|menu|item).*");
    }
    
    private boolean detectFillIntent(String step) {
        return step.matches(".*(enter|type|fill|input|set|write).*") ||
               step.matches(".*(field|textbox|input|box).*");
    }
    
    private boolean detectSelectIntent(String step) {
        return step.matches(".*(select|choose|pick|dropdown|option).*");
    }
    
    private boolean detectNavigateIntent(String step) {
        return step.matches(".*(navigate|go to|open|visit|load).*") ||
               step.matches(".*(url|page|website|site).*");
    }
    
    private boolean detectVerifyIntent(String step) {
        return step.matches(".*(verify|check|assert|validate|see|should|expect).*");
    }
    
    // ==========================================
    // CLICK SUGGESTIONS
    // ==========================================
    
    private List<StepSuggestion> generateClickSuggestions(String original, String normalized) {
        List<StepSuggestion> suggestions = new ArrayList<>();
        
        // Extract element name from various patterns
        String elementName = extractElementName(normalized, 
            "click on (the )?(.+)",
            "press (the )?(.+)",
            "tap (the )?(.+)",
            "click (.+) button",
            "click (.+) link"
        );
        
        if (elementName != null) {
            suggestions.add(new StepSuggestion(
                String.format("When I click %s", capitalize(elementName)),
                "Standard click pattern",
                0.95
            ));
            
            suggestions.add(new StepSuggestion(
                String.format("And I click %s button", capitalize(elementName)),
                "Click button pattern",
                0.90
            ));
            
            suggestions.add(new StepSuggestion(
                String.format("Then I click on %s", capitalize(elementName)),
                "Alternative click pattern",
                0.85
            ));
        }
        
        return suggestions;
    }
    
    // ==========================================
    // FILL SUGGESTIONS
    // ==========================================
    
    private List<StepSuggestion> generateFillSuggestions(String original, String normalized) {
        List<StepSuggestion> suggestions = new ArrayList<>();
        
        // Try to extract value and field name
        Pattern pattern = Pattern.compile("(enter|type|fill|input).+['\"](.+?)['\"].+(in|into|to|for).+(the )?(.+)", 
            Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(original);
        
        if (matcher.find()) {
            String value = matcher.group(2);
            String fieldName = matcher.group(5).trim();
            
            suggestions.add(new StepSuggestion(
                String.format("When I enter \"%s\" in %s", value, capitalize(fieldName)),
                "Standard fill pattern (recommended)",
                0.95
            ));
            
            suggestions.add(new StepSuggestion(
                String.format("And I type \"%s\" in %s", value, capitalize(fieldName)),
                "Alternative fill pattern",
                0.90
            ));
            
            suggestions.add(new StepSuggestion(
                String.format("When I fill %s with \"%s\"", capitalize(fieldName), value),
                "Inverted fill pattern",
                0.85
            ));
        }
        
        return suggestions;
    }
    
    // ==========================================
    // SELECT SUGGESTIONS
    // ==========================================
    
    private List<StepSuggestion> generateSelectSuggestions(String original, String normalized) {
        List<StepSuggestion> suggestions = new ArrayList<>();
        
        Pattern pattern = Pattern.compile("(select|choose|pick).+['\"](.+?)['\"].+(from|in).+(the )?(.+)", 
            Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(original);
        
        if (matcher.find()) {
            String option = matcher.group(2);
            String dropdown = matcher.group(5).trim();
            
            suggestions.add(new StepSuggestion(
                String.format("When I select \"%s\" in %s", option, capitalize(dropdown)),
                "Standard select pattern (recommended)",
                0.95
            ));
            
            suggestions.add(new StepSuggestion(
                String.format("And I choose \"%s\" from %s", option, capitalize(dropdown)),
                "Alternative select pattern",
                0.90
            ));
        }
        
        return suggestions;
    }
    
    // ==========================================
    // NAVIGATE SUGGESTIONS
    // ==========================================
    
    private List<StepSuggestion> generateNavigateSuggestions(String original, String normalized) {
        List<StepSuggestion> suggestions = new ArrayList<>();
        
        Pattern pattern = Pattern.compile("['\"]?(https?://[^'\"\\s]+)['\"]?", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(original);
        
        if (matcher.find()) {
            String url = matcher.group(1);
            
            suggestions.add(new StepSuggestion(
                String.format("Given I navigate to \"%s\"", url),
                "Standard navigation pattern (recommended)",
                0.95
            ));
            
            suggestions.add(new StepSuggestion(
                String.format("When I open \"%s\"", url),
                "Alternative navigation pattern",
                0.90
            ));
        }
        
        return suggestions;
    }
    
    // ==========================================
    // VERIFY SUGGESTIONS
    // ==========================================
    
    private List<StepSuggestion> generateVerifySuggestions(String original, String normalized) {
        List<StepSuggestion> suggestions = new ArrayList<>();
        
        Pattern pattern = Pattern.compile("(verify|check|see).+['\"](.+?)['\"]", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(original);
        
        if (matcher.find()) {
            String expectedText = matcher.group(2);
            
            suggestions.add(new StepSuggestion(
                String.format("Then I should see \"%s\"", expectedText),
                "Standard verification pattern (recommended)",
                0.95
            ));
            
            suggestions.add(new StepSuggestion(
                String.format("And verify \"%s\" is displayed", expectedText),
                "Alternative verification pattern",
                0.90
            ));
        }
        
        return suggestions;
    }
    
    // ==========================================
    // HELPERS
    // ==========================================
    
    private String extractElementName(String step, String... patterns) {
        for (String patternStr : patterns) {
            Pattern p = Pattern.compile(patternStr);
            Matcher m = p.matcher(step);
            if (m.find()) {
                // Return the last captured group (element name)
                int groupCount = m.groupCount();
                if (groupCount > 0) {
                    String name = m.group(groupCount);
                    return name.replaceAll("(the |a |an )", "").trim();
                }
            }
        }
        return null;
    }
    
    private String capitalize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        // Capitalize first letter of each word
        String[] words = text.split("\\s+");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            if (word.length() > 0) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1).toLowerCase())
                      .append(" ");
            }
        }
        
        return result.toString().trim();
    }
    
    /**
     * Represents a single step suggestion
     */
    public static class StepSuggestion {
        private final String suggestedStep;
        private final String reason;
        private final double confidence;  // 0.0 to 1.0
        
        public StepSuggestion(String suggestedStep, String reason, double confidence) {
            this.suggestedStep = suggestedStep;
            this.reason = reason;
            this.confidence = confidence;
        }
        
        public String getSuggestedStep() {
            return suggestedStep;
        }
        
        public String getReason() {
            return reason;
        }
        
        public double getConfidence() {
            return confidence;
        }
        
        @Override
        public String toString() {
            return String.format("%.0f%% - %s (%s)", 
                confidence * 100, suggestedStep, reason);
        }
    }
}
