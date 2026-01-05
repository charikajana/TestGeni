package automation.intelligence;

import automation.utils.LoggerUtil;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Intelligent step analyzer that extracts intent from natural language.
 * 
 * Understands:
 * - Action verbs (click, fill, verify, select, etc.)
 * - Target elements (button, field, link, etc.)
 * - Values to input
 * - Spatial relationships (next to, below, inside)
 * - Visual attributes (blue, large, bold)
 * - Ordinal positions (first, second, last)
 */
public class IntentAnalyzer {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(IntentAnalyzer.class);
    
    // Action verb mappings (LinkedHashMap maintains insertion order for predictable matching)
    private static final Map<String, ActionType> ACTION_VERBS = new LinkedHashMap<>();
    private static final Set<String> SYNONYM_GROUPS = new HashSet<>();
    
    static {
        // ========================================
        // CLICK ACTIONS (30+ variants)
        // ========================================
        ACTION_VERBS.put("click", ActionType.CLICK);
        ACTION_VERBS.put("press", ActionType.CLICK);
        ACTION_VERBS.put("tap", ActionType.CLICK);
        ACTION_VERBS.put("hit", ActionType.CLICK);
        ACTION_VERBS.put("push", ActionType.CLICK);
        ACTION_VERBS.put("select", ActionType.CLICK);
        ACTION_VERBS.put("choose", ActionType.CLICK);
        ACTION_VERBS.put("activate", ActionType.CLICK);
        ACTION_VERBS.put("trigger", ActionType.CLICK);
        ACTION_VERBS.put("invoke", ActionType.CLICK);
        ACTION_VERBS.put("execute", ActionType.CLICK);
        ACTION_VERBS.put("submit", ActionType.CLICK);
        ACTION_VERBS.put("apply", ActionType.CLICK);
        
        // Modal/Dialog actions
        ACTION_VERBS.put("close", ActionType.CLICK);
        ACTION_VERBS.put("dismiss", ActionType.CLICK);
        ACTION_VERBS.put("cancel", ActionType.CLICK);
        ACTION_VERBS.put("reject", ActionType.CLICK);
        ACTION_VERBS.put("deny", ActionType.CLICK);
        
        // ========================================
        // SCROLL ACTIONS
        // ========================================
        ACTION_VERBS.put("scroll", ActionType.SCROLL);
        ACTION_VERBS.put("move", ActionType.SCROLL);
        ACTION_VERBS.put("slide", ActionType.SCROLL);
        ACTION_VERBS.put("swipe", ActionType.SCROLL);
        
        // Toggle/Switch actions
        ACTION_VERBS.put("toggle", ActionType.CLICK);
        ACTION_VERBS.put("switch", ActionType.CLICK);
        ACTION_VERBS.put("enable", ActionType.CLICK);
        ACTION_VERBS.put("disable", ActionType.CLICK);
        
        // Expand/Collapse actions
        ACTION_VERBS.put("expand", ActionType.EXPAND);
        ACTION_VERBS.put("collapse", ActionType.COLLAPSE);
        ACTION_VERBS.put("show", ActionType.CLICK);
        ACTION_VERBS.put("hide", ActionType.CLICK);
        ACTION_VERBS.put("reveal", ActionType.CLICK);
        ACTION_VERBS.put("unfold", ActionType.CLICK);
        ACTION_VERBS.put("fold", ActionType.CLICK);
        
        // ========================================
        // FILL/INPUT ACTIONS (30+ variants)
        // ========================================
        ACTION_VERBS.put("fill", ActionType.FILL);
        ACTION_VERBS.put("enter", ActionType.FILL);
        ACTION_VERBS.put("type", ActionType.FILL);
        ACTION_VERBS.put("input", ActionType.FILL);
        ACTION_VERBS.put("write", ActionType.FILL);
        ACTION_VERBS.put("provide", ActionType.FILL);
        ACTION_VERBS.put("set", ActionType.FILL);
        ACTION_VERBS.put("populate", ActionType.FILL);
        ACTION_VERBS.put("insert", ActionType.FILL);
        ACTION_VERBS.put("add", ActionType.FILL);
        ACTION_VERBS.put("specify", ActionType.FILL);
        ACTION_VERBS.put("supply", ActionType.FILL);
        ACTION_VERBS.put("key", ActionType.FILL);
        ACTION_VERBS.put("put", ActionType.FILL);
        
        // Update/Modify actions
        ACTION_VERBS.put("update", ActionType.FILL);
        ACTION_VERBS.put("modify", ActionType.FILL);
        ACTION_VERBS.put("change", ActionType.FILL);
        ACTION_VERBS.put("edit", ActionType.FILL);
        ACTION_VERBS.put("amend", ActionType.FILL);
        ACTION_VERBS.put("revise", ActionType.FILL);
        
        // Clear/Remove actions
        ACTION_VERBS.put("clear", ActionType.FILL);
        ACTION_VERBS.put("erase", ActionType.FILL);
        ACTION_VERBS.put("delete", ActionType.FILL);
        ACTION_VERBS.put("remove", ActionType.FILL);
        ACTION_VERBS.put("empty", ActionType.FILL);
        
        // Copy/Paste actions
        ACTION_VERBS.put("paste", ActionType.FILL);
        ACTION_VERBS.put("copy", ActionType.FILL);
        
        // Upload actions
        ACTION_VERBS.put("upload", ActionType.FILL);
        ACTION_VERBS.put("attach", ActionType.FILL);
        
        // ========================================
        // VERIFY/CHECK ACTIONS (25+ variants)
        // ========================================
        ACTION_VERBS.put("verify", ActionType.VERIFY);
        ACTION_VERBS.put("check", ActionType.VERIFY);
        ACTION_VERBS.put("assert", ActionType.VERIFY);
        ACTION_VERBS.put("validate", ActionType.VERIFY);
        ACTION_VERBS.put("confirm", ActionType.VERIFY);
        ACTION_VERBS.put("ensure", ActionType.VERIFY);
        ACTION_VERBS.put("see", ActionType.VERIFY);
        ACTION_VERBS.put("find", ActionType.VERIFY);
        ACTION_VERBS.put("expect", ActionType.VERIFY);
        ACTION_VERBS.put("observe", ActionType.VERIFY);
        ACTION_VERBS.put("notice", ActionType.VERIFY);
        ACTION_VERBS.put("should", ActionType.VERIFY);
        ACTION_VERBS.put("must", ActionType.VERIFY);
        
        // Display/Show verification
        ACTION_VERBS.put("display", ActionType.VERIFY);
        ACTION_VERBS.put("displays", ActionType.VERIFY);
        ACTION_VERBS.put("shown", ActionType.VERIFY);
        ACTION_VERBS.put("showing", ActionType.VERIFY);
        ACTION_VERBS.put("visible", ActionType.VERIFY);
        
        // Content verification
        ACTION_VERBS.put("contain", ActionType.VERIFY);
        ACTION_VERBS.put("contains", ActionType.VERIFY);
        ACTION_VERBS.put("include", ActionType.VERIFY);
        ACTION_VERBS.put("includes", ActionType.VERIFY);
        ACTION_VERBS.put("match", ActionType.VERIFY);
        ACTION_VERBS.put("matches", ActionType.VERIFY);
        ACTION_VERBS.put("equal", ActionType.VERIFY);
        ACTION_VERBS.put("equals", ActionType.VERIFY);
        
        // Inspection
        ACTION_VERBS.put("inspect", ActionType.VERIFY);
        ACTION_VERBS.put("examine", ActionType.VERIFY);
        ACTION_VERBS.put("review", ActionType.VERIFY);
        ACTION_VERBS.put("test", ActionType.VERIFY);
        
        // ========================================
        // MOUSE/HOVER ACTIONS
        // ========================================
        ACTION_VERBS.put("hover", ActionType.HOVER);
        ACTION_VERBS.put("mouseover", ActionType.HOVER);
        ACTION_VERBS.put("mouse-over", ActionType.HOVER);
        ACTION_VERBS.put("mouse over", ActionType.HOVER);
        ACTION_VERBS.put("places cursor on", ActionType.HOVER);
        ACTION_VERBS.put("move mouse to", ActionType.HOVER);
        ACTION_VERBS.put("focus on", ActionType.HOVER);
        ACTION_VERBS.put("hover on", ActionType.HOVER);
        ACTION_VERBS.put("hover over", ActionType.HOVER);
        
        // ========================================
        // SELECT/CHOOSE ACTIONS (15+ variants)
        // ========================================
        ACTION_VERBS.put("pick", ActionType.SELECT);
        ACTION_VERBS.put("opt", ActionType.SELECT);
        ACTION_VERBS.put("decide", ActionType.SELECT);
        ACTION_VERBS.put("mark", ActionType.SELECT);
        ACTION_VERBS.put("designate", ActionType.SELECT);
        
        // Checkbox/Radio specific (Note: 'check' is in VERIFY, context determines usage)
        ACTION_VERBS.put("uncheck", ActionType.SELECT);
        ACTION_VERBS.put("tick", ActionType.SELECT);
        ACTION_VERBS.put("untick", ActionType.SELECT);
        
        // Dropdown/List specific
        ACTION_VERBS.put("dropdown", ActionType.SELECT);
        ACTION_VERBS.put("deselect", ActionType.SELECT);
        ACTION_VERBS.put("filter", ActionType.SELECT);
        ACTION_VERBS.put("sort", ActionType.SELECT);
        
        // ========================================
        // NAVIGATION ACTIONS (15+ variants)
        // ========================================
        ACTION_VERBS.put("navigate", ActionType.NAVIGATE);
        ACTION_VERBS.put("goto", ActionType.NAVIGATE);
        ACTION_VERBS.put("go", ActionType.NAVIGATE);
        ACTION_VERBS.put("open", ActionType.NAVIGATE);
        ACTION_VERBS.put("visit", ActionType.NAVIGATE);
        ACTION_VERBS.put("access", ActionType.NAVIGATE);
        ACTION_VERBS.put("load", ActionType.NAVIGATE);
        ACTION_VERBS.put("browse", ActionType.NAVIGATE);
        ACTION_VERBS.put("redirect", ActionType.NAVIGATE);
        ACTION_VERBS.put("transfer", ActionType.NAVIGATE);
        
        // ========================================
        // WAIT ACTIONS (10+ variants)
        // ========================================
        ACTION_VERBS.put("wait", ActionType.WAIT);
        ACTION_VERBS.put("pause", ActionType.WAIT);
        ACTION_VERBS.put("delay", ActionType.WAIT);
        ACTION_VERBS.put("hold", ActionType.WAIT);
        ACTION_VERBS.put("sleep", ActionType.WAIT);
        ACTION_VERBS.put("idle", ActionType.WAIT);
        ACTION_VERBS.put("rest", ActionType.WAIT);
    }
    
