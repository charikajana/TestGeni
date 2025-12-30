package automation.browser.actions.click;

import automation.browser.actions.common.BaseSemanticMatcher;
import automation.intelligence.IntentAnalyzer;
import automation.intelligence.StepIntent;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import java.util.*;

/**
 * Semantic matcher specifically for CLICK actions.
 * Handles modal-aware scoring, Close button penalties, Submit button detection.
 */
public class ClickSemanticMatcher extends BaseSemanticMatcher {
    
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
            logger.info("CLICK Best match: Score={} Text='{}'", bestScore, best.getText());
            
            if (bestScore >= SCORE_THRESHOLD) {
                return best.getLocator();
            } else {
                logger.warn("Best CLICK match score ({}) below threshold ({})", bestScore, SCORE_THRESHOLD);
            }
        }
        
        return null;
    }
    
    private List<ScoredElement> findCandidates(Page page, StepIntent intent) {
        List<ScoredElement> elements = new ArrayList<>();
        
        // Focus on clickable elements and labels (associated with radio/checkboxes)
        String[] selectors = {"button", "a", "input", "select", "[role='button']", "[onclick]", "label"};
        
        for (String selector : selectors) {
            try {
                Locator locator = page.locator(selector);
                int count = Math.min(locator.count(), 5);
                
                for (int i = 0; i < count; i++) {
                    if (elements.size() >= MAX_CANDIDATES) break;
                    try {
                        Locator elem = locator.nth(i);
                        elements.add(new ScoredElement(elem, getElementType(elem)));
                    } catch (Exception ignored) {}
                }
            } catch (Exception e) {
                logger.debug("Error finding {} elements: {}", selector, e.getMessage());
            }
            
            if (elements.size() >= MAX_CANDIDATES) break;
        }
        
        return elements;
    }
    
    private String getElementType(Locator elem) {
        try {
            String tagName = (String) elem.evaluate("el => el.tagName.toLowerCase()");
            if ("button".equals(tagName)) return "button";
            if ("a".equals(tagName)) return "link";
            if ("input".equals(tagName)) {
                String type = (String) elem.evaluate("el => el.type");
                if ("submit".equals(type) || "button".equals(type)) return "button";
                return "input";
            }
            return tagName;
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    /**
     * CLICK-specific scoring logic
     */
    private double scoreCandidate(ScoredElement candidate, StepIntent intent) {
        double score = 0.0;
        String candidateText = candidate.getText();
        String targetDesc = intent.getTargetDescription();
        
        if (targetDesc != null && !targetDesc.isEmpty()) {
            if (candidateText == null || candidateText.trim().isEmpty()) {
                score -= 30;  // Penalize empty buttons for click
            } else {
                double textScore = scoreTextSimilarity(candidateText, targetDesc);
                score += textScore * 40;
                
                if (candidateText.equalsIgnoreCase(targetDesc)) {
                    score += 20;
                } else if (candidateText.toLowerCase().contains(targetDesc.toLowerCase())) {
                    score += 10;
                }
            }
            
            String targetLower = targetDesc.toLowerCase();
            
            // MODAL-AWARE SCORING for Submit/Save/OK buttons
            if (targetLower.contains("submit") || targetLower.contains("save") || targetLower.contains("ok")) {
                
                //CRITICAL: Heavily penalize Close/Cancel buttons
                try {
                    String btnText = (String) candidate.getLocator().evaluate("el => el.textContent.trim().toLowerCase() || ''");
                    String btnId = (String) candidate.getLocator().evaluate("el => el.id.toLowerCase() || ''");
                    String btnClass = (String) candidate.getLocator().evaluate("el => el.className.toLowerCase() || ''");
                    
                    boolean isCloseButton = btnText.contains("close") || btnText.contains("cancel") || btnText.contains("×") ||
                                          btnId.contains("close") || btnId.contains("cancel") ||
                                          btnClass.contains("close") || btnClass.contains("cancel");
                    
                    if (isCloseButton) {
                        score -= 100;  // MASSIVE penalty for close/cancel buttons
                    }
                } catch (Exception ignored) {}
                
                // Check for modal and boost submit buttons inside modals
                try {
                    Boolean hasVisibleModal = (Boolean) candidate.getLocator().evaluate(
                        "() => { " +
                        "  const modal = document.querySelector('[role=\"dialog\"]:not([style*=\"display: none\"]), .modal:not([style*=\"display: none\"])'); " +
                        "  return modal !== null; " +
                        "}"
                    );
                    
                    if (hasVisibleModal) {
                        Boolean isInsideModal = (Boolean) candidate.getLocator().evaluate(
                            "el => { " +
                            "  const modal = document.querySelector('[role=\"dialog\"]:not([style*=\"display: none\"]), .modal:not([style*=\"display: none\"])'); " +
                            "  return modal && modal.contains(el); " +
                            "}"
                        );
                        
                        if (isInsideModal) {
                            // Only boost actual submit buttons
                            String btnType = (String) candidate.getLocator().evaluate("el => el.type || ''");
                            String btnId2 = (String) candidate.getLocator().evaluate("el => el.id || ''");
                            String btnClass2 = (String) candidate.getLocator().evaluate("el => el.className || ''");
                            String btnText2 = (String) candidate.getLocator().evaluate("el => el.textContent.trim().toLowerCase() || ''");
                            
                            boolean isSubmitButton = btnType.equalsIgnoreCase("submit") ||
                                                   btnId2.toLowerCase().contains("submit") ||
                                                   btnClass2.toLowerCase().contains("submit") ||
                                                   btnText2.contains("submit");
                            
                            if (isSubmitButton) {
                                score += 150;  // MASSIVE boost for submit buttons in modal
                            }
                        } else {
                            score -= 80;  // Penalty for buttons outside modal
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
        
        // Type match scoring
        if (intent.getElementType() != null) {
            score += scoreTypeSimilarity(candidate.getType(), intent.getElementType()) * 30;
        } else {
            score += scoreActionTypeAffinity(candidate.getType(), IntentAnalyzer.ActionType.CLICK) * 30;
        }
        
        return score;
    }
}
