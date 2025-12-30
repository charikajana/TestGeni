package automation.browser.actions.input;

import automation.browser.actions.common.BaseSemanticMatcher;
import automation.intelligence.IntentAnalyzer;
import automation.intelligence.StepIntent;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import java.util.*;

/**
 * Semantic matcher specifically for FILL actions.
 * Prioritizes empty input fields, checks labels, penalizes table cells.
 */
public class FillSemanticMatcher extends BaseSemanticMatcher {
    
    private static final int SCORE_THRESHOLD = 50;
    private static final int MAX_CANDIDATES = 30;
    
    @Override
    public Locator findBestMatch(Page page, StepIntent intent) {
        List<ScoredElement> candidates = findCandidates(page, intent);
        
        if (candidates.isEmpty()) {
            logger.debug("No candidates found for: {}", intent.getTargetDescription());
            return null;
        }
        
        // Score each candidate
        Map<ScoredElement, Double> scores = new HashMap<>();
        for (ScoredElement candidate : candidates) {
            double score = scoreCandidate(candidate, intent);
            scores.put(candidate, score);
        }
        
        // Find best match
        ScoredElement best = scores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
        
        if (best != null) {
            double bestScore = scores.get(best);
            
            // DEBUG: Show which element was matched
            try {
                String elemId = (String) best.getLocator().evaluate("el => el.id || 'NO_ID'");
                String elemName = (String) best.getLocator().evaluate("el => el.name || 'NO_NAME'");
                String elemPlaceholder = (String) best.getLocator().evaluate("el => el.placeholder || 'NO_PLACEHOLDER'");
                String elemType = (String) best.getLocator().evaluate("el => el.type || 'NO_TYPE'");
                
                logger.info("FILL Best match: Score={} ID='{}' Name='{}' Placeholder='{}' Type='{}' Text='{}'", 
                    bestScore, elemId, elemName, elemPlaceholder, elemType, best.getText());
            } catch (Exception e) {
                logger.info("FILL Best match: Score={} Text='{}'", bestScore, best.getText());
            }
            
            // Show top 3 candidates for comparison
            logger.debug("Top 3 FILL candidates:");
            scores.entrySet().stream()
                .sorted(Map.Entry.<ScoredElement, Double>comparingByValue().reversed())
                .limit(3)
                .forEach(entry -> {
                    try {
                        String id = (String) entry.getKey().getLocator().evaluate("el => el.id || ''");
                        String name = (String) entry.getKey().getLocator().evaluate("el => el.name || ''");
                        logger.debug("  Score={} ID='{}' Name='{}'", entry.getValue(), id, name);
                    } catch (Exception ignored) {}
                });
            
            if (bestScore >= SCORE_THRESHOLD) {
                return best.getLocator();
            } else {
                logger.warn("Best FILL match score ({}) below threshold ({})", bestScore, SCORE_THRESHOLD);
            }
        }
        
        return null;
    }
    
    private List<ScoredElement> findCandidates(Page page, StepIntent intent) {
        List<ScoredElement> elements = new ArrayList<>();
        
        // DEBUG: Log which page we're scanning
        try {
            String currentUrl = page.url();
            logger.info("SCANNING PAGE: {}", currentUrl);
        } catch (Exception e) {
            logger.warn("Could not get page URL: {}", e.getMessage());
        }
        
        // Focus on input elements AND contenteditable elements for FILL actions
        // Many modern web apps use contenteditable divs for inline editing (e.g., table cells)
        String[] selectors = {"input", "textarea", "select", "[contenteditable='true']", "[contenteditable]"};
        
        for (String selector : selectors) {
            try {
                // CRITICAL FIX: Use .all() to get ALL elements (visible + hidden)
                // Playwright's page.locator() by default only returns VISIBLE elements
                // This was causing us to miss form fields that are styled as hidden/off-screen
                Locator locator = page.locator(selector);
                List<Locator> allElements = locator.all(); // Returns ALL matching elements!
                int count = Math.min(allElements.size(), 30);
                
                for (int i = 0; i < count; i++) {
                    if (elements.size() >= MAX_CANDIDATES) break;
                    try {
                        Locator elem = allElements.get(i);
                        
                        // DEBUG: Log what we're finding
                        try {
                            String elemId = (String) elem.evaluate("el => el.id || ''");
                            String elemName = (String) elem.evaluate("el => el.name || ''");
                            String elemPlaceholder = (String) elem.evaluate("el => el.placeholder || ''");
                            String elemTag = (String) elem.evaluate("el => el.tagName.toLowerCase()");
                            logger.debug("  Found {} element: id='{}', name='{}', placeholder='{}'", 
                                elemTag, elemId, elemName, elemPlaceholder);
                        } catch (Exception ignored) {}
                        
                        elements.add(new ScoredElement(elem, selector));
                    } catch (Exception ignored) {}
                }
            } catch (Exception e) {
                logger.debug("Error finding {} elements: {}", selector, e.getMessage());
            }
            
            if (elements.size() >= MAX_CANDIDATES) break;
        }
        
        logger.debug("Found {} input candidates for FILL action", elements.size());
        return elements;
    }
    