    /**
     * Analyze a natural language step and extract intent
     */
    public StepIntent analyzeStep(String step) {
        logger.debug("Analyzing step: {}", step);
        
        String cleanStep = cleanStep(step);
        
        StepIntent intent = new StepIntent();
        intent.setOriginalStep(step);
        intent.setCleanStep(cleanStep);
        
        // Detect negation BEFORE extracting action type
        boolean isNegated = detectNegation(cleanStep);
        intent.setNegated(isNegated);
        
        // Extract action type
        ActionType actionType = extractActionType(cleanStep);
        intent.setActionType(actionType);
        
        // Extract values from quoted strings
        List<String> values = extractAllValues(cleanStep);
        intent.setValues(values);
        
        // Extract target description - should pass the list of values to exclude them
        String target = extractTarget(cleanStep, actionType, values);
        intent.setTargetDescription(target);
        
        // Set joined value for backward compatibility
        if (!values.isEmpty()) {
            intent.setValue(String.join("; ", values));
        }
        
        // Extract modifiers (spatial, visual, ordinal)
        Map<String, String> modifiers = extractModifiers(cleanStep);
        intent.setModifiers(modifiers);
        
        // Extract element type hints
        String elementType = extractElementType(cleanStep);
        intent.setElementType(elementType);
        
        // Extract parent reference (e.g., "inside 'Form'")
        String parentRef = extractParentReference(cleanStep);
        intent.setParentReference(parentRef);
        
        logger.info("Intent: {} -> Target='{}' Values={} Type='{}'", 
            actionType, target, values, elementType);
        
        return intent;
    }
    
