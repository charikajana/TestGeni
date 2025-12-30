package automation.planner;

import automation.intelligence.IntelligentStepProcessor;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Page;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Smart Step Parser with INTELLIGENCE LAYER (Phase 4).
 * 
 * Parsing strategies (in order):
 * 0. Intelligent NLP-based processing (NEW - Phase 4)
 * 1. Combined action detection
 * 2. Frame-scoped actions
 * 3. Table-specific patterns
 * 4. Legacy regex patterns
 * 5. Fuzzy intent classification
 * 6. LLM fallback (future)
 */
public class SmartStepParser {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(SmartStepParser.class);
    
    private final IntelligentStepProcessor intelligentProcessor;  // Phase 4: Intelligence
    private final StepPlanner legacyPlanner;
    private final Map<String, List<TableStepPattern>> tablePatterns;
    
    // Configuration flag to enable/disable intelligence
    private boolean intelligenceEnabled = true;
    
    public SmartStepParser() {
        this.intelligentProcessor = new IntelligentStepProcessor();
        this.legacyPlanner = new StepPlanner();
        this.tablePatterns = new HashMap<>();
        initializeTablePatterns();
    }
    
    /**
     * Enable or disable intelligence layer
     */
    public void setIntelligenceEnabled(boolean enabled) {
        this.intelligenceEnabled = enabled;
        logger.info("Intelligence Layer: {}", enabled ? "ENABLED" : "DISABLED");
    }
    
    /**
     * Check if a step is supported by the framework (matches any pattern)
     * WITHOUT executing it. Useful for BDD integration to decide between
     * smart automation vs custom logic.
     * 
     * @param step Natural language step
     * @return true if framework can handle this step, false otherwise
     * 
     * Example:
     *   if (parser.isStepSupported("When I click Submit")) {
     *       // Use framework
     *   } else {
     *       // Use custom code
     *   }
     */
    public boolean isStepSupported(String step) {
        if (step == null || step.trim().isEmpty()) {
            return false;
        }
        
        String normalizedStep = step.trim();
        
        try {
            // Try intelligence layer first
            if (intelligenceEnabled) {
                logger.debug("Checking step support via Intelligence Layer: {}", normalizedStep);
                // Intelligence layer can handle almost any natural language
                // Check if it can extract a valid intent
                if (intelligentProcessor.canProcess(normalizedStep)) {
                    logger.debug("✓ Step supported by Intelligence Layer");
                    return true;
                }
            }
            
            // Check legacy patterns
            logger.debug("Checking step support via Legacy Patterns: {}", normalizedStep);
            
            // Check combined actions
            if (PatternRegistry.getCombinedActions().stream()
                    .anyMatch(pattern -> pattern.matcher(normalizedStep).matches())) {
                logger.debug("✓ Step supported by Combined Actions");
                return true;
            }
            
            // Check table patterns
            for (List<TableStepPattern> patterns : tablePatterns.values()) {
                for (TableStepPattern pattern : patterns) {
                    if (pattern.getPattern().matcher(normalizedStep).matches()) {
                        logger.debug("✓ Step supported by Table Patterns");
                        return true;
                    }
                }
            }
            
            // Check all registered patterns
            for (Map.Entry<String, Pattern> entry : PatternRegistry.getAllPatterns().entrySet()) {
                if (entry.getValue().matcher(normalizedStep).matches()) {
                    logger.debug("✓ Step supported by Pattern: {}", entry.getKey());
                    return true;
                }
            }
            
            logger.debug("✗ Step NOT supported by framework patterns");
            return false;
            
        } catch (Exception e) {
            logger.warn("Error checking step support: {}", e.getMessage());
            return false;
        }
    }
    
    private void initializeTablePatterns() {
       PatternRegistry.registerTablePatterns(this::addTablePattern);
    }
    
    private void addTablePattern(String actionType, String regex, Map<String, Object> groupMapping) {
        tablePatterns.computeIfAbsent(actionType, k -> new ArrayList<>())
            .add(new TableStepPattern(Pattern.compile(regex, Pattern.CASE_INSENSITIVE), groupMapping));
    }
    
