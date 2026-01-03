package automation.browser.actions.verify;

import automation.browser.actions.BrowserAction;
import automation.browser.SmartLocator;
import automation.planner.ActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import automation.browser.locator.table.TableNavigator;

public class VerifyTextAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(VerifyTextAction.class);
    
    @Override
    public boolean execute(Page page, SmartLocator locator, ActionPlan plan) {
        String value = plan.getValue();
        String targetName = plan.getElementName();
        String textToVerify = (value != null && !value.isEmpty()) ? value : targetName;
        boolean isNegated = plan.isNegated() || "verify_not".equals(plan.getActionType());
        
        automation.reporting.StepExecutionReport.ValidationResult result = 
            new automation.reporting.StepExecutionReport.ValidationResult()
                .expected(isNegated ? "NOT " + textToVerify : textToVerify)
                .comparisonType("TEXT_MATCH");
        
        if (textToVerify == null) {
            logger.failure("Verification failed - No text specified");
            result.match(false).details("No text specified");
            plan.setMetadataValue("validation", result);
            return false;
        }

        if (isNegated) {
            logger.debug("Verifying text ABSENCE (negative): {}", textToVerify);
        } else {
            logger.debug("Verifying text presence: {}", textToVerify);
        }
        
        // Determine search scope once
        Locator searchScope = null;
        if (plan.getRowAnchor() != null) {
             TableNavigator navigator = new TableNavigator();
             searchScope = navigator.findRowByAnchor(page, plan.getRowAnchor());
             if (searchScope == null) {
                 logger.failure("Row not found for anchor: {}", plan.getRowAnchor());
                 result.match(false).elementFound(false).details("Row not found for anchor: " + plan.getRowAnchor());
                 plan.setMetadataValue("validation", result);
                 return false;
             }
        }
        
        // Retry logic for robustness - Increased timeout to 2 seconds
        long deadline = System.currentTimeMillis() + 2000; 
        int attempt = 1;
        String lastFoundText = null;
        
        while (System.currentTimeMillis() < deadline) {
            boolean iterationFound = false;
            
            // 1. Handle Frame Scoping
            String frameAnchor = plan.getFrameAnchor();
            if (frameAnchor != null) {
                com.microsoft.playwright.Frame frame = locator.findFrame(frameAnchor);
                if (frame != null) {
                    Object[] verificationResult = performVerificationInternal(frame, null, textToVerify, plan);
                    iterationFound = (boolean) verificationResult[0];
                    if (iterationFound) lastFoundText = (String) verificationResult[1];
                }
            }

            // 2. Standard verification (Main Page or Scope)
            if (!iterationFound) {
                Object[] verificationResult = performVerificationInternal(page, searchScope, textToVerify, plan);
                iterationFound = (boolean) verificationResult[0];
                if (iterationFound) lastFoundText = (String) verificationResult[1];
            }

            // 3. Automatic Cross-Frame verification fallback
            if (!iterationFound && searchScope == null) {
                try {
                    for (com.microsoft.playwright.Frame frame : page.frames()) {
                        if (frame == page.mainFrame()) continue;
                        if (frame.isDetached()) continue;
                        
                        Object[] verificationResult = performVerificationInternal(frame, null, textToVerify, plan);
                        if ((boolean) verificationResult[0]) {
                            iterationFound = true;
                            lastFoundText = (String) verificationResult[1];
                            break;
                        }
                    }
                } catch (Exception e) {}
            }
            
            // Handle logical outcomes based on negation
            if (!isNegated && iterationFound) {
                // POSITIVE match found - Success
                result.match(true).actual(lastFoundText).elementFound(true).elementVisible(true);
                plan.setMetadataValue("validation", result);
                return true;
            } else if (isNegated && iterationFound) {
                // NEGATIVE match found - Failure (should not be present)
                logger.failure("Negative verification failed: Text '{}' WAS found (should NOT be present)", textToVerify);
                result.match(false).actual(lastFoundText).details("Text was found when it should not be");
                plan.setMetadataValue("validation", result);
                return false;
            }
            
            try {
                Thread.sleep(100);
                attempt++;
                logger.debug("Verification fast-check {} for: '{}'", attempt, textToVerify);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Loop ended without returning
        if (isNegated) {
            // Reached timeout without finding it - SUCCESS
            logger.section("VALIDATION SUCCESS (Negative)");
            result.match(true).actual("Text not found").details("Text remained absent during fast-check");
            plan.setMetadataValue("validation", result);
            return true;
        } else {
            // Reached timeout without finding it - FAILURE
            logger.failure("Verification failed: Text '{}' not found within 2 seconds", textToVerify);
            result.match(false).actual(lastFoundText != null ? lastFoundText : "Text not found").details("Text not found after 2 seconds");
            plan.setMetadataValue("validation", result);
            return false;
        }
    }

    private Object[] performVerificationInternal(Object context, Locator searchScope, String textToVerify, ActionPlan plan) {
        // Try multiple strategies for maximum compatibility
        String foundText = null;
        String matchType = null;
        Locator allFound = null;
        
        // Strategy 1: Exact text match
        allFound = tryFindText(context, searchScope, textToVerify, true);
        if (allFound != null && allFound.count() > 0) {
            matchType = "EXACT";
            Object[] result = findVisibleCandidate(allFound, matchType, textToVerify, plan);
            if ((boolean)result[0]) return result;
        }
        
        // Strategy 2: Contains match
        allFound = tryFindText(context, searchScope, textToVerify, false);
        if (allFound != null && allFound.count() > 0) {
            matchType = "CONTAINS";
            Object[] result = findVisibleCandidate(allFound, matchType, textToVerify, plan);
            if ((boolean)result[0]) return result;
        }
        
        // Strategy 3: Case-insensitive match
        allFound = tryFindTextCaseInsensitive(context, searchScope, textToVerify);
        if (allFound != null && allFound.count() > 0) {
            matchType = "CASE-INSENSITIVE";
            Object[] result = findVisibleCandidate(allFound, matchType, textToVerify, plan);
            if ((boolean)result[0]) return result;
        }
        
        // Strategy 4: Value match
        String xpath = String.format("//*[(@value='%s' or .='%s')]", textToVerify, textToVerify);
        allFound = getLocator(context, searchScope, xpath); 
        if (allFound != null && allFound.count() > 0) {
            matchType = "VALUE/XPATH";
            Object[] result = findVisibleCandidate(allFound, matchType, textToVerify, plan);
            if ((boolean)result[0]) return result;
        }
        
        return new Object[] { false, null };
    }

    private Object[] findVisibleCandidate(Locator allFound, String matchType, String textToVerify, ActionPlan plan) {
        int count = allFound.count();
        String firstHiddenText = null;
        
        for (int i = 0; i < count; i++) {
            Locator candidate = allFound.nth(i);
            boolean visible = false;
            try { visible = candidate.isVisible(); } catch (Exception e) {}
            
            String foundText = getElementText(candidate);
            if (i == 0) firstHiddenText = foundText;

            if (visible || "VALUE/XPATH".equals(matchType) || "option".equalsIgnoreCase((String)candidate.evaluate("el => el.tagName"))) {
                logger.section("VALIDATION SUCCESS");
                logger.info(" Expected: {}", textToVerify);
                logger.info(" Found in Element: {}", foundText);
                logger.info(" Match Strategy: {} (Candidate {}/{})", matchType, (i+1), count);
                logger.info("--------------------------------------------------");
                
                plan.setMetadataValue("found_element_type", candidate.evaluate("el => el.tagName.toLowerCase()"));
                plan.setMetadataValue("found_selector", "text=\"" + foundText + "\"");
                
                if (isBookingReference(foundText)) storeBookingReference(foundText);
                return new Object[] { true, foundText };
            }
        }
        return new Object[] { false, firstHiddenText };
    }

    /**
     * Helper to get locator from Page or Frame
     */
    private Locator getLocator(Object context, Locator scope, String selector) {
        if (scope != null) return scope.locator(selector);
        if (context instanceof Page) return ((Page)context).locator(selector);
        if (context instanceof com.microsoft.playwright.Frame) return ((com.microsoft.playwright.Frame)context).locator(selector);
        return null;
    }
    
    private Locator tryFindText(Object context, Locator scope, String text, boolean exact) {
        try {
            if (scope != null) return scope.getByText(text, new Locator.GetByTextOptions().setExact(exact));
            if (context instanceof Page) return ((Page)context).getByText(text, new Page.GetByTextOptions().setExact(exact));
            if (context instanceof com.microsoft.playwright.Frame) return ((com.microsoft.playwright.Frame)context).getByText(text, new com.microsoft.playwright.Frame.GetByTextOptions().setExact(exact));
        } catch (Exception e) {}
        return null;
    }
    
    /**
     * Try case-insensitive search
     */
    private Locator tryFindTextCaseInsensitive(Object context, Locator scope, String text) {
        String xpath = String.format(
            "//*[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '%s')]",
            text.toLowerCase()
        );
        return getLocator(context, scope, xpath);
    }
    
    /**
     * Get text from element, checking multiple sources
     */
    private String getElementText(Locator element) {
        try {
            // Try innerText first (most common)
            String text = element.innerText().trim();
            if (!text.isEmpty()) return text;
            
            // Try textContent (for hidden elements)
            text = element.textContent().trim();
            if (!text.isEmpty()) return text;
            
            // Try value attribute (for inputs)
            text = (String) element.evaluate("el => el.value || ''");
            if (!text.isEmpty()) return text;
            
            // Try aria-label (for accessibility text)
            text = (String) element.evaluate("el => el.getAttribute('aria-label') || ''");
            if (!text.isEmpty()) return text;
            
            return "[No text found]";
        } catch (Exception e) {
            return "[Error reading text]";
        }
    }
    
    /**
     * Check if text matches booking reference pattern
     */
    private boolean isBookingReference(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        // Common booking reference patterns:
        // - All caps with numbers (e.g., "ABC123", "BOOK456")
        // - Starts with letters and contains numbers
        // - Length between 5-15 characters
        String upperText = text.toUpperCase();
        
        return (text.matches("^[A-Z0-9]{5,15}$") ||  // All caps/numbers
                text.matches("^[A-Z]{2,5}[0-9]{3,10}$") ||  // Letter prefix + numbers
                upperText.contains("BOOKING") ||
                upperText.contains("REFERENCE") ||
                upperText.contains("CONFIRMATION"));
    }
    
    /**
     * Store booking reference to TestContext
     */
    private void storeBookingReference(String reference) {
        try {
            automation.context.TestContext context = automation.context.TestContext.getInstance();
            context.setBookingReference("hotel", reference);
            logger.info("Auto-stored booking reference: {}", reference);
        } catch (Exception e) {
            logger.debug("Could not store booking reference: {}", e.getMessage());
        }
    }
}
