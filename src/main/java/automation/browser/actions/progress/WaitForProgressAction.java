package automation.browser.actions.progress;

import automation.browser.actions.BrowserAction;
import automation.browser.SmartLocator;
import automation.planner.ActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Action to wait for a progress bar to reach a specific percentage.
 * Useful for synchronizing with long-running background tasks.
 */
public class WaitForProgressAction implements BrowserAction {
    private static final LoggerUtil logger = LoggerUtil.getLogger(WaitForProgressAction.class);
    private static final int DEFAULT_TIMEOUT_SEC = 60;
    private static final int POLL_INTERVAL_MS = 500;

    @Override
    public boolean execute(Page page, SmartLocator smartLocator, ActionPlan plan) {
        long startTime = System.currentTimeMillis();
        String targetPercentageStr = plan.getValue();
        int targetPercentage = 100;
        
        try {
            if (targetPercentageStr != null) {
                targetPercentage = Integer.parseInt(targetPercentageStr.replaceAll("[^0-9]", ""));
            }
        } catch (Exception e) {
            logger.warning("Could not parse target percentage '{}', defaulting to 100", targetPercentageStr);
        }

        String elementName = plan.getElementName();
        if (elementName == null || elementName.isEmpty() || "progress bar".equalsIgnoreCase(elementName)) {
            elementName = "progressbar"; // Use role as default search
        }

        logger.info("Waiting for progress bar '{}' to reach {}%", elementName, targetPercentage);

        // 1. Find the progress bar
        Locator progressBar = smartLocator.findSmartElement(elementName, "progressbar");
        if (progressBar == null) {
            logger.failure("Could not find progress bar matching: {}", elementName);
            return false;
        }

        // 2. Polling loop
        int lastValue = -1;
        long timeoutMs = DEFAULT_TIMEOUT_SEC * 1000;
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            // Check if element is still valid
            if (progressBar.count() == 0) {
                logger.debug("Progress bar lost, attempting to re-locate...");
                progressBar = smartLocator.findSmartElement(elementName, "progressbar");
                if (progressBar == null || progressBar.count() == 0) {
                    try { Thread.sleep(1000); } catch (Exception e) {}
                    continue;
                }
            }

            int currentValue = getProgressValue(progressBar);
            
            if (currentValue != lastValue) {
                if (currentValue != -1) {
                    logger.debug("Progress: {}% (Target: {}%)", currentValue, targetPercentage);
                } else {
                    try {
                        logger.debug("Could not read progress value from element (Tag: {})", 
                            progressBar.evaluate("el => el.tagName"));
                    } catch (Exception e) {
                        logger.debug("Element stale or inaccessible");
                    }
                }
                lastValue = currentValue;
            }

            if (currentValue >= targetPercentage && currentValue != -1) {
                long duration = System.currentTimeMillis() - startTime;
                logger.success("Progress bar reached {}% (Total Wait: {}ms)", currentValue, duration);
                return true;
            }

            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Wait interrupted");
                return false;
            }
        }

        logger.failure("Timed out after {}s waiting for progress {}%. Current: {}%", 
            DEFAULT_TIMEOUT_SEC, targetPercentage, lastValue);
        return false;
    }

    private int getProgressValue(Locator locator) {
        try {
            // Priority 1: aria-valuenow (Standard ARIA)
            String ariaValue = locator.getAttribute("aria-valuenow");
            if (ariaValue != null && !ariaValue.isEmpty()) {
                return (int) Double.parseDouble(ariaValue);
            }

            // Priority 2: Text content (e.g. "45%")
            String text = locator.innerText();
            if (text != null && text.contains("%")) {
                String numericPart = text.replaceAll("[^0-9]", "");
                if (!numericPart.isEmpty()) {
                    return Integer.parseInt(numericPart);
                }
            }

            // Priority 3: Style width (e.g. "width: 45%")
            String style = locator.getAttribute("style");
            if (style != null && style.contains("width:")) {
                String widthPart = style.split("width:")[1].split("%")[0].replaceAll("[^0-9.]", "").trim();
                if (!widthPart.isEmpty()) {
                    return (int) Double.parseDouble(widthPart);
                }
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        return -1;
    }
}
