package automation;

import automation.browser.BrowserService;
import automation.browser.SmartLocator;
import automation.feature.FeatureReader;
import automation.planner.ActionPlan;
import automation.planner.SmartStepParser;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.*;

import java.util.List;

public class AgentApplication {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(AgentApplication.class);
    
    // Playwright resources
    private static Playwright playwright;
    private static Browser browser;
    private static Page page;
    
    public static void main(String[] args) throws Exception {
        logger.info("Simple Test Agent started...");

        // Initialize Playwright and Browser
        initializeBrowser();
        
        try {
            // Initialize services with the Page instance
            FeatureReader reader = new FeatureReader();
            SmartStepParser planner = new SmartStepParser();
            SmartLocator smartLocator = new SmartLocator(page);
            BrowserService browserService = new BrowserService(page, smartLocator);

            String featurePath = "src/main/resources/features/WebTable.feature";
            if (args != null && args.length > 0) {
                featurePath = args[0];
            }
            List<String> steps = reader.readSteps(featurePath);

            int totalSteps = steps.size();
            int passed = 0;
            int failed = 0;
            int skipped = 0;

            boolean shouldContinue = true;
            
            for (String step : steps) {
                if (!shouldContinue) {
                    skipped++;
                    logger.warn("SKIPPED: {}", step);
                    continue;
                }
                
                // Parse step with page context (enables intelligent semantic matching)
                ActionPlan plan = planner.parseStep(step, page, smartLocator);
                logger.debug(plan.toString());
                
                // Check if this is a composite action plan
                if (plan instanceof automation.planner.CompositeActionPlan) {
                    automation.planner.CompositeActionPlan compositePlan = (automation.planner.CompositeActionPlan) plan;
                    boolean allSubActionsSucceeded = true;
                    
                    logger.info("  Executing {} sub-actions...", compositePlan.getSubActionCount());
                    
                    // Execute each sub-action sequentially
                    int subIndex = 1;
                    for (ActionPlan subAction : compositePlan.getSubActions()) {
                        logger.info("    Sub-action {}/{}: {}", 
                            subIndex, compositePlan.getSubActionCount(), subAction.getActionType());
                        
                        automation.reporting.StepExecutionReport subReport = browserService.executeAction(subAction);
                        
                        // Log JSON report for sub-action
                        logger.info("STEP EXECUTION REPORT:\n{}", subReport.toJson());
                        
                        if ("PASSED".equals(subReport.getStatus())) {
                            logger.success("    Sub-action {} succeeded", subIndex);
                        } else {
                            logger.failure("    Sub-action {} failed", subIndex);
                            allSubActionsSucceeded = false;
                            break; // Stop executing remaining sub-actions on first failure
                        }
                        subIndex++;
                    }
                    
                    if (allSubActionsSucceeded) {
                        passed++;
                        logger.success("  All sub-actions completed successfully");
                    } else {
                        failed++;
                        shouldContinue = false;
                        logger.error("\nEXECUTION STOPPED: Composite action failed, skipping remaining steps\n");
                    }
                } else {
                    // Regular single action
                    automation.reporting.StepExecutionReport report = browserService.executeAction(plan);
                    
                    // Log JSON report
                    logger.info("STEP EXECUTION REPORT:\n{}", report.toJson());
                    
                    if ("PASSED".equals(report.getStatus())) {
                        passed++;
                    } else {
                        failed++;
                        // Stop execution on first failure to prevent cascading errors
                        shouldContinue = false;
                        logger.error("\nEXECUTION STOPPED: Step failed, skipping remaining steps to prevent uncontrolled loop\n");
                    }
                }
            }
            
            logger.summary("EXECUTION SUMMARY", totalSteps, passed, failed, skipped);
            
        } catch (Throwable e) {
            logger.error("Critical Error (Agent Crash): {}", e.getMessage(), e);
        } finally {
            closeBrowser();
        }
    }
    
    /**
     * Initialize Playwright, Browser, and Page
     */
    private static void initializeBrowser() {
        logger.info("Initializing browser...");
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
        page = browser.newPage();
        page.setDefaultTimeout(120000);
        page.setDefaultNavigationTimeout(120000);
        logger.success("Browser initialized successfully");
    }
    
    /**
     * Close Browser and Playwright resources
     */
    private static void closeBrowser() {
        logger.info("Closing browser...");
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
        logger.success("Browser closed successfully");
    }
    
    /**
     * Get the current Page instance (for external access if needed)
     */
    public static Page getPage() {
        return page;
    }
}
