package automation.browser;

import automation.planner.ActionPlan;
import automation.browser.actions.*;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.*;

import java.util.Map;

public class BrowserService {

    private static final LoggerUtil logger = LoggerUtil.getLogger(BrowserService.class);
    
    private final Page page;
    private final SmartLocator smartLocator;
    private final Map<String, BrowserAction> actionHandlers;
    private String currentFrameAnchor = null;

    /**
     * Constructor that accepts externally managed Page and SmartLocator
     */
    public BrowserService(Page page, SmartLocator smartLocator) {
        this.page = page;
        this.smartLocator = smartLocator;
        
        // Initialize action handlers using the centralized registry
        ActionHandlerRegistry registry = new ActionHandlerRegistry();
        this.actionHandlers = registry.getHandlers();
        
        logger.info("BrowserService initialized with external Page instance");
    }

    public automation.reporting.StepExecutionReport executeAction(ActionPlan plan) {
        long startTime = System.currentTimeMillis();
        String actionType = plan.getActionType();
        String stepText = plan.getTarget();
        
        // Create report
        automation.reporting.StepExecutionReport report = new automation.reporting.StepExecutionReport()
            .stepName(stepText)
            .action(actionType);

        // Handle frame persistence
        if ("switch_to_frame".equals(actionType)) {
            currentFrameAnchor = plan.getElementName();
        } else if ("switch_to_main_frame".equals(actionType)) {
            currentFrameAnchor = null;
        }
        
        // If plan doesn't have an explicit frame anchor but we have a persistent one, use it
        if (plan.getFrameAnchor() == null && currentFrameAnchor != null) {
            plan.setFrameAnchor(currentFrameAnchor);
        }

        BrowserAction handler = actionHandlers.get(actionType);
        if (handler != null) {
            try {
                // Always use the currently active page
                Page activePage = getActivePage();
                // Ensure SmartLocator is using the active page
                smartLocator.setPage(activePage);
                
                logger.debug("Executing action: {} for step: {}", actionType, stepText);
                
                boolean success = handler.execute(activePage, smartLocator, plan);
                plan.setExecuted(true);
                
                // Capture execution details
                long duration = System.currentTimeMillis() - startTime;
                report.status(success ? "PASSED" : "FAILED")
                    .duration(duration);
                
                // Extract locator details from plan metadata if available
                if (plan.hasMetadata("intelligent_locator")) {
                    report.addMetadata("usedIntelligentLocator", true);
                }
                
                // Add semantic details if available
                extractSemanticDetails(plan, report);
                
                // Add locator details
                extractLocatorDetails(plan, report);
                
                // Add validation result if available (for verification actions)
                if (plan.hasMetadata("validation")) {
                    automation.reporting.StepExecutionReport.ValidationResult validation =
                        (automation.reporting.StepExecutionReport.ValidationResult) plan.getMetadataValue("validation");
                    report.validation(validation);
                }
                
                return report;
                
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                report.status("FAILED")
                    .duration(duration)
                    .errorMessage(e.getMessage());
                logger.error("Action execution failed: {}", e.getMessage());
                return report;
            }
        } else {
            long duration = System.currentTimeMillis() - startTime;
            report.status("FAILED")
                .duration(duration)
                .errorMessage("Unknown action: " + actionType);
            logger.error("Unknown action: {} for step: {}", actionType, stepText);
            return report;
        }
    }
    
    /**
     * Extract semantic details from ActionPlan
     */
    private void extractSemanticDetails(ActionPlan plan, automation.reporting.StepExecutionReport report) {
        automation.reporting.StepExecutionReport.SemanticDetails semantic =
            new automation.reporting.StepExecutionReport.SemanticDetails();
        
        if (plan.getElementName() != null) {
            semantic.targetDescription(plan.getElementName());
        }
        
        if (plan.getValue() != null) {
            semantic.valueProvided(plan.getValue());
        }
        
        semantic.intelligenceUsed(plan.hasMetadata("intelligent_locator"));
        
        report.semanticLocator(semantic);
    }
    
    /**
     * Extract locator details from ActionPlan
     */
    private void extractLocatorDetails(ActionPlan plan, automation.reporting.StepExecutionReport report) {
        automation.reporting.StepExecutionReport.LocatorDetails locator =
            new automation.reporting.StepExecutionReport.LocatorDetails();
        
        if (plan.getElementName() != null) {
            locator.elementText(plan.getElementName());
        }
        
        // Pull selector and type from metadata if they were recorded during discovery
        if (plan.hasMetadata("found_selector")) {
            locator.selector((String) plan.getMetadataValue("found_selector"));
        } else if (plan.getActionType() != null && (plan.getActionType().contains("navigate") || plan.getActionType().contains("browser_state") || plan.getActionType().contains("wait"))) {
            locator.selector("N/A (Browser Action)");
        }
        
        if (plan.hasMetadata("found_element_type")) {
            locator.elementType((String) plan.getMetadataValue("found_element_type"));
        } else if (plan.getActionType() != null) {
            if (plan.getActionType().contains("navigate") || plan.getActionType().contains("browser_state") || plan.getActionType().contains("wait")) {
                locator.elementType("WINDOW/SYSTEM");
            } else {
                // Fallback for element type based on action
                locator.elementType(plan.getActionType().contains("click") ? "button" : "field");
            }
        }
        
        if (plan.getLocatorStrategy() != null) {
            locator.matchStrategy(plan.getLocatorStrategy());
        } else if (plan.hasMetadata("intelligent_locator")) {
            locator.matchStrategy("SEMANTIC");
        } else {
            locator.matchStrategy("PATTERN");
        }
        
        report.locatorIdentified(locator);
    }
    
    /**
     * Get the currently active page (handles multiple windows)
     */
    private Page getActivePage() {
        if (page != null && page.context() != null) {
            java.util.List<Page> pages = page.context().pages();
            logger.debug("getActivePage(): Total pages = {}", pages.size());
            
            for (int i = pages.size() - 1; i >= 0; i--) {
                Page p = pages.get(i);
                try {
                    // Simple check to see if page is still alive
                    String url = p.url();
                    logger.debug("  Page[{}]: URL={}", i, url);
                    logger.info("RETURNING ACTIVE PAGE: {}", url);
                    return p;
                } catch (Exception e) {
                    // Page likely closed
                    logger.debug("Skipping closed page: {}", i);
                }
            }
        }
        logger.warn("No active page found, returning fallback");
        return page; // Fallback to original page
    }

    // Getter methods (optional, for backward compatibility if needed)
    public Page getPage() {
        return getActivePage();
    }
    
    public SmartLocator getSmartLocator() {
        return smartLocator;
    }
}
