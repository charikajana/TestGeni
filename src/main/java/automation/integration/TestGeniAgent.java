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
    
    // Static instance for shared use across step definitions
    private static TestGeniAgent instance;
    
    private Page page;
    private SmartLocator smartLocator;
    private BrowserService browserService;
    private SmartStepParser stepParser;
    
    /**
     * Initialize with external Playwright Page (from host framework)
     * 
     * @param page - Playwright Page instance from host framework (Cucumber/TestNG hooks)
     */
    public TestGeniAgent(Page page) {
        if (page == null) {
            throw new IllegalArgumentException("Page instance cannot be null. Initialize Playwright in your host framework first.");
        }
        
        this.page = page;
        this.smartLocator = new SmartLocator(page);
        this.browserService = new BrowserService(page, smartLocator);
        this.stepParser = new SmartStepParser();
        
        logger.info("TestGeniAgent initialized with external Page instance");
    }
    
    /**
     * Get or create a shared TestGeniAgent instance
     * Useful for sharing across multiple step definition classes
     * 
     * @param page - Playwright Page from host framework
     * @return Shared TestGeniAgent instance
     */
    public static TestGeniAgent getInstance(Page page) {
        if (instance == null || instance.page != page) {
            instance = new TestGeniAgent(page);
        }
        return instance;
    }
    
    /**
     * Update the Page instance (useful when switching tabs/windows in host framework)
     * 
     * @param newPage - New Page instance from host framework
     */
    public void updatePage(Page newPage) {
        if (newPage == null) {
            throw new IllegalArgumentException("Page instance cannot be null");
        }
        
        if (this.page != newPage) {
            logger.info("Updating TestGeniAgent to use new Page instance");
            this.page = newPage;
            this.smartLocator.setPage(newPage);
            this.browserService = new BrowserService(newPage, smartLocator);
        }
    }
    
    /**
     * Get the current Page instance being used
     * 
     * @return Current Playwright Page
     */
    public Page getPage() {
        return this.page;
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
     * Execute a step using a template with parameters (for Cucumber integration)
     * 
     * @param stepTemplate - Template with %s placeholders (e.g., "I enter %s in %s field")
     * @param args - Arguments to fill the template
     * @return true if step executed successfully, false if it failed
     * 
     * Example:
     *   agent.executeTemplate("I enter \"%s\" in \"%s\" field", "john@example.com", "Email")
     *   Becomes: "I enter \"john@example.com\" in \"Email\" field"
     */
    public boolean executeTemplate(String stepTemplate, Object... args) {
        String fullStep = String.format(stepTemplate, args);
        return execute(fullStep);
    }
    
    /**
     * Execute the current step automatically using captured step text (EASIEST METHOD!)
     * Uses ThreadLocal storage set by host framework's hooks.
     * 
     * Returns complete details including:
     * - stepName: The full step text
     * - status: PASSED, FAILED, SKIPPED
     * - timestamp: Execution time
     * - targetedElement: Element description
     * - locatorUsed: Actual selector used
     * - expectedText: Expected value (for verify steps)
     * - actualText: Actual value (for verify steps)
     * - duration: Execution time in milliseconds
     * 
     * @return StepExecutionReport with all execution details
     * 
     * Example in your step definition:
     *   @When("I enter {string} in {string} field")
     *   public void enterField(String value, String field) {
     *       StepExecutionReport report = testGeni.executeCurrentStep();
     *       
     *       if (!"PASSED".equals(report.getStatus())) {
     *           // Fallback to your own logic
     *           customEnterField(value, field);
     *       }
     *   }
     * 
     * Note: Requires CucumberStepContext.setCurrentStepText() in @BeforeStep hook
     */
    public StepExecutionReport executeCurrentStep() {
        String stepText = CucumberStepContext.getCurrentStepText();
        
        if (stepText == null || stepText.isEmpty()) {
            logger.error("No step text available. Did you call CucumberStepContext.setCurrentStepText() in @BeforeStep hook?");
            return new StepExecutionReport()
                .stepName(stepText)
                .status("FAILED")
                .errorMessage("No step text available in CucumberStepContext")
                .duration(0L);
        }
        
        return executeWithReport(stepText);
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
