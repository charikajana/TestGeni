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
        automation.browser.locator.core.DomScanner scanner = new automation.browser.locator.core.DomScanner();
        List<automation.browser.locator.core.ElementCandidate> scanResults = scanner.scan(page);
        
        List<ScoredElement> elements = new ArrayList<>();
        String targetValue = intent.getValue();
        String targetDesc = intent.getTargetDescription();
        
        if (targetValue == null || targetValue.isEmpty()) {
            targetValue = targetDesc;
        }

        for (automation.browser.locator.core.ElementCandidate c : scanResults) {
            // VERIFY can apply to almost anything, but we prefer visible elements with text
            if (c.visible && c.text != null && !c.text.trim().isEmpty()) {
                elements.add(new ScoredElement(page.locator("xpath=" + c.xpath).first(), c));
                if (elements.size() >= MAX_CANDIDATES) break;
            }
        }
        
        logger.debug("Found {} candidates for VERIFY action using DomScanner", elements.size());
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
                score += 200; // Increased
            }
            // Contains match
            else if (textLower.contains(targetLower)) {
                score += 100; // Increased
                
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
            
            String tagName = candidate.getType().toLowerCase();
            String className = candidate.getClassName().toLowerCase();
            
            // Boost for heading elements (common verification targets)
            if (tagName.matches("h[1-6]")) {
                score += 30;
            }
            
            // Boost for alert-like classes/text
            if (className.contains("message") || className.contains("notification") || className.contains("alert") || className.contains("success")) {
                score += 30;
            }
        }
        
        // Type affinity scoring
        score += scoreActionTypeAffinity(candidate.getType(), IntentAnalyzer.ActionType.VERIFY) * 20;
        
        return score;
    }
}
