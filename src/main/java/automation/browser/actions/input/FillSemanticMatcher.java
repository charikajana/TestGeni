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
    
    private static final int SCORE_THRESHOLD = 120; // Increased from 50 to prevent false positives
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
        
        // Find best match
        ScoredElement best = scores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
        
        if (best != null) {
            double bestScore = scores.get(best);
            logger.info("FILL Best match: Score={} ID='{}' Text='{}' (Analysis took {}ms)", 
                bestScore, best.getId(), best.getText(), (System.currentTimeMillis() - startTime));
            
            if (bestScore >= SCORE_THRESHOLD) {
                return best.getLocator();
            } else {
                logger.warn("Best FILL match score ({}) below threshold ({})", bestScore, SCORE_THRESHOLD);
            }
        }
        
        return null;
    }
    
    private List<ScoredElement> findCandidates(Page page, StepIntent intent) {
        automation.browser.locator.core.DomScanner scanner = new automation.browser.locator.core.DomScanner();
        List<automation.browser.locator.core.ElementCandidate> scanResults = scanner.scan(page);
        
        List<ScoredElement> elements = new ArrayList<>();
        Set<String> inputTags = new HashSet<>(Arrays.asList("input", "textarea", "select"));
        
        for (automation.browser.locator.core.ElementCandidate c : scanResults) {
            // Filter candidates offline
            String tag = c.tag.toLowerCase();
            String role = c.role.toLowerCase();
            
            boolean isInput = inputTags.contains(tag) || 
                             "textbox".equals(role) || 
                             "combobox".equals(role) ||
                             "searchbox".equals(role);
            
            if (isInput) {
                elements.add(new ScoredElement(page.locator("xpath=" + c.xpath).first(), c));
                if (elements.size() >= MAX_CANDIDATES) break;
            }
        }
        
        return elements;
    }
    
    /**
     * FILL-specific scoring logic (Offline-safe)
     */
    private double scoreCandidate(ScoredElement candidate, StepIntent intent) {
        // Since we don't have the full ElementCandidate here (only some fields in ScoredElement),
        // we should ideally pass the candidate to the constructor or use a better object.
        // For more accurate matching, check multiple attributes
        
        double score = 0.0;
        String candidateText = candidate.getText();
        String targetDesc = intent.getTargetDescription();
        
        if (targetDesc != null && !targetDesc.isEmpty()) {
            boolean hasText = candidateText != null && !candidateText.trim().isEmpty();
            String targetLower = targetDesc.toLowerCase();
            String targetLowerNoSpaces = targetLower.replaceAll("\\s+", "");
            
            // Get element attributes from the candidate
            String idLower = candidate.getId().toLowerCase();
            String classLower = candidate.getClassName().toLowerCase();
            String nameLower = candidate.getName().toLowerCase();
            String placeholderLower = candidate.getPlaceholder().toLowerCase();
            String labelLower = candidate.getLabel().toLowerCase();
            String dataQaLower = candidate.getDataQa().toLowerCase();
            
            // 1. EXACT ID/Name/Placeholder Match (Highest Priority)
            if (idLower.equals(targetLowerNoSpaces)) {
                score += 500;
            } else if (nameLower.equals(targetLowerNoSpaces)) {
                score += 450;
            } else if (placeholderLower.equals(targetLower)) {
                score += 400;
            }
            
            // 2. Word Boundary Matches
            if (idLower.matches(".*\\b" + targetLowerNoSpaces + "\\b.*")) {
                score += 250;
            } else if (idLower.contains(targetLowerNoSpaces)) {
                score += 80; // Partial ID match
            }

            if (nameLower.matches(".*\\b" + targetLowerNoSpaces + "\\b.*")) {
                score += 200;
            } else if (nameLower.contains(targetLowerNoSpaces)) {
                score += 60; // Partial Name match
            }

            if (placeholderLower.contains(targetLower)) {
                score += 100;
            }
            
            // 3. GLOBAL KEYWORD MATCH (for disambiguation like 'Signup Email')
            // If target has multiple words (e.g. "Signup", "Email"), reward candidates 
            // that have these words in their attributes.
            String[] targetWords = targetLower.split("\\s+");
            if (targetWords.length > 1) {
                int wordsFound = 0;
                String allAttrs = (idLower + " " + classLower + " " + nameLower + " " + placeholderLower + " " + labelLower + " " + dataQaLower).toLowerCase();
                
                for (String word : targetWords) {
                    if (word.length() < 3) continue; // Skip small words
                    if (allAttrs.contains(word)) {
                        wordsFound++;
                        score += 50; // Bonus for each word from description found in element metadata
                    }
                }
                
                // Extra bonus if ALL major words are found
                if (wordsFound >= targetWords.length) {
                    score += 100;
                    logger.debug("Multi-word match bonus (+100) for '{}' in attributes of '{}'", targetLower, allAttrs);
                }
            }
            
            // Check text/label similarity
            if (hasText) {
                double textScore = scoreTextSimilarity(candidateText, targetDesc);
                score += textScore * 40;
                if (candidateText.equalsIgnoreCase(targetDesc)) {
                    score += 150; // Strong match for exact label text
                }
            } else if (!labelLower.isEmpty()) {
                double labelScore = scoreTextSimilarity(labelLower, targetDesc);
                score += labelScore * 40;
                if (labelLower.equalsIgnoreCase(targetDesc)) {
                    score += 150;
                }
            }
            
            // Differentiator Bonus: If we have "Signup" in description and "signup" in attributes
            // This is key for AutomationExercise-like pages
            for (String word : targetWords) {
                if (word.equals("signup") || word.equals("login") || word.equals("subscribe") || word.equals("email") || word.equals("phone") || word.equals("mobile")) {
                    if (idLower.contains(word) || classLower.contains(word) || nameLower.contains(word) || dataQaLower.contains(word) || placeholderLower.contains(word)) {
                        score += 150; // Strong differentiator bonus
                        logger.debug("Differentiator bonus (+150) for '{}' found in attributes", word);
                    }
                }
            }

            // PENALTY: Logic from previous version to prevent common mismatches
            if (targetLowerNoSpaces.contains("email") && !idLower.contains("email") && !nameLower.contains("email")) {
                if (idLower.contains("name") || idLower.contains("first") || idLower.contains("last") || nameLower.contains("name") || placeholderLower.contains("name")) {
                    score -= 500; // Increased penalty to ensure it's rejected
                }
            }
            
            // Input/textarea type gets base score
            if (candidate.getType().equals("input") || candidate.getType().equals("textarea")) {
                score += 30;
            }
        }
        
        // Type match scoring
        if (intent.getElementType() != null) {
            score += scoreTypeSimilarity(candidate.getType(), intent.getElementType()) * 20; // Reduced from 30
        } else {
            score += scoreActionTypeAffinity(candidate.getType(), IntentAnalyzer.ActionType.FILL) * 20; // Reduced from 30
        }
        
        // IMPORTANT: If score is too low, it's likely a mismatch
        // We should be confident about the match
        if (score < 100 && targetDesc != null && !targetDesc.isEmpty()) {
            logger.debug("Low confidence score {} for target '{}', candidate ID='{}'", score, targetDesc, candidate.getId());
        }
        
        return score;
    }
}
