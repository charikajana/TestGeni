package automation.integration;

import automation.browser.BrowserService;
import automation.browser.SmartLocator;
import automation.planner.ActionPlan;
import automation.planner.SmartStepParser;
import automation.reporting.StepExecutionReport;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.*;

import java.util.ArrayList;
import java.util.List;

public class TestGeniAgent {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(TestGeniAgent.class);
    
    private Page page;
    private SmartLocator smartLocator;
    private BrowserService browserService;
    private SmartStepParser stepParser;
    
    /**
     * Initialize with external Playwright Page (from Selenium/Playwright BDD tests)
     */
    public TestGeniAgent(Page page) {
        this.page = page;
        this.smartLocator = new SmartLocator(page);
        this.browserService = new BrowserService(page, smartLocator);
        this.stepParser = new SmartStepParser();
        
        logger.info("SmartAutomationAgent initialized");
    }
    
    /**
     * Execute a natural language step using intelligent automation
     * 
     * @param naturalLanguageStep - Natural language step (e.g., "When I click Submit button")
     * @return true if step executed successfully, false if it failed
     */
    public boolean execute(String naturalLanguageStep) {
        try {
            logger.info("Executing: {}", naturalLanguageStep);
            
            // Parse the step using intelligent NLP
            ActionPlan plan = stepParser.parseStep(naturalLanguageStep, page, smartLocator);
            
            // Execute the action
            StepExecutionReport report = browserService.executeAction(plan);
            
            boolean success = "PASSED".equals(report.getStatus());
            
            if (success) {
                logger.success("✓ Smart automation succeeded: {}", naturalLanguageStep);
            } else {
                logger.warn("✗ Smart automation failed: {}", naturalLanguageStep);
            }
            
            return success;
            
        } catch (Exception e) {
            logger.error("Smart automation error: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Execute a step and return detailed execution report
     */
    public StepExecutionReport executeWithReport(String naturalLanguageStep) {
        try {
            ActionPlan plan = stepParser.parseStep(naturalLanguageStep, page, smartLocator);
            return browserService.executeAction(plan);
        } catch (Exception e) {
            logger.error("Execution error: {}", e.getMessage());
            return new StepExecutionReport()
                .stepName(naturalLanguageStep)
                .status("FAILED")
                .duration(0L);
        }
    }
    
    /**
     * Check if a step is supported by the framework WITHOUT executing it.
     * Useful for deciding between smart automation and custom fallback logic.
     * 
     * @param naturalLanguageStep - Natural language step
     * @return true if framework can handle this step, false if custom code needed
     * 
     * Example:
     *   if (agent.isSupported("When I click Submit")) {
     *       agent.execute("When I click Submit");  // Use framework
     *   } else {
     *       customClickSubmit();  // Use custom code
     *   }
     */
    public boolean isSupported(String naturalLanguageStep) {
        if (stepParser == null) {
            logger.warn("StepParser not initialized - cannot check support");
            return false;
        }
        
        return stepParser.isStepSupported(naturalLanguageStep);
    }
    
    /**
     * Get suggestions for rewriting an unsupported step to match framework patterns.
     * Like "Did you mean...?" for test steps!
     * 
     * @param unsupportedStep - Step that doesn't match any pattern
     * @return List of suggested alternative phrasings (top 5, sorted by confidence)
     * 
     * Example:
     *   Input: "Click the submit button"
     *   Output: [
     *     "95% - When I click Submit button (Standard click pattern)",
     *     "90% - And I click Submit button (Click button pattern)",
     *     ...
     *   ]
     */
    public List<String> getSuggestions(String unsupportedStep) {
        automation.intelligence.StepSuggestionEngine suggestionEngine =
            new automation.intelligence.StepSuggestionEngine();
        
        List<automation.intelligence.StepSuggestionEngine.StepSuggestion> suggestions =
            suggestionEngine.generateSuggestions(unsupportedStep);
        
        List<String> formattedSuggestions = new ArrayList<>();
        for (automation.intelligence.StepSuggestionEngine.StepSuggestion suggestion : suggestions) {
            formattedSuggestions.add(suggestion.toString());
        }
        
        return formattedSuggestions;
    }
    
    /**
     * Get detailed suggestion objects for advanced usage
     */
    public List<automation.intelligence.StepSuggestionEngine.StepSuggestion> getDetailedSuggestions(String unsupportedStep) {
        automation.intelligence.StepSuggestionEngine suggestionEngine =
            new automation.intelligence.StepSuggestionEngine();
        
        return suggestionEngine.generateSuggestions(unsupportedStep);
    }
    
    /**
     * Get execution statistics
     */
    public ExecutionStats getStats() {
        // TODO: Implement statistics tracking
        return new ExecutionStats();
    }
    
    public static class ExecutionStats {
        private int smartExecutions = 0;
        private int fallbackExecutions = 0;
        private long totalTimeSaved = 0;
        
        public void recordSmartExecution(long duration) {
            smartExecutions++;
            // Estimate 5 minutes saved per step (no manual locator needed)
            totalTimeSaved += 300000; // 5 minutes in ms
        }
        
        public void recordFallback() {
            fallbackExecutions++;
        }
        
        public int getSmartExecutions() { return smartExecutions; }
        public int getFallbackExecutions() { return fallbackExecutions; }
        public long getTotalTimeSavedMs() { return totalTimeSaved; }
        public double getAutomationRate() {
            int total = smartExecutions + fallbackExecutions;
            return total == 0 ? 0 : (smartExecutions * 100.0 / total);
        }
    }
}
