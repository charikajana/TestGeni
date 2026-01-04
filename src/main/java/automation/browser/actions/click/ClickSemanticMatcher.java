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
        long startTime = System.currentTimeMillis();
        List<ScoredElement> candidates = findCandidates(page, intent);
        
        if (candidates.isEmpty()) {
            logger.debug("No candidates found for: {}", intent.getTargetDescription());
            return null;
        }
        
        // Score each candidate (OFFLINE SCORING - very fast)
        Map<ScoredElement, Double> scores = new HashMap<>();
        for (ScoredElement candidate : candidates) {
            double score = scoreCandidate(candidate, intent);
            scores.put(candidate, score);
        }
        
        // Find top candidates (only do expensive JS on top contenders if needed)
        List<ScoredElement> topContenders = scores.entrySet().stream()
            .sorted(Map.Entry.<ScoredElement, Double>comparingByValue().reversed())
            .limit(3)
            .map(Map.Entry::getKey)
            .toList();
            
        if (topContenders.isEmpty()) return null;
        
        ScoredElement best = topContenders.get(0);
        double bestScore = scores.get(best);
        
        // Perform one final VITAL check: Is it in a modal? (only if it's a submit-like action)
        bestScore = performActionSpecificRefinement(best, bestScore, intent);
        
        logger.info("CLICK Best match: Score={} Text='{}' (Analysis took {}ms)", 
            bestScore, best.getText(), (System.currentTimeMillis() - startTime));
        
        if (bestScore >= SCORE_THRESHOLD) {
            return best.getLocator();
        } else {
            logger.warn("Best CLICK match score ({}) below threshold ({})", bestScore, SCORE_THRESHOLD);
        }
        
        return null;
    }
    
    private List<ScoredElement> findCandidates(Page page, StepIntent intent) {
        automation.browser.locator.core.DomScanner scanner = new automation.browser.locator.core.DomScanner();
        List<automation.browser.locator.core.ElementCandidate> scanResults = scanner.scan(page);
        
        List<ScoredElement> elements = new ArrayList<>();
        Set<String> clickableTags = new HashSet<>(Arrays.asList("button", "a", "input", "select", "label", "li"));
        
        for (automation.browser.locator.core.ElementCandidate c : scanResults) {
            // Filter candidates offline based on tags and roles
            // Must be a clickable tag OR have button role, AND not be hidden
            boolean isClickable = (clickableTags.contains(c.tag) || "button".equalsIgnoreCase(c.role)) &&
                                 !c.className.toLowerCase().contains("hidden");
            
            if (isClickable) {
                elements.add(new ScoredElement(page.locator("xpath=" + c.xpath).first(), c));
                if (elements.size() >= MAX_CANDIDATES) break;
            }
        }
        
        return elements;
    }
    
    /**
     * CLICK-specific scoring logic (Offline-safe)
     */
    private double scoreCandidate(ScoredElement candidate, StepIntent intent) {
        double score = 0.0;
        String candidateText = candidate.getText();
        String targetDesc = intent.getTargetDescription();
        
        if (targetDesc != null && !targetDesc.isEmpty()) {
            if (candidateText == null || candidateText.trim().isEmpty()) {
                score -= 30;  // Penalize empty elements for click
            } else {
                double textScore = scoreTextSimilarity(candidateText, targetDesc);
                score += textScore * 40;
                
                if (candidateText.equalsIgnoreCase(targetDesc)) {
                    score += 20;
                } else if (candidateText.toLowerCase().contains(targetDesc.toLowerCase())) {
                    score += 10;
                }
            }
            
            // Penalty for Close/Cancel buttons if we are looking for something else
            String targetLower = targetDesc.toLowerCase();
            String btnTextLower = candidateText.toLowerCase();
            String btnIdLower = candidate.getId().toLowerCase();
            String btnClassLower = candidate.getClassName().toLowerCase();
            
            boolean looksLikeClose = btnTextLower.contains("close") || btnTextLower.contains("cancel") || btnTextLower.contains("×") ||
                                    btnIdLower.contains("close") || btnIdLower.contains("cancel") ||
                                    btnClassLower.contains("close") || btnClassLower.contains("cancel");
                                    
            if (looksLikeClose && !targetLower.contains("close") && !targetLower.contains("cancel")) {
                score -= 50;
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
    
    /**
     * Only perform expensive browser side checks (like modal detection) for the top contender 
     * to prevent hanging.
     */
    private double performActionSpecificRefinement(ScoredElement candidate, double currentScore, StepIntent intent) {
        String targetLower = intent.getTargetDescription().toLowerCase();
        
        // Only refine for Submit/Save/OK buttons or in suspected modal scenarios
        if (targetLower.contains("submit") || targetLower.contains("save") || targetLower.contains("ok") || targetLower.contains("continue")) {
            try {
                // Combined check to minimize RPC
                Map<String, Object> state = (Map<String, Object>) candidate.getLocator().evaluate(
                    "el => { " +
                    "  const modal = document.querySelector('[role=\"dialog\"]:not([style*=\"display: none\"]), .modal:not([style*=\"display: none\"])'); " +
                    "  return { " +
                    "    hasModal: modal !== null, " +
                    "    isInside: modal && modal.contains(el) " +
                    "  }; " +
                    "}"
                );
                
                if (Boolean.TRUE.equals(state.get("hasModal"))) {
                    if (Boolean.TRUE.equals(state.get("isInside"))) {
                        return currentScore + 100; // Boost
                    } else {
                        return currentScore - 80; // Penalize buttons outside visible modal
                    }
                }
            } catch (Exception ignored) {}
        }
        return currentScore;
    }
}
