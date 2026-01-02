package automation.browser.actions.verify;

import automation.browser.actions.BrowserAction;
import automation.browser.SmartLocator;
import automation.planner.ActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.List;

/**
 * Action to verify that a specific tooltip appears when an element is hovered.
 */
public class VerifyTooltipAction implements BrowserAction {
    private static final LoggerUtil logger = LoggerUtil.getLogger(VerifyTooltipAction.class);

    @Override
    public boolean execute(Page page, SmartLocator smartLocator, ActionPlan plan) {
        String elementName = plan.getElementName();
        String expectedTooltipText = plan.getValue();

        automation.reporting.StepExecutionReport.ValidationResult result = 
            new automation.reporting.StepExecutionReport.ValidationResult()
                .expected(expectedTooltipText != null && !expectedTooltipText.isEmpty() ? expectedTooltipText : "[Disappeared]")
                .comparisonType("TEXT_MATCH");

        // Case 1: Verify tooltip disappears (empty expectedTooltipText)
        if (expectedTooltipText == null || expectedTooltipText.isEmpty()) {
            logger.info("Verifying tooltip has disappeared");
            boolean success = verifyTooltipNotVisible(page);
            result.actual(success ? "[Disappeared]" : "[Still Visible]").match(success);
            plan.setMetadataValue("validation", result);
            return success;
        }

        // Case 2: Verify tooltip appears with specific text
        // If elementName is provided, hover over it first
        if (elementName != null && !elementName.isEmpty()) {
            logger.info("Verifying tooltip for '{}' contains: '{}'", elementName, expectedTooltipText);
            
            // Find and Hover the target element
            Locator target = smartLocator.waitForSmartElement(elementName, null);
            if (target == null) {
                logger.failure("Could not find target element to hover: {}", elementName);
                result.match(false).elementFound(false).details("Could not find target element to hover: " + elementName);
                plan.setMetadataValue("validation", result);
                return false;
            }

            target.hover();
            logger.debug("Hovered over element, waiting for tooltip...");
        } else {
            // No element specified - assume we're already hovering (from a previous "hover" step)
            logger.info("Verifying tooltip '{}' is visible (assuming already hovered)", expectedTooltipText);
        }

        // Check for the tooltip
        String[] actualPlaceholder = { null };
        if (findTooltipWithTextAndReturn(page, expectedTooltipText, actualPlaceholder)) {
            logger.success("Tooltip verified successfully: '{}'", expectedTooltipText);
            result.actual(actualPlaceholder[0]).match(true);
            plan.setMetadataValue("validation", result);
            return true;
        }

        logger.failure("Tooltip with text '{}' not found", expectedTooltipText);
        result.actual(actualPlaceholder[0] != null ? actualPlaceholder[0] : "[Not Found]").match(false);
        plan.setMetadataValue("validation", result);
        return false;
    }

    /**
     * Helper with actual text capture
     */
    private boolean findTooltipWithTextAndReturn(Page page, String expectedText, String[] actualText) {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < 5000) {
            // Strategy 1: Look for role="tooltip"
            Locator tooltipByRole = page.locator("[role='tooltip']").all().stream()
                .filter(Locator::isVisible)
                .findFirst().orElse(null);
            if (tooltipByRole != null) {
                actualText[0] = tooltipByRole.innerText();
                if (actualText[0].contains(expectedText)) {
                    return true;
                }
            }

            // Strategy 2: Check for common tooltip CSS classes
            String[] commonClasses = {".tooltip", ".tooltip-inner", ".md-tooltip", ".p-tooltip", ".v-tooltip__content", ".ant-tooltip"};
            for (String cssClass : commonClasses) {
                Locator tooltipByClass = page.locator(cssClass).all().stream()
                    .filter(Locator::isVisible)
                    .findFirst().orElse(null);
                if (tooltipByClass != null) {
                    actualText[0] = tooltipByClass.innerText();
                    if (actualText[0].contains(expectedText)) {
                        return true;
                    }
                }
            }
            
            // Strategy 3: Check for any visible element with the exact text
            try {
                Locator textMatch = page.locator("text=\"" + expectedText + "\"").all().stream()
                    .filter(Locator::isVisible)
                    .findFirst().orElse(null);
                if (textMatch != null) {
                    actualText[0] = expectedText;
                    return true;
                }
            } catch (Exception ignored) {}

            try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        return false;
    }
    
    /**
     * Verify that no tooltip is currently visible
     */
    private boolean verifyTooltipNotVisible(Page page) {
        try {
            // Check for any visible tooltips
            List<Locator> visibleTooltips = page.locator("[role='tooltip']").all().stream()
                .filter(Locator::isVisible)
                .toList();
                
            if (visibleTooltips.isEmpty()) {
                logger.success("Tooltip has disappeared as expected");
                return true;
            }
            
            logger.failure("Tooltip is still visible");
            return false;
        } catch (Exception e) {
            logger.success("No tooltips found (disappeared as expected)");
            return true;
        }
    }
    
    /**
     * Find tooltip with specific text using multiple strategies
     */
    private boolean findTooltipWithText(Page page, String expectedText) {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < 5000) {
            // Strategy 1: Look for role="tooltip"
            Locator tooltipByRole = page.locator("[role='tooltip']").all().stream()
                .filter(Locator::isVisible)
                .findFirst().orElse(null);
            if (tooltipByRole != null) {
                String actualText = tooltipByRole.innerText();
                if (actualText.contains(expectedText)) {
                    logger.debug("Tooltip found via role='tooltip': '{}'", actualText.trim());
                    return true;
                }
            }

            // Strategy 2: Check for common tooltip CSS classes
            String[] commonClasses = {".tooltip", ".tooltip-inner", ".md-tooltip", ".p-tooltip", ".v-tooltip__content", ".ant-tooltip"};
            for (String cssClass : commonClasses) {
                Locator tooltipByClass = page.locator(cssClass).all().stream()
                    .filter(Locator::isVisible)
                    .findFirst().orElse(null);
                if (tooltipByClass != null) {
                    String actualText = tooltipByClass.innerText();
                    if (actualText.contains(expectedText)) {
                        logger.debug("Tooltip found via CSS class '{}': '{}'", cssClass, actualText.trim());
                        return true;
                    }
                }
            }
            
            // Strategy 3: Check for any visible element with the exact text
            try {
                Locator textMatch = page.locator("text=\"" + expectedText + "\"").all().stream()
                    .filter(Locator::isVisible)
                    .findFirst().orElse(null);
                if (textMatch != null) {
                    logger.debug("Tooltip found via direct text match: '{}'", expectedText);
                    return true;
                }
            } catch (Exception ignored) {}

            try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        
        return false;
    }
}