    /**
     * FILL-specific scoring logic
     */
    private double scoreCandidate(ScoredElement candidate, StepIntent intent) {
        double score = 0.0;
        String candidateText = candidate.getText();
        String targetDesc = intent.getTargetDescription();
        
        if (targetDesc != null && !targetDesc.isEmpty()) {
            boolean hasText = candidateText != null && !candidateText.trim().isEmpty();
            
            if (!hasText) {
                score += 0;  // Neutral - empty fields are normal for filling
            } else {
                double textScore = scoreTextSimilarity(candidateText, targetDesc);
                score += textScore * 40;
                
                if (candidateText.equalsIgnoreCase(targetDesc)) {
                    score += 20;
                } else if (candidateText.toLowerCase().contains(targetDesc.toLowerCase())) {
                    score += 10;
                }
                
                // CRITICAL: Heavily penalize elements with unrelated text
                if (hasText && !candidateText.toLowerCase().contains(targetDesc.toLowerCase())) {
                    score -= 60;  // Probably existing data, not an input field
                }
            }
            
            // Check all ways a field can be labeled
            try {
                String elemType = (String) candidate.getLocator().evaluate("el => el.type || ''");
                String placeholder = (String) candidate.getLocator().evaluate("el => el.placeholder || ''");
                String name = (String) candidate.getLocator().evaluate("el => el.name || ''");
                String id = (String) candidate.getLocator().evaluate("el => el.id || ''");
                String ariaLabel = (String) candidate.getLocator().evaluate("el => el.getAttribute('aria-label') || ''");
                String tagName = (String) candidate.getLocator().evaluate("el => el.tagName.toLowerCase() || ''");
                Boolean isDisabled = (Boolean) candidate.getLocator().evaluate("el => el.disabled || el.readOnly");
                
                // Check for associated <label>
                String associatedLabel = (String) candidate.getLocator().evaluate(
                    "el => { " +
                    "  const label = el.id ? document.querySelector(`label[for='${el.id}']`) : null; " +
                    "  return label ? label.textContent.trim() : ''; " +
                    "}"
                );
                
                String targetLower = targetDesc.toLowerCase();
                boolean isEmpty = !hasText;
                boolean isInputElement = tagName.equals("input") || tagName.equals("textarea") || tagName.equals("select");
                
                // CRITICAL: Match field description to element attributes (HIGHEST PRIORITY!)
                // Priority: 1. Exact/Full Match, 2. ID, 3. Name, 4. Label, 5. Placeholder
                
                String targetLowerNoSpaces = targetLower.replaceAll("\\s+", "");
                String idLower = id.toLowerCase();
                String nameLower = name.toLowerCase();
                String labelLower = associatedLabel != null ? associatedLabel.toLowerCase() : "";
                String placeholderLower = placeholder.toLowerCase();
                
                // HIGHEST PRIORITY: Check for exact/compound matches WITHOUT spaces
                // This ensures "First Name" -> "firstName" gets highest score, not "lastName"
                if (idLower.equals(targetLowerNoSpaces) || idLower.replaceAll("[-_]", "").equals(targetLowerNoSpaces)) {
                    score += 300;  // Exact ID match without spaces (e.g., "firstname" == "firstName")
                    logger.debug("EXACT ID match '{}' == '{}' - added +300", id, targetDesc);
                }
                
                if (nameLower.equals(targetLowerNoSpaces) || nameLower.replaceAll("[-_]", "").equals(targetLowerNoSpaces)) {
                    score += 280;  // Exact name match without spaces
                    logger.debug("EXACT name match '{}' == '{}' - added +280", name, targetDesc);
                }
                
                if (labelLower.equals(targetLower) || labelLower.equals(targetLowerNoSpaces)) {
                    score += 250;  // Exact label match (with or without spaces)
                    logger.debug("EXACT label match '{}' == '{}' - added +250", associatedLabel, targetDesc);
                }
                
                if (placeholderLower.equals(targetLower) || placeholderLower.equals(targetLowerNoSpaces)) {
                    score += 240;  // Exact placeholder match (with or without spaces)
                    logger.debug("EXACT placeholder match '{}' == '{}' - added +240", placeholder, targetDesc);
                }
                
                // SECONDARY: Word-by-word matching (only if no exact match found)
                String[] targetWords = targetLower.split("\\s+");
                for (String word : targetWords) {
                    if (word.equals("field")) continue;  // Skip the word "field"
                    
                    // Check if ID contains this word (HIGH PRIORITY but less than exact match)
                    if (idLower.contains(word)) {
                        score += 60;  // Reduced from 120 - word match is weaker than exact match
                        logger.debug("ID '{}' contains target word '{}' - added +60", id, word);
                    }
                    
                    // Check if name contains this word
                    if (nameLower.contains(word)) {
                        score += 50;  // Reduced from 100
                        logger.debug("Name '{}' contains target word '{}' - added +50", name, word);
                    }
                    
                    // Check if associated label contains this word
                    if (!labelLower.isEmpty() && labelLower.contains(word)) {
                        score += 45;  // Reduced from 90
                        logger.debug("Label '{}' contains target word '{}' - added +45", labelLower, word);
                    }
                    
                    // Check if placeholder contains this word
                    if (placeholderLower.contains(word)) {
                        score += 40;  // Reduced from 80
                        logger.debug("Placeholder '{}' contains target word '{}' - added +40", placeholder, word);
                    }
                }
                
                // CRITICAL: Check if input is inside a visible modal - prioritize modal inputs!
                try {
                    Boolean isInModal = (Boolean) candidate.getLocator().evaluate(
                        "el => { " +
                        "  const modal = el.closest('[role=\"dialog\"], .modal'); " +
                        "  return modal !== null && getComputedStyle(modal).display !== 'none'; " +
                        "}"
                    );
                    
                    if (isInModal) {
                        score += 80;  // HUGE boost for inputs inside visible modals!
                        logger.debug("Input '{}' is inside modal - added +80 boost", id);
                    }
                } catch (Exception ignored) {}
                
                // MASSIVE boost for empty input elements
                if (isInputElement && isEmpty) {
                    score += 50;
                }
                
                // Heavy penalty for non-input elements when searching for "field"
                if (targetLower.contains("field") && !isInputElement) {
                    score -= 70;
                }
                
                // Check associated label (highest priority)
                if (associatedLabel != null && !associatedLabel.isEmpty() && 
                    associatedLabel.toLowerCase().contains(targetLower)) {
                    score += 40;
                }
                
                // Check aria-label
                if (ariaLabel.toLowerCase().contains(targetLower)) {
                    score += 35;
                }
                
                // Check placeholder/name/id
                if (placeholder.toLowerCase().contains(targetLower) ||
                    name.toLowerCase().contains(targetLower) ||
                    id.toLowerCase().contains(targetLower)) {
                    score += 30;
                }
                
                // Penalize disabled fields
                if (isDisabled) {
                    score -= 40;
                }
                
                // Email field special case
                if (targetLower.contains("email")) {
                    if (elemType.equalsIgnoreCase("email") || placeholder.contains("@")) {
                        score += 30;
                    }
                }
            } catch (Exception ignored) {}
        }
        
        // Type match scoring
        if (intent.getElementType() != null) {
            score += scoreTypeSimilarity(candidate.getType(), intent.getElementType()) * 30;
        } else {
            score += scoreActionTypeAffinity(candidate.getType(), IntentAnalyzer.ActionType.FILL) * 30;
        }
        
        return score;
    }
}
