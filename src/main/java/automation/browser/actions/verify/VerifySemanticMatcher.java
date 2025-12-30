package automation.browser.actions.verify;

import automation.browser.actions.common.BaseSemanticMatcher;
import automation.intelligence.IntentAnalyzer;
import automation.intelligence.StepIntent;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import java.util.*;

/**
 * Semantic matcher specifically for VERIFY actions.
 * Searches for elements containing specific text or having specific attributes.
 */
public class VerifySemanticMatcher extends BaseSemanticMatcher {
    
    private static final int SCORE_THRESHOLD = 40;  // Lower threshold for verify - text match is key
    private static final int MAX_CANDIDATES = 50;
    
    @Override
    public Locator findBestMatch(Page page, StepIntent intent) {
        List<ScoredElement> candidates = findCandidates(page, intent);
        
        if (candidates.isEmpty()) {
            logger.debug("No candidates found for verification: {}", intent.getTargetDescription());
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
            logger.info("VERIFY Best match: Score={} Text='{}'", bestScore, best.getText());
            
            if (bestScore >= SCORE_THRESHOLD) {
                return best.getLocator();
            } else {
                logger.warn("Best VERIFY match score ({}) below threshold ({})", bestScore, SCORE_THRESHOLD);
            }
        }
        
        return null;
    }
    
    private List<ScoredElement> findCandidates(Page page, StepIntent intent) {
        List<ScoredElement> elements = new ArrayList<>();
        String targetValue = intent.getValue();
        
        if (targetValue == null || targetValue.isEmpty()) {
            logger.debug("No value to verify - using broad search");
            targetValue = intent.getTargetDescription();
        }
        
        // Strategy 1: Find elements containing the target text
        try {
            // Use text locator for initial candidates
            String searchText = targetValue;
            Locator textMatches = page.locator(String.format("text='%s'", searchText));
            int count = Math.min(textMatches.count(), 20);
            
            for (int i = 0; i < count; i++) {
                if (elements.size() >= MAX_CANDIDATES) break;
                try {
                    Locator elem = textMatches.nth(i);
                    elements.add(new ScoredElement(elem, "text-match"));
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            logger.debug("Text search failed: {}", e.getMessage());
        }
        
        // Strategy 2: Find by common visible elements
        String[] selectors = {
            "h1, h2, h3, h4, h5, h6",  // Headings
            "p",                         // Paragraphs
            "span",                      // Spans
            "div",                       // Divs
            "label",                     // Labels
            "[role='heading']",          // ARIA headings
            "[role='alert']",            // Alerts
            ".message, .notification"    // Common message classes
        };
        
        for (String selector : selectors) {
            if (elements.size() >= MAX_CANDIDATES) break;
            
            try {
                Locator locator = page.locator(selector);
                int count = Math.min(locator.count(), 10);
                
                for (int i = 0; i < count; i++) {
                    if (elements.size() >= MAX_CANDIDATES) break;
                    try {
                        Locator elem = locator.nth(i);
                        // Only add if it has some text content
                        String text = elem.textContent();
                        if (text != null && text.trim().length() > 0) {
                            elements.add(new ScoredElement(elem, selector));
                        }
                    } catch (Exception ignored) {}
                }
            } catch (Exception e) {
                logger.debug("Error finding {} elements: {}", selector, e.getMessage());
            }
        }
        
        logger.debug("Found {} candidates for VERIFY action", elements.size());
        return elements;
    }
    
    private double scoreCandidate(ScoredElement candidate, StepIntent intent) {
        double score = 0.0;
        String candidateText = candidate.getText();
        String targetDesc = intent.getTargetDescription();
        String targetValue = intent.getValue();
        
        // For verify, we want to match the VALUE being verified
        String searchTarget = (targetValue != null && !targetValue.isEmpty()) ? targetValue : targetDesc;
        
        if (candidateText == null || candidateText.trim().isEmpty()) {
            return -50;  // Heavy penalty for empty elements in verification
        }
        
        if (searchTarget != null && !searchTarget.isEmpty()) {
            String textLower = candidateText.toLowerCase();
            String targetLower = searchTarget.toLowerCase();
            
            // Exact match gets highest score
            if (candidateText.equalsIgnoreCase(searchTarget)) {
                score += 100;
            }
            // Contains match
            else if (textLower.contains(targetLower)) {
                score += 80;
                
                // Bonus if it's a close match (target is significant portion of text)
                double ratio = (double) targetLower.length() / textLower.length();
                if (ratio > 0.5) {
                    score += 20;  // Target is more than half the text
                }
            }
            // Partial word match
            else {
                double textScore = scoreTextSimilarity(candidateText, searchTarget);
                score += textScore * 40;
            }
            
            try {
                String tagName = (String) candidate.getLocator().evaluate("el => el.tagName.toLowerCase() || ''");
                String role = (String) candidate.getLocator().evaluate("el => el.getAttribute('role') || ''");
                String className = (String) candidate.getLocator().evaluate("el => el.className || ''");
                
                // Boost for heading elements (common verification targets)
                if (tagName.matches("h[1-6]")) {
                    score += 30;
                }
                
                // Boost for ARIA roles
                if (role.equals("heading") || role.equals("alert") || role.equals("status")) {
                    score += 25;
                }
                
                // Boost for message/notification classes
                if (className.toLowerCase().contains("message") || 
                    className.toLowerCase().contains("notification") ||
                    className.toLowerCase().contains("alert")) {
                    score += 20;
                }
                
                // Check visibility - penalize hidden elements
                Boolean isVisible = (Boolean) candidate.getLocator().evaluate(
                    "el => { " +
                    "  const style = getComputedStyle(el); " +
                    "  return style.display !== 'none' && style.visibility !== 'hidden' && style.opacity !== '0'; " +
                    "}"
                );
                
                if (isVisible) {
                    score += 15;  // Visible elements preferred
                } else {
                    score -= 50;  // Heavy penalty for hidden elements
                }
                
                // Boost for elements in modals (if verifying modal content)
                try {
                    Boolean isInModal = (Boolean) candidate.getLocator().evaluate(
                        "el => { " +
                        "  const modal = el.closest('[role=\"dialog\"], .modal'); " +
                        "  return modal !== null && getComputedStyle(modal).display !== 'none'; " +
                        "}"
                    );
                    
                    if (isInModal && targetDesc != null && 
                        (targetDesc.toLowerCase().contains("modal") || 
                         targetDesc.toLowerCase().contains("dialog") ||
                         targetDesc.toLowerCase().contains("form"))) {
                        score += 40;  // Boost if looking for modal content
                    }
                } catch (Exception ignored) {}
                
            } catch (Exception ignored) {}
        }
        
        // Type affinity scoring
        score += scoreActionTypeAffinity(candidate.getType(), IntentAnalyzer.ActionType.VERIFY) * 20;
        
        return score;
    }
}
