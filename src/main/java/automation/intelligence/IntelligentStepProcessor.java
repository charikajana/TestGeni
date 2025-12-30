package automation.intelligence;

import automation.browser.SmartLocator;
import automation.browser.actions.click.ClickSemanticMatcher;
import automation.browser.actions.input.FillSemanticMatcher;
import automation.browser.actions.select.SelectSemanticMatcher;
import automation.browser.actions.verify.VerifySemanticMatcher;
import automation.planner.ActionPlan;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import automation.utils.LoggerUtil;

/**
 * Intelligent step processor that uses NLP and semantic matching.
 * 
 * Processing flow:
 * 1. Try intelligent intent-based processing
 * 2. Fall back to pattern-based processing if needed
 * 3. Return Action Plan for execution
 */
public class IntelligentStepProcessor {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(IntelligentStepProcessor.class);
    
    private IntentAnalyzer intentAnalyzer;
    private ClickSemanticMatcher clickMatcher;
    private FillSemanticMatcher fillMatcher;
    private SelectSemanticMatcher selectMatcher;
    private VerifySemanticMatcher verifyMatcher;
    // Note: We don't create SmartStepParser here to avoid circular dependency
    // The fallback to pattern parsing is handled by SmartStepParser itself
    
    public IntelligentStepProcessor() {
        this.intentAnalyzer = new IntentAnalyzer();
        this.clickMatcher = new ClickSemanticMatcher();
        this.fillMatcher = new FillSemanticMatcher();
        this.selectMatcher = new SelectSemanticMatcher();
        this.verifyMatcher = new VerifySemanticMatcher();
    }
    