    /**
     * Main parsing method - tries multiple strategies
     * 
     * @param step The natural language step to parse
     * @return ActionPlan ready for execution
     */
    public ActionPlan parseStep(String step) {
        return parseStep(step, null, null);
    }
    
    /**
     * Parse step with optional page context for intelligent processing
     * 
     * @param step The natural language step
     * @param page Optional page context (enables semantic matching)
     * @param smartLocator Optional smart locator (for fallback)
     * @return ActionPlan ready for execution
     */
    public ActionPlan parseStep(String step, Page page, automation.browser.SmartLocator smartLocator) {
        logger.section("PARSING STEP");
        logger.info("Step: {}", step);
        
        // STRATEGY 0: Check if this is a combined action step FIRST (before intelligence layer)
        // This prevents the intelligence layer from incorrectly concatenating values
        if (isCombinedAction(step)) {
            logger.debug("→ Composite Action Detected - Bypassing Intelligence Layer");
            logger.info("Detected Combined Action Step");
            return parseCombinedActions(step, page, smartLocator);
        }
        
        // STRATEGY 2: Check for frame-scoped actions ("In iframe 'x', click 'y'")
        logger.debug("→ Trying: Frame-Scoped Actions");
        ActionPlan frameScopedPlan = tryFrameScoping(step, page, smartLocator);
        if (frameScopedPlan != null) {
            logger.success("✓ Parsed via: FRAME-SCOPED PATTERN");
            return frameScopedPlan;
        }

        // STRATEGY 1: INTELLIGENT NLP-BASED PROCESSING (Phase 4)
        if (intelligenceEnabled) {
            logger.debug("→ Trying: Intelligent NLP Processing");
            ActionPlan intelligentPlan = intelligentProcessor.processStep(step, page, smartLocator);
            
            if (intelligentPlan != null && intelligentPlan.isValid()) {
                logger.success("✓ Parsed via: INTELLIGENCE LAYER");
                return intelligentPlan;
            }
            logger.debug("  ✗ Intelligence layer did not match");
        }
        
        // STRATEGY 3: Try table-specific patterns first (new features)
        logger.debug("→ Trying: Table-Specific Patterns");
        ActionPlan tablePlan = tryTablePatterns(step);
        if (tablePlan != null) {
            logger.success("✓ Parsed via: TABLE PATTERN ({})", tablePlan.getActionType());
            return tablePlan;
        }
        
        // STRATEGY 4: Fall back to legacy patterns (existing features)
        logger.debug("→ Trying: Legacy Regex Patterns");
        ActionPlan legacyPlan = legacyPlanner.plan(step);
        if (legacyPlan != null && !"unknown".equals(legacyPlan.getActionType())) {
            logger.success("✓ Parsed via: LEGACY PATTERN ({})", legacyPlan.getActionType());
            return legacyPlan;
        }
        
        // STRATEGY 5: Try intent-based fuzzy matching
        logger.debug("→ Trying: Fuzzy Intent Classification");
        ActionPlan fuzzyPlan = tryIntentClassification(step);
        if (fuzzyPlan != null) {
            logger.warning("✓ Parsed via: FUZZY INTENT ({})", fuzzyPlan.getActionType());
            return fuzzyPlan;
        }
        
        // STRATEGY 6: LLM Fallback (future - would call OpenAI API here)
        logger.error("✗ Could not parse step: {}", step);
        return createUnknownPlan(step);
    }
    
