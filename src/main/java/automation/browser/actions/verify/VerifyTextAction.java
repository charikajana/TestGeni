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
        
        // Retry logic for robustness
        long deadline = System.currentTimeMillis() + 10000; // 10s timeout for verification
        int attempt = 1;
        String lastFoundText = null;
        
        while (System.currentTimeMillis() < deadline) {
            // 1. Handle Frame Scoping
            String frameAnchor = plan.getFrameAnchor();
            if (frameAnchor != null) {
                com.microsoft.playwright.Frame frame = locator.findFrame(frameAnchor);
                if (frame != null) {
                    Object[] verificationResult = performVerificationInternal(frame, null, textToVerify);
                    boolean found = (boolean) verificationResult[0];
                    lastFoundText = (String) verificationResult[1];
                    
                    if (isNegated) {
                        if (!found) {
                            logger.section("VALIDATION SUCCESS (Negative)");
                            result.match(true).actual("Text not found").details("Text not found as expected");
                            plan.setMetadataValue("validation", result);
                            return true;
                        }
                    } else {
                        if (found) {
                            result.match(true).actual(lastFoundText).elementFound(true).elementVisible(true);
                            plan.setMetadataValue("validation", result);
                            return true;
                        }
                    }
                }
            }

            // 2. Standard verification (Main Page or Scope)
            Object[] verificationResult = performVerificationInternal(page, searchScope, textToVerify);
            boolean found = (boolean) verificationResult[0];
            lastFoundText = (String) verificationResult[1];
            
            if (isNegated) {
                if (!found) {
                    logger.section("VALIDATION SUCCESS (Negative)");
                    result.match(true).actual("Text not found").details("Text not found as expected");
                    plan.setMetadataValue("validation", result);
                    return true;
                }
            } else {
                if (found) {
                    result.match(true).actual(lastFoundText).elementFound(true).elementVisible(true);
                    plan.setMetadataValue("validation", result);
                    return true;
                }
            }

            // 3. Automatic Cross-Frame verification fallback
            if (searchScope == null) {
                try {
                    for (com.microsoft.playwright.Frame frame : page.frames()) {
                        if (frame == page.mainFrame()) continue;
                        if (frame.isDetached()) continue;
                        
                        try {
                            verificationResult = performVerificationInternal(frame, null, textToVerify);
                            found = (boolean) verificationResult[0];
                            lastFoundText = (String) verificationResult[1];
                            
                            if (isNegated) {
                                if (!found) {
                                    logger.section("VALIDATION SUCCESS (Negative)");
                                    result.match(true).actual("Text not found").details("Text not found as expected");
                                    plan.setMetadataValue("validation", result);
                                    return true;
                                }
                            } else {
                                if (found) {
                                    result.match(true).actual(lastFoundText).elementFound(true).elementVisible(true);
                                    plan.setMetadataValue("validation", result);
                                    return true;
                                }
                            }
                        } catch (Exception e) {}
                    }
                } catch (Exception e) {}
            }
            
            try {
                Thread.sleep(1000);
                attempt++;
                logger.debug("Verification retry {}/10 for: '{}'", attempt, textToVerify);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        result.match(false).actual(lastFoundText != null ? lastFoundText : "Text not found");
        if (isNegated) {
            logger.failure("Negative verification failed: Text '{}' WAS found (should NOT be present)", textToVerify);
            result.details("Text was found when it should not be");
        } else {
            logger.failure("Verification timed out: Text '{}' not found after 10 seconds", textToVerify);
            result.details("Text not found after 10s timeout");
        }
        plan.setMetadataValue("validation", result);
        return false;
    }

    private Object[] performVerificationInternal(Object context, Locator searchScope, String textToVerify) {
        // Try multiple strategies for maximum compatibility
        String foundText = null;
        String matchType = null;
        Locator foundElement = null;
        
        // Strategy 1: Exact text match
        foundElement = tryFindText(context, searchScope, textToVerify, true);
        if (foundElement != null && foundElement.count() > 0) {
            foundText = getElementText(foundElement);
            matchType = "EXACT";
        }
        
        // Strategy 2: Contains match
        if (foundElement == null || foundElement.count() == 0) {
            foundElement = tryFindText(context, searchScope, textToVerify, false);
            if (foundElement != null && foundElement.count() > 0) {
                foundText = getElementText(foundElement);
                matchType = "CONTAINS";
            }
        }
        
        // Strategy 3: Case-insensitive match
        if (foundElement == null || foundElement.count() == 0) {
            foundElement = tryFindTextCaseInsensitive(context, searchScope, textToVerify);
            if (foundElement != null && foundElement.count() > 0) {
                foundText = getElementText(foundElement);
                matchType = "CASE-INSENSITIVE";
            }
        }
        
        // Strategy 4: Value/XPath match
        if (foundElement == null || foundElement.count() == 0) {
            String xpath = String.format("//*[(@value='%s' or .='%s')]", textToVerify, textToVerify);
            foundElement = getLocator(context, searchScope, xpath);
            if (foundElement != null && foundElement.count() > 0) {
                foundText = getElementText(foundElement);
                matchType = "VALUE/XPATH";
            }
        }
        
        // Validate result
        if (foundElement != null && foundElement.count() > 0) {
            boolean visible = false;
            try { visible = foundElement.isVisible(); } catch (Exception e) {}

            if (visible || "VALUE/XPATH".equals(matchType) || "option".equalsIgnoreCase((String)foundElement.evaluate("el => el.tagName"))) {
                logger.section("VALIDATION SUCCESS");
                logger.info(" Expected: {}", textToVerify);
                logger.info(" Found in Element: {}", foundText);
                logger.info(" Match Strategy: {}", matchType + (visible ? "" : " (Hidden/Value)"));
                logger.info("--------------------------------------------------");
                
                // Phase 3: Auto-store booking references to context
                if (isBookingReference(foundText)) {
                    storeBookingReference(foundText);
                }
                
                return new Object[] { true, foundText };
            }
        }
        return new Object[] { false, foundText };
    }

    /**
     * Helper to get locator from Page or Frame
     */
    private Locator getLocator(Object context, Locator scope, String selector) {
        if (scope != null) return scope.locator(selector).first();
        if (context instanceof Page) return ((Page)context).locator(selector).first();
        if (context instanceof com.microsoft.playwright.Frame) return ((com.microsoft.playwright.Frame)context).locator(selector).first();
        return null;
    }

    /**
     * Try to find text using exact or contains match
     */
    private Locator tryFindText(Object context, Locator scope, String text, boolean exact) {
        try {
            if (scope != null) return scope.getByText(text, new Locator.GetByTextOptions().setExact(exact)).first();
            if (context instanceof Page) return ((Page)context).getByText(text, new Page.GetByTextOptions().setExact(exact)).first();
            if (context instanceof com.microsoft.playwright.Frame) return ((com.microsoft.playwright.Frame)context).getByText(text, new com.microsoft.playwright.Frame.GetByTextOptions().setExact(exact)).first();
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