    /**
     * Detect if the step contains negation keywords
     */
    private boolean detectNegation(String step) {
        String lowerStep = step.toLowerCase();
        
        // Negation patterns for verification/assertion steps
        String[] negationKeywords = {
            "not displayed", "not visible", "not shown", "not present",
            "not exist", "not exists", "not appear", "not appears",
            "should not", "shouldn't", "must not", "mustn't",
            "never", "no longer", "doesn't", "don't",
            "is not", "isn't", "are not", "aren't", "was not", "wasn't",
            "not be", "cannot", "can't", "deleted", "removed", "gone", "absent"
        };
        
        for (String negationKeyword : negationKeywords) {
            if (lowerStep.contains(negationKeyword)) {
                logger.debug("Detected negation: {}", negationKeyword);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Clean step text (remove Gherkin keywords, extra spaces)
     */
    private String cleanStep(String step) {
        String cleaned = step.replaceAll("^(?i)(Given|When|Then|And|But)\\s+", "");
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        return cleaned;
    }
    
    /**
     * Extract action type from step
     */
    private ActionType extractActionType(String step) {
        String lowerStep = step.toLowerCase();
        
        // PRIORITY 1: Explicit NAVIGATE verbs (prevents URL keywords from interfering)
        // This must come FIRST to handle "navigate to .../checkbox" correctly
        String[] navigationVerbs = {"navigate", "goto", "go to", "open", "visit", "access", "load", "browse"};
        for (String navVerb : navigationVerbs) {
            // Check at start: "navigate to..."
            if (lowerStep.startsWith(navVerb + " ") || lowerStep.startsWith(navVerb + " to ")) {
                return ActionType.NAVIGATE;
            }
            // Check after pronoun: "I navigate to...", "user navigate to...", etc.
            String pronounPattern = ".*(^| )(i|user|we|you|he|she|they) " + navVerb + "( to)? .*";
            if (lowerStep.matches(pronounPattern)) {
                return ActionType.NAVIGATE;
            }
        }
        
        // PRIORITY 1.5: Explicit HOVER verbs (prevents "over" from being treated as spatial modifier)
        // Must come before other patterns to handle "hover over...", "hover on..." correctly
        if (lowerStep.contains("hover")) {
            // "hover over", "hover on", "mouse over", etc.
            if (lowerStep.matches(".*\\bhover\\s+(over|on)\\b.*") || 
                lowerStep.matches(".*\\bmouse[-\\s]?over\\b.*")) {
                return ActionType.HOVER;
            }
            // Simple "hover [element]"
            if (lowerStep.startsWith("hover ") || lowerStep.contains(" hover ")) {
                return ActionType.HOVER;
            }
        }
        
        // PRIORITY 2: Special handling for 'select/choose' with list/grid context
        // "Select 'X' from list" should be SELECT, not a simple CLICK on a "list" element
        if (lowerStep.contains("select ") || lowerStep.contains("choose ")) {
            if (lowerStep.contains("from ") || lowerStep.contains(" in ") || lowerStep.contains(" for ") ||
                lowerStep.contains("list") || lowerStep.contains("grid") || 
                lowerStep.contains("dropdown") || lowerStep.contains("menu")) {
                return ActionType.SELECT;
            }
        }
        
        // PRIORITY 3: Special handling for ambiguous verbs
        if (lowerStep.contains("check")) {
            // "check checkbox" OR "check the box" → SELECT
            if (lowerStep.contains("checkbox") || 
                lowerStep.contains("check box") ||
                lowerStep.contains("radio")) {
                return ActionType.SELECT;
            }
            // "check that" OR "check if" → VERIFY
            if (lowerStep.contains(" that ") || 
                lowerStep.contains(" if ") ||
                lowerStep.contains(" whether ")) {
                return ActionType.VERIFY;
            }
            // Default "check" → VERIFY (more common)
            if (lowerStep.startsWith("check ") || lowerStep.contains(" check ")) {
                return ActionType.VERIFY;
            }
        }
        
        // PRIORITY 3: Special handling for 'remove' verb (context-sensitive)
        if (lowerStep.contains("remove")) {
            // "remove 'X' from Y" → SELECT (deselect from multiselect/autocomplete)
            if (lowerStep.matches(".*remove.*from.*")) {
                return ActionType.SELECT;
            }
        }

        // PRIORITY 4: Special handling for 'set/adjust/move' verb (context-sensitive for sliders)
        if (lowerStep.contains("set ") || lowerStep.contains("adjust ") || 
            lowerStep.contains("move ") || lowerStep.contains("slide ") ||
            lowerStep.contains("drag ")) {
            if (lowerStep.contains("slider") || lowerStep.contains("range") || 
                lowerStep.contains("volume") || lowerStep.contains("brightness") ||
                lowerStep.contains("zoom") || lowerStep.contains("percentage")) {
                return ActionType.UNKNOWN; // Fallback to specialized legacy SetSliderAction
            }
            // "set date" -> DATE_SET
            if (lowerStep.contains("date") || lowerStep.contains("picker") || 
                lowerStep.contains("tomorrow") || lowerStep.contains("yesterday")) {
                return ActionType.DATE_SET;
            }
        }

        // PRIORITY 5: Check if the value looks like a date (even if verb is 'enter' or 'fill')
        // BUT avoid false positives for phone numbers and dropdown selections
        String value = extractValue(step);
        if (value != null && isDateValue(value)) {
            // Check if this is likely a dropdown selection rather than a datepicker interaction
            boolean isSelectVerb = lowerStep.startsWith("select") || lowerStep.startsWith("choose") || 
                                 lowerStep.contains(" select ") || lowerStep.contains(" choose ");
            
            // For date-like values, we only want to classify as DATE_SET if the verb is associated with setting/filling
            boolean isFillVerb = lowerStep.startsWith("enter") || lowerStep.startsWith("fill") || lowerStep.startsWith("set") ||
                                lowerStep.startsWith("type") || lowerStep.contains(" enter ") || lowerStep.contains(" fill ") || 
                                lowerStep.contains(" set ") || lowerStep.contains(" type ");

            // If it's a select verb and matches "select 'value' from 'target'" pattern, keep it as SELECT
            if (isSelectVerb && lowerStep.contains(" from ")) {
                // Let it fall through
            } else if (isFillVerb && !lowerStep.contains("phone") && 
                !lowerStep.contains("mobile") && 
                !lowerStep.contains("tel") && 
                !lowerStep.contains("number") &&
                !lowerStep.contains("contact")) {
                return ActionType.DATE_SET;
            }
        }
        
        // Classification for SELECT / DROPDOWN
        String targetName = extractTarget(step, ActionType.UNKNOWN).toLowerCase();
        if (lowerStep.contains("set") || lowerStep.contains("choose") || lowerStep.contains("select") || lowerStep.contains("pick")) {
            if (targetName.contains("select") || targetName.contains("menu") || targetName.contains("dropdown")) {
                return ActionType.SELECT;
            }
        }
        
        // PRIORITY 6: Check for other action verbs (LinkedHashMap maintains insertion order)
        // Longer verbs are checked first for specificity
        for (Map.Entry<String, ActionType> entry : ACTION_VERBS.entrySet()) {
            String verb = entry.getKey();
            
            // Skip navigation verbs (already handled above)
            if (Arrays.asList(navigationVerbs).contains(verb)) {
                continue;
            }
            
            // Match verb at start or with word boundaries
            if (lowerStep.startsWith(verb + " ") || 
                lowerStep.contains(" " + verb + " ") ||
                lowerStep.endsWith(" " + verb)) {
                return entry.getValue();
            }
        }
        
        // Default to CLICK for unknown
        logger.debug("Could not determine action type, defaulting to CLICK");
        return ActionType.CLICK;
    }
    
    private String extractTarget(String step, ActionType actionType) {
        return extractTarget(step, actionType, new ArrayList<>());
    }

    /**
     * Extract target element description
     */
    private String extractTarget(String step, ActionType actionType, List<String> valuesToExclude) {
        String target = step;
        
        // Find and remove ONLY the primary action verb that was identified
        String primaryVerb = findPrimaryActionVerb(step);
        if (primaryVerb != null) {
            target = target.replaceFirst("(?i)\\b" + primaryVerb + "\\b", "").trim();
        }
        
        // For FILL/SELECT/CLICK, identifying the target name
        if (actionType != ActionType.UNKNOWN) {
            // Remove identified values specifically to prevent them from interfering with preposition pivoting
            // e.g. "Verify 'Logged in as User' is displayed" -> the 'as' inside quotes shouldn't be a pivot.
            if (valuesToExclude != null) {
                for (String val : valuesToExclude) {
                    target = target.replace("\"" + val + "\"", " ");
                    target = target.replace("'" + val + "'", " ");
                    target = target.replaceAll("(?i)\\b" + Pattern.quote(val) + "\\b", " ");
                }
            }

            // Remove Gherkin keywords
            target = target.replaceAll("(?i)^(Given|When|Then|And|But|User|I)\\s+", "").trim();
            
            // Preposition Pivot: Find the last preposition that likely separates values/verbs from the target
            // e.g. "Select 'A' and 'B' FROM 'Dropdown'" or "Set value TO 'X'"
            Pattern pivotPattern = Pattern.compile("(?i)(.*)\\b(from|in|into|of|for|within|inside|on|to|at|as)\\b\\s+(.+)$");
            Matcher pivotMatcher = pivotPattern.matcher(target);
            
            if (pivotMatcher.find()) {
                String contextPart = pivotMatcher.group(1);
                String prep = pivotMatcher.group(2).toLowerCase();
                String targetPart = pivotMatcher.group(3);
                
                // Rule-based target selection
                if (prep.equals("to") || prep.equals("at")) {
                    // "Set [TARGET] to [VALUE]" or "Enter [VALUE] at [TARGET]"
                    // If targetPart is quoted, it's likely the value. So target is contextPart.
                    if (targetPart.contains("\"") || targetPart.contains("'")) {
                        target = contextPart;
                    } else if (contextPart.contains("\"") || contextPart.contains("'")) {
                        // "Enter [VALUE] as [TARGET]"
                        target = targetPart;
                    } else {
                        target = contextPart;
                    }
                } else if (prep.equals("as")) {
                    // "Enter [VALUE] as [TARGET]"
                    // If contextPart contains common field names, it's likely the target
                    if (contextPart.toLowerCase().contains("name") || contextPart.toLowerCase().contains("email") || contextPart.toLowerCase().contains("phone")) {
                         target = contextPart;
                    } else {
                         target = targetPart;
                    }
                } else if (prep.equals("inside") || prep.equals("within") || prep.equals("on") || prep.equals("under")) {
                    // "Click [TARGET] inside [PARENT]" or "Fill [VALUE] inside [TARGET]"
                    // If it's a FILL action, contextPart is likely the VALUE, so target is targetPart
                    if (actionType == ActionType.FILL || actionType == ActionType.DATE_SET) {
                        target = targetPart;
                    } else {
                        // For CLICK, VERIFY, HOVER, etc., contextPart is the TARGET
                        target = contextPart;
                    }
                } else {
                    // "Select [VALUE] from [TARGET]"
                    target = targetPart;
                }
                
                // Clean up any left-over values that were quoted in the target
                target = target.replaceAll("[\"'][^\"']+[\"']", " ");
            } else {
                // If no preposition, remove action verbs and then all but the last quoted string
                target = target.replaceAll("(?i)\\b(set|select|choose|click|fill|verify|check|type|enter|go to|open)\\b", " ");
                List<String> quoted = extractAllValues(target);
                if (quoted.size() >= 1) {
                    for (int i = 0; i < quoted.size(); i++) {
                        String q = quoted.get(i);
                        if (target.trim().endsWith("\"" + q + "\"") || target.trim().endsWith("'" + q + "'")) {
                             continue;
                        }
                        target = target.replaceFirst("[\"']" + Pattern.quote(q) + "[\"']", " ");
                    }
                }
            }
            
            // Final cleanup
            target = target.replaceAll("(?i)\\b(the|a|an|and|also)\\b", " ");
            target = target.replaceAll("[\"']", " ").replaceAll("\\s+", " ").trim();
        } else {
            // For unknown actions, just strip quotes but keep the text
            target = target.replaceAll("[\"']([^\"']+)[\"']", "$1").trim();
        }
        
        // Remove trailing "as" keyword (for "Enter First Name as 'value'" syntax)
        target = target.replaceAll("(?i)\\s+as\\s*$", "").trim();
        // Also remove "as" + everything after it (handles "First Name as Doe" after value extraction)
        target = target.replaceAll("(?i)\\s+as(?:\\s+.*)?$", "").trim();
        
        // Remove container phrases (e.g., "in the left menu", "on the sidebar")
        target = target.replaceAll("(?i)\\b(in|on|within|inside)\\s+(?:the\\s+)?(left\\s+menu|right\\s+menu|sidebar|navbar|header|footer|menu|top\\s+bar|toolbar|main\\s+content)\\b", "").trim();
        
        // Remove common element type suffixes (e.g., "Elements card" -> "Elements", "Submit button" -> "Submit")
        target = target.replaceAll("(?i)\\s+\\b(button|link|card|field|input|checkbox|radio|dropdown|select|tab|menu|sidebar|navbar|header|footer|icon|image|svg|box|panel|item|link)\\b$", "").trim();
        
        // Normalize whitespace (reduce multiple spaces to single space)
        target = target.replaceAll("\\s+", " ").trim();
        
        // Remove parent scoping phrases if they exist (e.g. "inside 'Form'")
        // We match (inside|within|on|in|under) ["']?Parent["']?
        target = target.replaceAll("(?i)\\b(inside|within|on|under)\\s+[\"']?([^\"']+)[\"']?", "").trim();
        
        // For FILL/DATE_SET/VERIFY actions, extract text after "in"/"into"/"for" BEFORE removing pronouns
        // This is critical because pronouns may appear before "in" (e.g., "I in full name")
        if (actionType == ActionType.FILL || actionType == ActionType.DATE_SET || actionType == ActionType.VERIFY) {
            // Pattern: optional_prefix + (in|into|for|of|on) + field_name
            // Made first group optional with (.+?)? to handle cases like "in Mobile Number" (no prefix)
            Pattern inPattern = Pattern.compile("(?i)(.+?)?\\s*(in|into|for|of|on|within)\\s+(.+)$");
            Matcher matcher = inPattern.matcher(target);
            if (matcher.find()) {
                target = matcher.group(3);  // Get text after the preposition
            } else {
                // If no "in/into" pattern, try to remove pronouns at the start
                target = target.replaceAll("(?i)^(I|user|we|you|he|she|they|it|this|that)\\s+", "");
            }
        } else {
            // For other actions, remove pronouns normally
            target = target.replaceAll("(?i)^(I|user|we|you|he|she|they|it|this|that)\\s+", "");
        }
        
        // Remove common noise words (at start, middle, or end)
        target = target.replaceAll("(?i)^(the|a|an|on|at|to|from|is|are|be|has|have|my|your|our|their|with)\\s+", "");  // At start
        target = target.replaceAll("(?i)\\s+(the|a|an|on|at|to|from|is|are|be|has|have|with)\\s+", " ");  // In middle
        target = target.replaceAll("(?i)\\s+(the|a|an|on|at|to|from|is|are|be|has|have|with)$", "");  // At end
        
        // Remove trailing prepositions and noise
        target = target.replaceAll("(?i)\\s+(with|for|by|text)$", "");
        
        // Remove common verification status suffixes (e.g., "Login message is displayed" -> "Login message")
        target = target.replaceAll("(?i)\\s+(is|are|should|must|was|were|be)?\\s*(be)?\\s*(displayed|visible|present|shown|display|appearing|active|hidden|gone|visibility|appeared)$", "").trim();
        target = target.replaceAll("(?i)\\s+(text|message)\\s+(present|shown|displayed)$", "").trim();

        // AESTHETIC FIX: If the target is just a state word (e.g. "displayed", "visible", "present"), 
        // it's likely not the actual element name but part of the verification phrasing.
        // We should return empty so the framework performs a broad search for the text.
        String trimmedTarget = target.trim().toLowerCase();
        if (trimmedTarget.equals("displayed") || trimmedTarget.equals("visible") || 
            trimmedTarget.equals("present") || trimmedTarget.equals("shown") || 
            trimmedTarget.equals("display") || trimmedTarget.equals("visibility")) {
            return "";  // Return empty to signal broad search
        }
        
        return target.trim();
    }
    
    /**
     * Find the primary action verb in the step (the one that determines the action type)
     */
    private String findPrimaryActionVerb(String step) {
        String lowerStep = step.toLowerCase();
        
        // Check for each action verb in order of appearance
        for (Map.Entry<String, ActionType> entry : ACTION_VERBS.entrySet()) {
            String verb = entry.getKey();
            // Look for verb as a whole word
            if (lowerStep.matches(".*\\b" + verb + "\\b.*")) {
                return verb;
            }
        }
        
        return null;
    }
    
    /**
     * Extract value from quoted strings
     */
    private String extractValue(String step) {
        List<String> values = extractAllValues(step);
        return values.isEmpty() ? null : values.get(0);
    }

    /**
     * Extract all values from quoted strings
     */
    private List<String> extractAllValues(String step) {
        List<String> results = new ArrayList<>();
        Pattern pattern = Pattern.compile("[\"']([^\"']+)[\"']");
        Matcher matcher = pattern.matcher(step);
        
        while (matcher.find()) {
            results.add(matcher.group(1));
        }
        
        return results;
    }
    
    /**
     * Extract modifiers (spatial, visual, ordinal)
     */
    private Map<String, String> extractModifiers(String step) {
        Map<String, String> modifiers = new HashMap<>();
        
        // Container/Scope (e.g., "in the sidebar", "on the left menu")
        Pattern containerPattern = Pattern.compile("(?i)\\b(in|on|within|inside)\\s+(?:the\\s+)?(left\\s+menu|right\\s+menu|sidebar|navbar|header|footer|menu|top\\s+bar|toolbar|main\\s+content)\\b");
        Matcher containerMatcher = containerPattern.matcher(step);
        if (containerMatcher.find()) {
            modifiers.put("container", containerMatcher.group(2).toLowerCase());
        }
        
        // Spatial relationships
        Pattern spatialPattern = Pattern.compile("(?i)(next to|below|above|inside|near|beside|under|over)\\s+(.+?)(?:\\s|$)");
        Matcher spatialMatcher = spatialPattern.matcher(step);
        if (spatialMatcher.find()) {
            modifiers.put("spatial_relation", spatialMatcher.group(1));
            modifiers.put("spatial_reference", spatialMatcher.group(2));
        }
        
        // Visual attributes
        Pattern colorPattern = Pattern.compile("(?i)(red|blue|green|yellow|orange|purple|black|white|gray|grey)\\s");
        Matcher colorMatcher = colorPattern.matcher(step);
        if (colorMatcher.find()) {
            modifiers.put("color", colorMatcher.group(1));
        }
        
        Pattern sizePattern = Pattern.compile("(?i)(large|small|big|tiny|huge)\\s");
        Matcher sizeMatcher = sizePattern.matcher(step);
        if (sizeMatcher.find()) {
            modifiers.put("size", sizeMatcher.group(1));
        }
        
        // Ordinal positions
        Pattern ordinalPattern = Pattern.compile("(?i)(first|second|third|fourth|fifth|last|1st|2nd|3rd|\\d+th)\\s");
        Matcher ordinalMatcher = ordinalPattern.matcher(step);
        if (ordinalMatcher.find()) {
            modifiers.put("position", ordinalMatcher.group(1));
        }
        
        return modifiers;
    }
    
    /**
     * Extract element type hints
     */
    private String extractElementType(String step) {
        String lowerStep = step.toLowerCase();
        
        // Check for explicit type mentions (ordered by specificity)
        String[] types = {
            // Form elements
            "button", "link", "field", "input", "checkbox", "radio", 
            "dropdown", "select", "textarea", "label", "image", "icon",
            
            // Interactive elements (NEW for 122 verbs)
            "toggle", "switch", "slider", "accordion", "tooltip",
            
            // Layout elements
            "menu", "tab", "modal", "dialog", "popup", "form",
            "sidebar", "navbar", "footer", "header", "panel",
            
            // Modern UI components
            "chip", "badge", "card", "breadcrumb", "notification",
            "alert", "banner", "carousel", "spinner"
        };
        
        for (String type : types) {
            if (lowerStep.contains(type)) {
                return type;
            }
        }
        
        return null;
    }
    
    /**
     * Extract parent reference from phrases like "inside 'Form'", "within the sidebar"
     */
    private String extractParentReference(String step) {
        // Priority 1: Quoted parent after scoping word
        Pattern quotedParent = Pattern.compile("(?i)\\b(inside|within|on|under|at)\\s+(?:the\\s+)?(?:element|section|container|block|div|span|area|group|form)?\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
        Matcher m1 = quotedParent.matcher(step);
        if (m1.find()) {
            return m1.group(2);
        }
        
        // Priority 2: Named common containers (legacy/semantic)
        Pattern namedParent = Pattern.compile("(?i)\\b(inside|within|on|in)\\s+(?:the\\s+)?(left\\s+menu|right\\s+menu|sidebar|navbar|header|footer|menu|top\\s+bar|toolbar|main\\s+content|registration\\s+form|login\\s+box|search\\s+bar|nav)\\b", Pattern.CASE_INSENSITIVE);
        Matcher m2 = namedParent.matcher(step);
        if (m2.find()) {
            return m2.group(2);
        }
        
        return null;
    }
    
    /**
     * Check if a text looks like a date or relative date keyword
     */
    private boolean isDateValue(String text) {
        String lower = text.toLowerCase().trim();
        
        // Relative date keywords
        if (lower.equals("today") || lower.equals("tomorrow") || lower.equals("yesterday")) return true;
        
        // Exclude phone numbers (10 digits, common in many countries)
        // Phone numbers like "9876543210" should NOT be treated as dates
        if (text.matches("^\\d{10}$")) return false;
        
        // Matches numeric offsets like "+30 days", "5 days from today" (NOT just numbers alone)
        if (lower.matches("^[+-]?\\d{1,3}\\s+days?(\\s+from\\s+today)?$")) return true;
        
        // Pure numbers alone should NOT be treated as dates (they are values/slider positions/offsets)
        if (lower.matches("^[+-]?\\d+$")) return false;
        
        // Matches common date formats (simplistic check)
        // MM/DD/YYYY, YYYY-MM-DD, Month Day Year
        if (lower.matches("^\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}$")) return true;
        if (lower.matches("^\\d{4}[/-]\\d{1,2}[/-]\\d{1,2}$")) return true;
        
        String[] months = {
            "january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december",
            "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"
        };
        for (String month : months) {
            if (lower.matches(".*\\b" + month + "\\b.*")) return true;
        }
        
        return false;
    }
    
    /**
     * Action type enumeration
     */
    public enum ActionType {
        CLICK,
        FILL,
        VERIFY,
        SELECT,
        NAVIGATE,
        WAIT,
        HOVER,
        SCROLL,
        DATE_SET,
        EXPAND,
        COLLAPSE,
        UNKNOWN
    }
}