    /**
     * Handles patterns like "In iframe 'frame1', Enter 'John' in 'First Name'"
     */
    private ActionPlan tryFrameScoping(String step, Page page, automation.browser.SmartLocator smartLocator) {
        Pattern p = Pattern.compile("^(?i)(?:given|when|then|and|but)?\\s*(?:in|within|inside)\\s+(?:the\\s+)?(?:iframe|frame)\\s+[\"']?([^\"']+)[\"']?[\\s,]+(.+)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(step);
        
        if (m.find()) {
            String frameName = m.group(1);
            String remainingStep = m.group(2).trim();
            
            logger.info("Detected Frame Scoping: '{}'", frameName);
            
            // Parse the remaining part as a normal step
            ActionPlan innerPlan = parseStep(remainingStep, page, smartLocator);
            
            // Set the frame anchor
            innerPlan.setFrameAnchor(frameName);
            
            // If it's a composite action plan, propagate the frame anchor to all sub-actions
            if (innerPlan instanceof CompositeActionPlan) {
                for (ActionPlan sub : ((CompositeActionPlan) innerPlan).getSubActions()) {
                    sub.setFrameAnchor(frameName);
                }
            }
            
            return innerPlan;
        }
        return null;
    }

    /**
     * Detects if a step contains multiple actions chained together.
     * Looks for delimiters: "and", "also", "then", ",", "&"
     * 
     * Examples:
     * - "When Enter FirstName and Enter LastName"
     * - "When Enter FirstName also Enter LastName also Enter Email"
     * - "When Enter FirstName, Enter LastName, Enter Email"
     * 
     * BUT NOT multi-value selects like:
     * - "Select "A" and "B" and "C" from dropdown" (this is a single action with multiple values)
     */
    private boolean isCombinedAction(String step) {
        // Remove Gherkin keywords to analyze the actual step content
        String cleanStep = step.replaceAll("^(?i)(Given|When|Then|And|But)\\s+", "");
        
        // FIRST: Check if this is a multi-value select pattern
        // Pattern: Select "value1" and "value2" and "value3" from "dropdown"
        // This should NOT be treated as a combined action
        Pattern multiSelectPattern = Pattern.compile(
            "(?i)^(?:select|choose)\\s+[\"']([^\"']+)[\"'](?:\\s+and\\s+[\"'][^\"']+[\"'])+\\s+(?:from|in|for)\\s+",
            Pattern.CASE_INSENSITIVE
        );
        if (multiSelectPattern.matcher(cleanStep).find()) {
            logger.debug("Multi-value select detected - NOT a combined action");
            return false;
        }
        
        // Define action-related keywords that indicate this is an action step
        // (not a table verification or other complex step)
        String[] actionKeywords = {"enter", "click", "select", "type", "fill", "choose", "check", "uncheck", "close"};
        
        boolean hasActionKeyword = false;
        String lowerStep = cleanStep.toLowerCase();
        for (String keyword : actionKeywords) {
            if (lowerStep.contains(keyword)) {
                hasActionKeyword = true;
                break;
            }
        }
        
        // Only process as combined if it's an action step
        if (!hasActionKeyword) {
            return false;
        }
        
        // Check for delimiters, but be smart about it
        // Pattern: word (delimiter) word (delimiter) word
        // This helps avoid false positives like "First Name and Last Name" (which is a single value)
        
        // Count potential action delimiters
        // Use regex to find patterns like: "Enter X and Enter Y" or "Click X also Click Y"
        Pattern combinedPattern = Pattern.compile(
            "(enter|click|select|type|fill|choose|check|uncheck|close).*?" +  // First action
            "\\s+(?:and|also|then|,|&)\\s+" +  // Delimiter
            "(enter|click|select|type|fill|choose|check|uncheck|close)",  // Second action
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = combinedPattern.matcher(cleanStep);
        boolean isCombined = matcher.find();
        
        if (isCombined) {
            logger.debug("Combined action detected with delimiters: and/also/then/,/&");
        }
        
        return isCombined;
    }
    
    /**
     * Parses a combined action step by splitting it into individual sub-actions.
     * Supports multiple delimiters: "and", "also", "then", ",", "&"
     * 
     * Returns a CompositeActionPlan containing all sub-actions.
     */
    private ActionPlan parseCombinedActions(String step, Page page, automation.browser.SmartLocator smartLocator) {
        // Extract the Gherkin keyword (Given/When/Then/And)
        String gherkinKeyword = "";
        Pattern keywordPattern = Pattern.compile("^(Given|When|Then|And|But)\\s+", Pattern.CASE_INSENSITIVE);
        Matcher keywordMatcher = keywordPattern.matcher(step);
        if (keywordMatcher.find()) {
            gherkinKeyword = keywordMatcher.group(1);
        }
        
        // Remove Gherkin keyword for splitting
        String cleanStep = step.replaceAll("^(?i)(Given|When|Then|And|But)\\s+", "");
        
        // Split by delimiters while preserving quoted strings
        // This regex splits by: "and", "also", "then", ",", or "&" (with surrounding spaces)
        // But NOT if they're inside quotes
        List<String> subActions = splitByDelimiters(cleanStep);
        
        logger.info("Split into {} sub-actions", subActions.size());
        for (int i = 0; i < subActions.size(); i++) {
            logger.debug("  Sub-action {}: '{}'", i + 1, subActions.get(i));
        }
        
        List<ActionPlan> parsedActions = new ArrayList<>();
        
        for (int i = 0; i < subActions.size(); i++) {
            String subAction = subActions.get(i).trim();
            
            // For subsequent actions, prepend "And" if no Gherkin keyword exists
            String fullSubAction = (i == 0 && !gherkinKeyword.isEmpty()) 
                ? gherkinKeyword + " " + subAction 
                : "And " + subAction;
            
            logger.debug("  {}. {}", (i + 1), subAction);
            
            // Recursively parse each sub-action WITH page context for intelligent matching
            ActionPlan subPlan = parseSingleAction(fullSubAction, page, smartLocator);
            
            if (subPlan != null && !"unknown".equals(subPlan.getActionType())) {
                parsedActions.add(subPlan);
            } else {
                logger.warning("Failed to parse sub-action: {}", subAction);
                // Still add it, but mark as unknown
                parsedActions.add(createUnknownPlan(fullSubAction));
            }
        }
        
        // Create a composite action plan
        CompositeActionPlan compositePlan = new CompositeActionPlan(step, parsedActions);
        logger.success("Created composite plan with {} actions", parsedActions.size());
        
        return compositePlan;
    }
    
    /**
     * Splits a step by delimiters while respecting quoted strings.
     * Delimiters: "and", "also", "then", ",", "&"
     */
    private List<String> splitByDelimiters(String step) {
        List<String> parts = new ArrayList<>();
        
        logger.debug("⚙️ COMPOSITE SPLIT INPUT: '{}'", step);
        
        // Split by common delimiters, but be smart about quotes
        // Use a regex that matches delimiters outside of quotes
        String delimiterRegex = "\\s+(?:and|also|then|,|&)\\s+(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";
        
        String[] rawParts = step.split(delimiterRegex);
        
        logger.debug("⚙️ REGEX SPLIT INTO {} RAW PARTS", rawParts.length);
        for (int i = 0; i < rawParts.length; i++) {
            logger.debug("  Raw Part {}: '{}'", i, rawParts[i]);
        }
        
        for (String part : rawParts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                parts.add(trimmed);
                logger.debug("✓ Added trimmed part: '{}'", trimmed);
            }
        }
        
        // If no split occurred, return the original step as a single action
        if (parts.isEmpty()) {
            parts.add(step);
            logger.debug("⚠️ No splits occurred, returning original step");
        }
        
        logger.debug("⚙️ FINAL SPLIT RESULT: {} parts", parts.size());
        
        return parts;
    }
    
    /**
     * Parse a single action without checking for combined actions.
     * This prevents infinite recursion when parsing sub-actions.
     */
    private ActionPlan parseSingleAction(String step, Page page, automation.browser.SmartLocator smartLocator) {
        // Try intelligence layer first (Phase 4) WITH page context for semantic matching!
        if (intelligenceEnabled) {
            ActionPlan intelligentPlan = intelligentProcessor.processStep(step, page, smartLocator);
            if (intelligentPlan != null && intelligentPlan.isValid()) {
                return intelligentPlan;
            }
        }
        
        // Try table patterns
        ActionPlan tablePlan = tryTablePatterns(step);
        if (tablePlan != null) {
            return tablePlan;
        }
        
        // Try legacy patterns
        ActionPlan legacyPlan = legacyPlanner.plan(step);
        if (legacyPlan != null && !"unknown".equals(legacyPlan.getActionType())) {
            return legacyPlan;
        }
        
        // Try intent classification
        ActionPlan fuzzyPlan = tryIntentClassification(step);
        if (fuzzyPlan != null) {
            return fuzzyPlan;
        }
        
        return createUnknownPlan(step);
    }
    
    private ActionPlan tryTablePatterns(String step) {
        String cleanStep = step.replaceAll("^(?i)(Given|When|Then|And|But)\\s+", "");
        
        for (Map.Entry<String, List<TableStepPattern>> entry : tablePatterns.entrySet()) {
            String actionType = entry.getKey();
            for (TableStepPattern pattern : entry.getValue()) {
                Matcher m = pattern.regex.matcher(cleanStep);
                if (m.find()) {
                    return buildActionPlan(actionType, step, m, pattern.groupMapping);
                }
            }
        }
        return null;
    }
    
    private ActionPlan buildActionPlan(String actionType, String step, Matcher m, Map<String, Object> mapping) {
        EnhancedActionPlan plan = new EnhancedActionPlan(actionType, step);
        plan.setLocatorStrategy("smart-table");
        
        for (Map.Entry<String, Object> entry : mapping.entrySet()) {
            String field = entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof Integer) {
                int groupIndex = (Integer) value;
                String extractedValue = m.group(groupIndex);
                setField(plan, field, extractedValue);
            } else if (value instanceof String) {
                // Static value
                setField(plan, field, (String) value);
            }
        }
        
        // Set rowAnchor to enable row-scoped element finding if we have a row condition
        if (plan.getRowConditionValue() != null) {
            plan.setRowAnchor(plan.getRowConditionValue());
            logger.debug("Setting rowAnchor: {}", plan.getRowConditionValue());
        }
        
        return plan;
    }
    