    /**
     * Check if a step can be processed by the intelligence layer
     * WITHOUT actually executing it.
     * 
     * @param step Natural language step
     * @return true if intelligence layer can understand this step
     */
    public boolean canProcess(String step) {
        if (step == null || step.trim().isEmpty()) {
            return false;
        }
        
        try {
            // EXCEPTION: Composite actions with multiple quoted values
            // Count quoted strings - if we have multiple quoted values AND conjunctions, it's likely composite
            int quoteCount = 0;
            boolean inQuote = false;
            for (char c : step.toCharArray()) {
                if (c == '"') {
                    if (!inQuote) quoteCount++;
                    inQuote = !inQuote;
                }
            }
            
            // If we have 2+ quoted values and "and/also/then/,", it's likely a composite action
            String lowerStep = step.toLowerCase();
            if (quoteCount >= 2 && (lowerStep.contains(" and ") || lowerStep.contains(" also ") || 
                                     lowerStep.contains(" then ") || lowerStep.contains(","))) {
                logger.debug("Composite action detected ({} quoted values) - skipping intelligence layer", quoteCount);
                return false;
            }

            // EXCEPTION: Deselect actions
            if (step.toLowerCase().contains("deselect")) {
                 return false;
            }

            // Try to extract intent
            StepIntent intent = intentAnalyzer.analyzeStep(step);
            
            // EXCEPTION: Select actions
            // The legacy parser has expert logic for native and custom dropdowns
            // and multi-selects. We prefer that for reliability.
            if (intent.getActionType() == IntentAnalyzer.ActionType.SELECT) {
                 logger.debug("Select action detected - deferring to legacy parser for expert handling");
                 return false;
            }
            
            // If we got a valid intent (not UNKNOWN), we can process it
            if (intent != null && intent.getActionType() != IntentAnalyzer.ActionType.UNKNOWN) {
                return true;
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Process step intelligently
     * 
     * @param step Natural language step
     * @param page Current page (optional, for semantic matching)
     * @param smartLocator Smart locator (for fallback)
     * @return ActionPlan ready for execution
     */
    public ActionPlan processStep(String step, Page page, SmartLocator smartLocator) {
        // Try intelligent processing
        ActionPlan intelligentPlan = tryIntelligentProcessing(step, page);
        
        if (intelligentPlan != null && intelligentPlan.isValid()) {
            return intelligentPlan;
        }
        
        // Return null to signal fallback needed (handled by SmartStepParser)
        return null;
    }
    
    /**
     * Try intelligent NLP-based processing
     */
    private ActionPlan tryIntelligentProcessing(String step, Page page) {
        try {
            // Skip browser-level actions (alerts, prompts, confirms, etc.)
            // These need special handlers, not DOM element matching
            // Also skip deselect actions - they have dedicated DeselectAction handler
            String lowerStep = step.toLowerCase();
            if (isBrowserLevelAction(step) || lowerStep.contains("deselect") || 
                lowerStep.contains("remove") || lowerStep.contains("unselect") ||
                lowerStep.contains("progress") || lowerStep.contains("reach") || lowerStep.contains("wait for") || 
                lowerStep.contains("monitor") || lowerStep.contains("frame") || lowerStep.contains("iframe") || 
                lowerStep.contains("key") || lowerStep.contains("press") || lowerStep.contains("shortcut")) {
                logger.debug("Skipping intelligence layer for browser-level or specialized action");
                return null; // Skip intelligence layer
            }
            
            // Step 1: Extract intent
            StepIntent intent = intentAnalyzer.analyzeStep(step);
            logger.debug("Intent extracted: {}", intent);
            
            // EXCEPTION: Select actions
            // The legacy parser has expert logic for native and custom dropdowns
            if (intent.getActionType() == IntentAnalyzer.ActionType.SELECT) {
                 logger.debug("Select action detected - deferring to legacy parser");
                 return null;
            }
            
            // Step 2: Find element (if page available and element needed)
            Locator element = null;
            if (page != null && needsElementMatch(intent)) {
                // Use action-specific matcher based on intent type
                element = findElementWithActionMatcher(page, intent);
                
                if (element == null) {
                    logger.debug("No semantic match found");
                    return null;  // Fall back to patterns
                }
            }
            
            // Step 3: Create ActionPlan from intent
            ActionPlan plan = convertIntentToPlan(intent, element);
            
            return plan;
            
        } catch (Exception e) {
            logger.debug("Intelligent processing failed: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Find element using the appropriate action-specific matcher
     */
    private Locator findElementWithActionMatcher(Page page, StepIntent intent) {
        IntentAnalyzer.ActionType actionType = intent.getActionType();
        
        switch (actionType) {
            case CLICK:
                return clickMatcher.findBestMatch(page, intent);
                
            case FILL:
            case DATE_SET:
                return fillMatcher.findBestMatch(page, intent);
                
            case SELECT:
                return selectMatcher.findBestMatch(page, intent);
                
            case VERIFY:
                return verifyMatcher.findBestMatch(page, intent);
                
            case HOVER:
            case SCROLL:
                // Hover and Scroll use same element selection logic as Click
                return clickMatcher.findBestMatch(page, intent);
                
            default:
                logger.warn("No specific matcher for action type: {}", actionType);
                return null;
        }
    }
    
    private boolean needsElementMatch(StepIntent intent) {
        IntentAnalyzer.ActionType action = intent.getActionType();
        return action == IntentAnalyzer.ActionType.CLICK ||
               action == IntentAnalyzer.ActionType.FILL ||
               action == IntentAnalyzer.ActionType.SELECT ||
               action == IntentAnalyzer.ActionType.VERIFY ||
               action == IntentAnalyzer.ActionType.HOVER ||
               action == IntentAnalyzer.ActionType.SCROLL ||
               action == IntentAnalyzer.ActionType.DATE_SET;
    }
    
    /**
     * Convert StepIntent to ActionPlan
     */
    private ActionPlan convertIntentToPlan(StepIntent intent, Locator element) {
        ActionPlan plan = new ActionPlan();
        plan.setTarget(intent.getOriginalStep());
        
        // Map intelligent action type to legacy action type
        String actionType = mapActionType(intent.getActionType());
        plan.setActionType(actionType);
        
        // Set element and value
        plan.setElementName(intent.getTargetDescription());
        plan.setValue(intent.getValue());
        
        // Set negation flag (for negative assertions like "not displayed")
        plan.setNegated(intent.isNegated());
        
        // Store found locator if available  
        if (element != null) {
            // Store as metadata for direct use
            plan.setMetadataValue("intelligent_locator", element);
            // CRITICAL: Store the page URL so we can detect if the page changed (e.g., window switch)
            try {
                String pageUrl = element.page().url();
                plan.setMetadataValue("resolved_page_url", pageUrl);
            } catch (Exception e) {
                // If we can't get the URL, don't block the process
                logger.debug("Could not store page URL for locator validation: {}", e.getMessage());
            }
        }
        
        return plan;
    }
    
    /**
     * Map intelligent action type to legacy system
     */
    private String mapActionType(IntentAnalyzer.ActionType actionType) {
        switch (actionType) {
            case CLICK: return "click";
            case FILL: return "fill";
            case VERIFY: return "verify";
            case SELECT: return "select";
            case NAVIGATE: return "navigate";
            case WAIT: return "wait";
            case HOVER: return "hover";
            case SCROLL: return "scroll";
            case DATE_SET: return "set_date";
            default: return "unknown";
        }
    }
    
    /**
     * Create unknown action plan as fallback
     */
    private ActionPlan createUnknownPlan(String step) {
        ActionPlan plan = new ActionPlan();
        plan.setTarget(step);
        plan.setActionType("unknown");
        return plan;
    }
    
    /**
     * Check if step involves browser-level actions (not DOM elements)
     */
    private boolean isBrowserLevelAction(String step) {
        String lowerStep = step.toLowerCase();
        
        // Alert/confirm/prompt keywords
        String[] browserKeywords = {
            "alert", "confirm", "prompt", 
            "dialog", "popup",
            "accept alert", "dismiss alert", "verify alert",
            "accept confirm", "dismiss confirm",
            
            // Keyboard actions - skip intelligence layer
            "press escape", "press enter", "press tab",
            "press space", "press delete", "press backspace",
            "hit escape", "hit enter", "type escape",
            
            // Page refresh - skip intelligence layer
            "refresh", "reload",
            
            // Browser navigation - skip intelligence layer
            "go back", "navigate back", "browser back",
            "go forward", "navigate forward", "browser forward",
            
            // Special click types - skip intelligence layer (use legacy patterns)
            "double click", "double tap", "right click", "right tap",
            "context click", "secondary click",
            
            // Placeholder verification - skip intelligence layer
            "placeholder", "place holder",
            
            // Selection verification - skip intelligence layer (use legacy patterns)
            "multiple items", "is selected", "are selected", "not selected", "items are",
            "is enabled", "is disabled", "should be enabled", "should be disabled",
            "is active", "is clickable", "is interactive", "unchecked", "is checked",
            "isEnabled", "isDisabled", "isenabled", "isdisabled", "isclickable", "isinteractive",
            "is not disabled", "is not enabled",
            "greyed out", "grayed out", "inactive", "read-only", "readonly", "restricted",
            "not checked", "not chosen", "is on", "is off", "is chosen", "should be selected",
            "should not be selected", "not be selected", "not be checked", "should not be checked",
            
            // Table operations - skip intelligence layer (use table patterns)
            
            // Table operations - skip intelligence layer (use table patterns)
            "new row", "row is added", "row added", "in column",
            "table row", "table cell", "table data",
            "row where", "column value", "get all column",
            
            // Validation states - skip intelligence layer
            "is invalid", "has red border", "shows error", "is required",
            "is filled in", "url", "title", "tab count", "window count", "original tab",
            "homepage", "base url", "root url", "start page", "domain", "path",
            "parameter", "query", "hash", "anchor", "fragment", "host",
            
            // Window management (bypass for PatternRegistry)
            "switch to", "new window", "new tab", "close window", "close tab", 
            "window exists", "tab exists", "click and switch", "main window",
            
            // Progress Bar / Reach (bypass for specialized WaitForProgressAction)
            "progress", "reach",
            
            // Tooltip verification (bypass for PatternRegistry)
            "tooltip"
        };
        
        for (String keyword : browserKeywords) {
            // Use regex for whole-word boundary check on short keywords like "url"
            if (keyword.length() <= 3) {
                if (java.util.regex.Pattern.compile("\\b" + keyword + "\\b", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(lowerStep).find()) {
                    return true;
                }
            } else if (lowerStep.contains(keyword)) {
                return true;
            }
        }
        
        return false;
    }
}
