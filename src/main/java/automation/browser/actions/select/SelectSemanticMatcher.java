package automation.browser.actions.select;

import automation.browser.actions.common.BaseSemanticMatcher;
import automation.intelligence.IntentAnalyzer;
import automation.intelligence.StepIntent;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import java.util.*;

/**
 * Semantic matcher specifically for SELECT actions.
 * Prioritizes <select> elements and custom dropdowns.
 */
public class SelectSemanticMatcher extends BaseSemanticMatcher {
    
    private static final int SCORE_THRESHOLD = 50;
    private static final int MAX_CANDIDATES = 20;
    
    @Override
    public Locator findBestMatch(Page page, StepIntent intent) {
        List<ScoredElement> candidates = findCandidates(page, intent);
        
        if (candidates.isEmpty()) {
            logger.debug("No select candidates found for: {}", intent.getTargetDescription());
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
            logger.info("SELECT Best match: Score={} Text='{}'", bestScore, best.getText());
            
            if (bestScore >= SCORE_THRESHOLD) {
                return best.getLocator();
            } else {
                logger.warn("Best SELECT match score ({}) below threshold ({})", bestScore, SCORE_THRESHOLD);
            }
        }
        
        return null;
    }
    
    private List<ScoredElement> findCandidates(Page page, StepIntent intent) {
        List<ScoredElement> elements = new ArrayList<>();
        
        // Focus on select elements and custom dropdowns
        String[] selectors = {
            "select",                           // Standard HTML select
            "[role='combobox']",                // ARIA combobox
            "[role='listbox']",                 // ARIA listbox
            ".select, .dropdown",               // Common custom dropdown classes
            "[class*='select']",                // Elements with 'select' in class
            "input[type='search']"              // Some dropdowns use search inputs
        };
        
        for (String selector : selectors) {
            try {
                Locator locator = page.locator(selector);
                int count = Math.min(locator.count(), 20);
                
                for (int i = 0; i < count; i++) {
                    if (elements.size() >= MAX_CANDIDATES) break;
                    try {
                        Locator elem = locator.nth(i);
                        elements.add(new ScoredElement(elem, "select"));
                    } catch (Exception ignored) {}
                }
            } catch (Exception e) {
                logger.debug("Error finding {} elements: {}", selector, e.getMessage());
            }
            
            if (elements.size() >= MAX_CANDIDATES) break;
        }
        
        logger.debug("Found {} select candidates", elements.size());
        return elements;
    }
    
    private double scoreCandidate(ScoredElement candidate, StepIntent intent) {
        double score = 0.0;
        String candidateText = candidate.getText();
        String targetDesc = intent.getTargetDescription();
        
        if (targetDesc != null && !targetDesc.isEmpty()) {
            // Text similarity
            if (candidateText != null && !candidateText.trim().isEmpty()) {
                double textScore = scoreTextSimilarity(candidateText, targetDesc);
                score += textScore * 40;
                
                if (candidateText.equalsIgnoreCase(targetDesc)) {
                    score += 20;
                } else if (candidateText.toLowerCase().contains(targetDesc.toLowerCase())) {
                    score += 10;
                }
            }
            
            try {
                String tagName = (String) candidate.getLocator().evaluate("el => el.tagName.toLowerCase() || ''");
                String role = (String) candidate.getLocator().evaluate("el => el.getAttribute('role') || ''");
                String name = (String) candidate.getLocator().evaluate("el => el.name || ''");
                String id = (String) candidate.getLocator().evaluate("el => el.id || ''");
                String ariaLabel = (String) candidate.getLocator().evaluate("el => el.getAttribute('aria-label') || ''");
                
                // Check for associated <label>
                String associatedLabel = (String) candidate.getLocator().evaluate(
                    "el => { " +
                    "  const label = el.id ? document.querySelector(`label[for='${el.id}']`) : null; " +
                    "  return label ? label.textContent.trim() : ''; " +
                    "}"
                );
                
                String targetLower = targetDesc.toLowerCase();
                
                // HUGE boost for actual <select> elements
                if (tagName.equals("select")) {
                    score += 60;
                }
                
                // Boost for ARIA roles
                if (role.equals("combobox") || role.equals("listbox")) {
                    score += 50;
                }
                
                // Check if inside modal (prioritize modal selects)
                try {
                    Boolean isInModal = (Boolean) candidate.getLocator().evaluate(
                        "el => { " +
                        "  const modal = el.closest('[role=\"dialog\"], .modal'); " +
                        "  return modal !== null && getComputedStyle(modal).display !== 'none'; " +
                        "}"
                    );
                    
                    if (isInModal) {
                        score += 80;  // HUGE boost for selects inside modals
                    }
                } catch (Exception ignored) {}
                
                // Check associated label
                if (associatedLabel != null && !associatedLabel.isEmpty() && 
                    associatedLabel.toLowerCase().contains(targetLower)) {
                    score += 40;
                }
                
                // Check aria-label
                if (ariaLabel.toLowerCase().contains(targetLower)) {
                    score += 35;
                }
                
                // Check name/id
                if (name.toLowerCase().contains(targetLower) ||
                    id.toLowerCase().contains(targetLower)) {
                    score += 30;
                }
                
            } catch (Exception ignored) {}
        }
        
        // Type affinity scoring
        score += scoreActionTypeAffinity("select", IntentAnalyzer.ActionType.SELECT) * 30;
        
        return score;
    }
}