    private void setField(EnhancedActionPlan plan, String fieldName, String value) {
        if (value == null) return;
        
        switch (fieldName) {
            case "tableName":
                plan.setTableName(value);
                break;
            case "columnName":
                plan.setColumnName(value);
                break;
            case "conditionColumn":
                plan.setRowConditionColumn(value);
                break;
            case "conditionValue":
                plan.setRowConditionValue(value);
                break;
            case "targetColumn":
                plan.setTargetColumnName(value);
                break;
            case "expectedValue":
                plan.setExpectedValue(value);
                break;
            case "value":
                plan.setValue(value);
                break;
            case "rowNumber":
                plan.setRowNumber(Integer.parseInt(value));
                break;
            case "rowCount":
                plan.setExpectedRowCount(Integer.parseInt(value));
                break;
            case "sortOrder":
                plan.setSortOrder(value);
                break;
            case "filterValue":
                plan.setFilterValue(value);
                break;
            case "pageNumber":
                plan.setPageNumber(Integer.parseInt(value));
                break;
            case "bulkAction":
                plan.setIsBulkAction(true);
                plan.setBulkActionType(value);
                break;
            case "buttonName":
            case "frameName":
            case "elementName":
                plan.setElementName(value);
                break;
            case "searchValue":
                plan.setValue(value);
                break;
        }
    }

    
    /**
     * Intent-based classification using keyword matching
     */
    private ActionPlan tryIntentClassification(String step) {
        String lower = step.toLowerCase();
        
        // Classify by intent keywords
        if (containsAny(lower, "sort", "order by")) {
            return createIntent("sort_table", step, "Detected sorting intent");
        }
        if (containsAny(lower, "filter", "where")) {
            return createIntent("filter_table", step, "Detected filtering intent");
        }
        if (containsAny(lower, "page", "pagination", "navigate")) {
            return createIntent("navigate_to_page", step, "Detected pagination intent");
        }
        if (containsAny(lower, "select all", "deselect all")) {
            return createIntent("bulk_select", step, "Detected bulk selection intent");
        }
        
        return null;
    }
    
    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }
    
    private ActionPlan createIntent(String actionType, String step, String reason) {
        ActionPlan plan = new ActionPlan(actionType, step);
        plan.setLocatorStrategy("intent-based");
        logger.debug("Reason: {}", reason);
        return plan;
    }
    
    private ActionPlan createUnknownPlan(String step) {
        ActionPlan plan = new ActionPlan("unknown", step);
        plan.setLocatorStrategy("failed");
        return plan;
    }
    
    /**
     * Helper class to store pattern and its group mapping
     */
    private static class TableStepPattern {
        Pattern regex;
        Map<String, Object> groupMapping;
        
        TableStepPattern(Pattern regex, Map<String, Object> groupMapping) {
            this.regex = regex;
            this.groupMapping = groupMapping;
        }
        
        public Pattern getPattern() {
            return regex;
        }
    }
}
