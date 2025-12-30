package automation.integration;

/**
 * Thread-local storage for Cucumber step context.
 * Allows capturing the current step text from Cucumber hooks.
 */
public class CucumberStepContext {
    
    private static final ThreadLocal<String> currentStepText = new ThreadLocal<>();
    
    /**
     * Set the current step text (called from @BeforeStep hook)
     */
    public static void setCurrentStepText(String stepText) {
        currentStepText.set(stepText);
    }
    
    /**
     * Get the current step text
     */
    public static String getCurrentStepText() {
        return currentStepText.get();
    }
    
    /**
     * Clear the current step text (called from @AfterStep hook)
     */
    public static void clear() {
        currentStepText.remove();
    }
}
