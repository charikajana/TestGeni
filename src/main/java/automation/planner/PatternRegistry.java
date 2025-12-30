package automation.planner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Central registry for all step matching patterns.
 * Organized by category for easy maintenance and updates.
 * 
 * Each pattern is defined with:
 * - Action type (navigate, fill, click, etc.)
 * - Regex pattern
 * - Group indices for element, value, and row anchor
 */
public class PatternRegistry {
    
    /**
     * Registers all patterns by calling the provided registration function.
     * 
     * @param register Function that accepts (actionType, regex, elementGroup, valueGroup, rowAnchorGroup)
     */
    public static void registerAllPatterns(PatternRegistrar register) {
        
        // ========================================
        // NAVIGATION PATTERNS
        // ========================================
        // Click and switch to new window with intelligent element extraction
        // Supports: "click 'Home' and switch", "click on Home link and switch", "click Home button and switch to new window"
        register.add("click_and_switch_window",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:click|tap)\\s+(?:on\\s+)?(?:the\\s+)?[\"']?([^\"']+?)[\"']?\\s*(?:link|button|icon|element|img)?\\s*(?:and|&)\\s*(?:switch|navigate|go)\\s+to\\s+(?:the\\s+)?(?:new|second|latest)\\s+(?:window|tab)",
            1, -1, -1);

        register.add("navigate", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:open|go to|navigate to|launch|visit)\\s+(?:the\\s+)?(?:url|website|site|page)?\\s*[\"']?([^\"']+)[\"']?", 
            1, -1, -1);
        
        // Navigate to application by name (reads URL from config)
        // Example: "Open Browser and Navigate to HotelBooker"
        register.add("navigate_app", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:open|launch)\\s+(?:the\\s+)?browser\\s+and\\s+navigate\\s+to\\s+(.+)$", 
            1, -1, -1);

        // ========================================
        // SCROLL PATTERNS
        // ========================================
        
        // Scroll page to position: "scroll to bottom", "scroll to top of page"
        register.add("scroll", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:scroll|move)\\s+(?:to\\s+)?(top|bottom|middle|start|end)(?:\\s+(?:of|on)\\s+(?:the\\s+)?(?:page|screen|window))?$", 
            -1, 1, -1);

        // Scroll by amount: "scroll down 500 pixels", "scroll up 100 px"
        register.add("scroll", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:scroll|move)\\s+(down|up|left|right)(?:\\s+by)?\\s+(\\d+)(?:\\s*(?:px|pixels?))?", 
            1, 2, -1);

        // Scroll within element: "scroll right in image gallery", "scroll to bottom of product list"
        register.add("scroll", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:scroll|move)\\s+(?:to\\s+)?(top|bottom|left|right|down|up)(?:\\s+of|\\s+in|\\s+inside)\\s+[\"']?([^\"']+)[\"']?$", 
            2, 1, -1); // Element name is group 2, Direction/Position is group 1

        // Scroll to specific element: "scroll to Contact Us", "scroll to element with id '...'"
        // This is a catch-all scroll, so it goes last in this block
        register.add("scroll", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:scroll|move)\\s+(?:to|into|until)\\s+(?:view\\s+of\\s+)?(?:the\\s+)?[\"']?(.+?)[\"']?$", 
            1, -1, -1);
        
        // ========================================
        // AUTOCOMPLETE / SUGGESTION PATTERNS
        // ========================================
        // Enter value from autocomplete suggestions
        // Example: "enters location 'Dallas' from suggestion"
        register.add("fill_autocomplete", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:enter|type|select)s?\\s+(.+?)\\s+[\"']([^\"']+)[\"']\\s+from\\s+(?:suggestion|dropdown|list|autocomplete)$", 
            1, 2, -1);
        
        // ========================================
        // DATE SELECTION PATTERNS
        // ========================================
        // Select relative date (X days from today/tomorrow)
        // Example: "selects arrival date 25 days from today"
        register.add("select_date_relative", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*selects?\\s+(.+?)\\s+(\\d+)\\s+days?\\s+from\\s+(today|tomorrow)$", 
            1, 2, -1);

        // Set absolute date
        // Example: Set "05/20/2026" in "Select Date"
        register.add("set_date",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:set|select|enter)\\s+(?:the\\s+)?(?:date\\s+)?[\"']([^\"']+)[\"']\\s+(?:in|for|into|to)\\s+(?:the\\s+)?(?:date\\s+picker|field|input)?\\s*[\"']?([^\"']+)[\"']?$",
            2, 1, -1);
        
        // Natural language date: Select date "today" for "Birth Date"
        register.add("set_date",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:set|select|enter)\\s+(?:the\\s+)?(?:date\\s+)?(?:of\\s+)?(today|tomorrow|yesterday)\\s+(?:in|for|into|to)\\s+(?:the\\s+)?[\"']?([^\"']+)[\"']?$",
            2, 1, -1);
        
        // ========================================
        // CREDENTIALS PATTERNS (Phase 2)
        // ========================================
        // Enter username and password from config
        // Example: "user enters username and password"
        register.add("fill_credentials", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*enters?\\s+username\\s+and\\s+password$", 
            -1, -1, -1);
        
        // ========================================
        // MULTI-CRITERIA SELECTION (Phase 2)
        // ========================================
        // Select with multiple criteria
        // Example: "Select the Rate Plan from 'Sabre' with refundable 'No'"
        register.add("select_with_criteria", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*select\\s+(?:the\\s+)?(.+?)\\s+from\\s+[\"']([^\"']+)[\"']\\s+with\\s+(.+?)\\s+[\"']([^\"']+)[\"']$", 
            1, 2, -1);
        
        // ========================================
        // FORM SECTION AUTO-FILL (Phase 2)
        // ========================================
        // Auto-fill entire form sections from config
        // Example: "Add Booking Contact details", "Add Traveller details"
        register.add("fill_form_section", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*add\\s+(.+?)\\s+details$", 
            1, -1, -1);
        
        // ========================================
        // SETTINGS TOGGLE (Phase 3)
        // ========================================
        // Toggle settings/checkboxes/switches
        // Example: "disable Travel Policy In Agency Admin"
        register.add("toggle_setting", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(enable|disable)\\s+(.+?)(?:\\s+in\\s+(.+))?$", 
            2, 1, -1);
        
        // ========================================
        // DYNAMIC DATA / CONTEXT (Phase 3)
        // ========================================
        // Store extracted data to context
        // Example: "store the Booking Reference Number"
        register.add("store_context", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*store\\s+(?:the\\s+)?(.+?)(?:\\s+as\\s+(.+))?$", 
            1, 2, -1);
        
        // Retrieve and use stored data
        // Example: "user enters booking reference" (uses stored value)
        register.add("enter_stored_reference", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*enters?\\s+booking\\s+reference$", 
            -1, -1, -1);
        
        // ========================================
        // SCREENSHOT PATTERNS
        // ========================================
        register.add("screenshot", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:take|capture)\\s+(?:a\\s+)?(?:screen\\s?shot|snap\\s?shot)", 
            -1, -1, -1);
        
        // ========================================
        // WAIT PATTERNS
        // ========================================
        // Time-based waits: "wait for 20 seconds", "wait 20 sec", "wait 5s"
        register.add("wait_time", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:wait|pause)(?:\\s+for)?\\s+(\\d+)\\s*(?:second|sec|s)(?:s)?", 
            1, -1, -1);
        
        // Wait for element to disappear: "wait for 'Loading' to disappear"
        register.add("wait_disappear", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:wait|pause)(?:\\s+for|\\s+until)?\\s+[\"']?([^\"']+)[\"']?\\s+(?:to\\s+)?(?:disappear|hide|be\\s+hidden|is\\s+gone|vanish|not\\s+visible)", 
            1, -1, -1);
        
        // Wait for element to appear: "wait for 'Submit' to appear"
        register.add("wait_appear", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:wait|pause)(?:\\s+for|\\s+until)?\\s+[\"']?([^\"']+)[\"']?\\s+(?:to\\s+)?(?:appear|show|be\\s+visible|is\\s+visible|display|be\\s+displayed)", 
            1, -1, -1);
        
        // Page load waits: "wait for page load", "wait for page to be loaded"
        register.add("wait_page", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:wait|pause)(?:\\s+for)?\\s+(?:page|network)(?:\\s+to)?(?:\\s+be)?(?:\\s+)?(?:load(?:ed)?(?:\\s+completed)?|idle|ready)", 
            -1, -1, -1);

        // Progress bar verification: "Monitor the progress until reach '25'"
        register.add("wait_for_progress",
            "(?i)(?:wait|pause|monitor).*?(progress|loading).*?(?:until\\s+reach|until|reach|to|is|be)\\s+[\"']([^\"']+)[\"']",
            1, 2, -1);
        
        // ========================================
        // KEYBOARD ACTIONS (MUST BE BEFORE CLICK PATTERN)
        // ========================================
        // Press key: "Press Escape", "Press Enter key", "I press Escape to close modal"
        // Press key: "Press Escape", "Press Enter key", "I press Escape to close modal", "I press Ctrl+S", "Press Tab 5 times"
        // Press key: "Press Escape", "Press Enter key", "I press Escape to close modal", "I press Ctrl+S", "Press Tab 5 times"
        register.add("press_key", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:press|hit|type)\\s+(Escape|Enter|Tab|Space|Delete|Backspace|ArrowUp|ArrowDown|ArrowLeft|ArrowRight)(?:\\s+key)?(?:\\s+to\\s+.+)?$", 
            1, -1, -1);
        
        // Refresh/reload page: "I refresh the page", "reload page", "refresh browser"
        register.add("refresh_page", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:refresh|reload)(?:\\s+(?:the|current))?\\s+(?:page|browser|screen)$", 
            -1, -1, -1);
        
        // Browser back: "I go back", "navigate back", "browser back", "click browser back button"
        register.add("browser_back", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:go|navigate|move|browser|click|press)?\\s*(?:browser\\s*)?back(?:wards?)?\\s*(?:button|icon)?$", 
            -1, -1, -1);
        
        // Browser forward: "I go forward", "navigate forward", "browser forward", "click forward button"
        register.add("browser_forward", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:go|navigate|move|browser|click|press)?\\s*(?:browser\\s*)?forward(?:s)?\\s*(?:button|icon)?$", 
            -1, -1, -1);
        
        
        // Verify validation state: "Verify 'first name' field is invalid", "Verify 'mobile' has red border"
        register.add("verify_validation", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:verify|check|ensure)\\s+(?:the\\s+)?[\"']?([^\"']+)[\"']?\\s+(?:field|box|input)?\\s*(?:is\\s+invalid|has\\s+red\\s+border|shows\\s+error|is\\s+required)$", 
            1, -1, -1);
        
        // ========================================
        // ALERT / CONFIRM / PROMPT PATTERNS
        // ========================================
        
        // Verify alert message: "Verify alert says 'message'"
        register.add("verify_alert", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:verify|check|assert)\\s+(?:alert|popup|dialog)\\s+(?:says|shows|displays|contains|has|message)\\s+[\"']([^\"']+)[\"']", 
            -1, 1, -1);
        
        // Accept alert with message verification: "Accept alert with message 'text'"
        register.add("accept_alert", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:accept|ok|confirm|close)\\s+(?:alert|popup|dialog)(?:\\s+with)?(?:\\s+message)?(?:\\s+)?[\"']?([^\"']*)[\"']?$", 
            -1, 1, -1);
        
        // Verify and accept alert: "Verify and accept alert with 'message'"
        register.add("accept_alert", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:verify\\s+and\\s+)?(?:accept|ok|confirm)\\s+(?:alert|popup|dialog)\\s+(?:with|having)?\\s+[\"']([^\"']+)[\"']", 
            -1, 1, -1);
        
        // Accept confirm dialog: "Accept confirm"
        register.add("accept_alert", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:accept|ok|click\\s+ok|confirm)\\s+(?:confirm|confirmation)", 
            -1, -1, -1);
        
        // Dismiss alert/confirm: "Dismiss alert", "Dismiss confirm", "Cancel confirm"
        register.add("dismiss_alert", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:dismiss|cancel|close|reject)\\s+(?:alert|confirm|popup|dialog|confirmation)", 
            -1, -1, -1);
        
        // Enter text in prompt: "Enter 'text' in prompt"
        register.add("prompt_alert", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:enter|type|input|provide)\\s+[\"']([^\"']+)[\"']\\s+(?:in|into|to)\\s+(?:prompt|input\\s+dialog)", 
            -1, 1, -1);
        
        // Dismiss prompt: "Dismiss prompt"
        register.add("dismiss_prompt", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:dismiss|cancel|close)\\s+(?:prompt|input\\s+dialog)", 
            -1, -1, -1);
        

        
        // ========================================
        // TABLE-SCOPED ACTIONS
        // ========================================
        // Click button in row: Click "Edit" in the row identifying "John"
        // Click button in row: Click "Edit" in the row identifying "John"
        register.add("click", 
            "(?i)^(?:when|and|then)?\\s*(?:I|user|we|he|she|they)?\\s*(?:click|tap)\\s+[\"']([^\"']+)[\"']\\s+(?:in|for|at|on)\\s+(?:the\\s+)?(?:row|record)\\s+(?:identifying|for|with|containing)\\s+[\"']([^\"']+)[\"']", 
            1, -1, 2);
        
        // Fill in row: Enter "5000" in "Salary" for the row with "John"
        // Fill in row: Enter "5000" in "Salary" for the row with "John"
        register.add("fill", 
            "(?i)^(?:when|and|then)?\\s*(?:I|user|we|he|she|they)?\\s*(?:enter|fill|type)\\s+[\"']([^\"']+)[\"']\\s+(?:in|into|for)\\s+[\"']([^\"']+)[\"']\\s+(?:in|for|at)\\s+(?:the\\s+)?(?:row|record)\\s+(?:identifying|for|with|containing)\\s+[\"']([^\"']+)[\"']", 
            2, 1, 3);
        
        // Verify in row: Verify "5000" is displayed in the row for "John"
        register.add("verify", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:verify|assert)\\s+[\"']([^\"']+)[\"']\\s+is\\s+displayed\\s+(?:in|for|at)\\s+(?:the\\s+)?(?:row|record)\\s+(?:identifying|for|with|containing)\\s+[\"']([^\"']+)[\"']", 
            -1, 1, 2);
        
        // ========================================
        // VERIFICATION PATTERNS (HIGH PRIORITY)
        // ========================================
        // Title verification: "Verify page title is 'Elements'", "Check page title contains '...'"
        register.add("verify_page_title", 
            "(?i)^(?:then|and|when)?\\s*(?:validate|verify|check|ensure)\\s+(?:that\\s+)?(?:the\\s+)?page\\s+title\\s+(?:is|contains|equals|has|matches)\\s+[\"']?([^\"']+)[\"']?$", 
            -1, 1, -1);
            
        // URL verification: "Verify URL contains '/elements'", "Verify URL is exactly '...'", "Check domain is '...'"
        register.add("verify_url", 
            "(?i)^(?:then|and|when)?\\s*(?:validate|verify|check|ensure|assert)\\s+(?:that\\s+)?(?:the\\s+)?(?:current\\s+)?URL\\s+(?:contains|is|exactly|is\\s+exactly|starts\\s+with|has|matches|equals)\\s+[\"']?([^\"']+)[\"']?(?:\\s+again|\\s+viewed|\\s+now)?$", 
            -1, 1, -1);
        
        register.add("verify_url", 
            "(?i)^(?:then|and|when)?\\s*(?:validate|verify|check|ensure|assert)\\s+(?:that\\s+)?(?:the\\s+)?(?:URL\\s+)?(path|domain|hash|parameter|query)\\s+(?:is|contains|equals|matches)\\s+[\"']?([^\"']+)[\"']?(?:\\s+again|\\s+now)?$", 
            -1, 2, -1);
            
        // Specific state verification for menu items (PRIORITY)
        register.add("verify_selected", 
            "(?i)^(?:then|and|when)?\\s*(?:validate|verify|check|ensure)\\s+(?:the\\s+)?active\\s+menu\\s+item\\s+(?:is|should\\s+be)\\s+[\"']([^\"']+)[\"']$", 
            1, -1, -1);
            
        // ========================================
        // SELECTION VERIFICATION & MULTISELECT
        // ========================================
        
        // Multiselect items from list (e.g., Select multiple items 'X;Y')
        // Maps value (group 1) to plan.getValue() which MultiselectAction uses
        register.add("multiselect_item", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*select\\s+(?:multiple\\s+items\\s+|items\\s+)?[\"']([^\"']+)[\"'](?:\\s+from\\s+(?:list|grid|table))?$", 
            -1, 1, -1);
            
        // Verify items 'X;Y' are selected
        register.add("verify_selected", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:validate|verify|assert|check)\\s+(?:items\\s+)?[\"']([^\"']+)[\"']\\s+are\\s+(?:selected|checked)$", 
            1, -1, -1);
            
        // Verify 'X' is selected (Supports unquoted)
        register.add("verify_selected", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:validate|verify|assert|check)\\s+(?:that\\s+)?(?:the\\s+)?(.+?)\\s+(?:is|should\\s+be|are)\\s+(?:selected|checked)$", 
            1, -1, -1);
            
        // Verify 'X' is not selected (Supports unquoted)
        register.add("verify_not_selected", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:validate|verify|assert|check)\\s+(?:that\\s+)?(?:the\\s+)?(.+?)\\s+(?:is|should\\s+be|are)\\s+(?:not|un)\\s*(?:selected|checked)$", 
            1, -1, -1);

        // Homepage/Base URL shorthand: "Verify I'm back on homepage", "Check URL is base URL"
        register.add("verify_url", 
            "(?i)^(?:then|and|when)?\\s*(?:validate|verify|check|ensure|assert)\\s+(?:that\\s+)?(?:I'm\\s+|I\\s+am\\s+)?(?:back\\s+on\\s+)?(?:the\\s+)?(?:homepage|base\\s+URL|root\\s+URL|start\\s+page)$", 
            -1, -1, -1);
            
        
        // ========================================
        // MOVED STATE VERIFICATION PATTERNS
        // (Must be checked before generic verify_value)
        // ========================================
        
        // State verification (Enabled/Disabled)
        // Specific phrasal variations first to avoid greedy capture issues
        register.add("verify_enabled", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:validate|verify|assert|check)\\s+(?:that\\s+)?(?:the\\s+)?(.+?)\\s+(?:is\\s+not\\s+disabled|should\\s+be\\s+enabled)(?:\\s+in\\s+(?:the\\s+)?(?:row|record)\\s+(?:identifying|for|with|containing)\\s+[\"']([^\"']+)[\"'])?$", 
            1, -1, 2);
        
        register.add("verify_disabled", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:validate|verify|assert|check)\\s+(?:that\\s+)?(?:the\\s+)?(.+?)\\s+(?:is\\s+not\\s+enabled|should\\s+be\\s+disabled)(?:\\s+in\\s+(?:the\\s+)?(?:row|record)\\s+(?:identifying|for|with|containing)\\s+[\"']([^\"']+)[\"'])?$", 
            1, -1, 2);

        register.add("verify_enabled", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:validate|verify|assert|check)\\s+(?:that\\s+)?(?:the\\s+)?(.+?)\\s+(?:is\\s+)?(?:enabled|isEnabled|active|clickable|interactive)(?:\\s+in\\s+(?:the\\s+)?(?:row|record)\\s+(?:identifying|for|with|containing)\\s+[\"']([^\"']+)[\"'])?$", 
            1, -1, 2);
        
        register.add("verify_disabled", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:validate|verify|assert|check)\\s+(?:that\\s+)?(?:the\\s+)?(.+?)\\s+(?:is\\s+)?(?:disabled|isDisabled|greyed out|grayed out|inactive|read-only|readonly|restricted)(?:\\s+in\\s+(?:the\\s+)?(?:row|record)\\s+(?:identifying|for|with|containing)\\s+[\"']([^\"']+)[\"'])?$", 
            1, -1, 2);
        
        // State verification (Checked/Selected)
        register.add("verify_not_selected", 
            "(?i)^(?:then|and|when)?\\s*(?:validate|verify|check|ensure)\\s+(?:that\\s+)?(?:the\\s+)?(.+?)\\s+(?:is\\s+)?(?:not\\s+selected|not\\s+checked|not\\s+chosen|unchecked|off|should\\s+not\\s+be\\s+selected)(?:\\s+in\\s+(?:the\\s+)?(?:row|record)\\s+(?:identifying|for|with|containing)\\s+[\"']([^\"']+)[\"'])?$", 
            1, -1, 2);

        register.add("verify_selected", 
            "(?i)^(?:then|and|when)?\\s*(?:validate|verify|check|ensure)\\s+(?:that\\s+)?(?:the\\s+)?(.+?)\\s+(?:is\\s+)?(?:selected|checked|on|chosen|active|expanded|open|should\\s+be\\s+selected)(?:\\s+in\\s+(?:the\\s+)?(?:row|record)\\s+(?:identifying|for|with|containing)\\s+[\"']([^\"']+)[\"'])?$", 
            1, -1, 2);
            
        // Specific state verification for menu items (PRIORITY)
        register.add("verify_selected", 
            "(?i)^(?:then|and|when)?\\s*(?:validate|verify|check|ensure)\\s+(?:the\\s+)?active\\s+menu\\s+item\\s+(?:is|should\\s+be)\\s+[\"']([^\"']+)[\"']$", 
            1, -1, -1);
            
        // ========================================
        // TEXT VISIBILITY VERIFICATION (PRIORITY)
        // ========================================
        // Format: Then Validate "Target Text" [message/text] [should be] [visible/displayed]
        register.add("verify", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:validate|verify|assert|check|ensure|I\\s+see)\\s+[\"']([^\"']+)[\"'](?:\\s+(?:text|message|label|heading|info|message/text))?\\s*(?:is|are|should\\s+be|should\\s+be\\s+transparently)?\\s*(?:present|shown|displayed|visible|display|be\\s+displayed|be\\s+visible)$", 
            1, -1, -1);
        
        // Format: Then Validate [the] [text/message] "Target Text" [is] [visible/displayed]
        register.add("verify", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:validate|verify|assert|check|ensure|I\\s+see)\\s+(?:the\\s+)?(?:text|message|label|heading|title|info|content|message/text)\\s+[\"']([^\"']+)[\"'](?:\\s+(?:is|are|should\\s+be)?\\s*(?:present|shown|displayed|visible|display|be\\s+displayed|be\\s+visible))?$", 
            1, -1, -1);

        register.add("verify_not", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:validate|verify|assert|check)\\s+[\"']([^\"']+)[\"']\\s+(?:that\\s+)?(?:it\\s+)?(?:is\\s+)?not\\s+(?:displayed|visible|present|shown)$", 
            -1, 1, -1);
            
        // Greedy/Element-based visibility (LOW PRIORITY)
        register.add("verify_not", 
           "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:validate|verify|assert|check)\\s+(?:that\\s+)?(?:the\\s+)?(.+?)\\s+[\"']([^\"']+)[\"']\\s+(?:is\\s+)?not\\s+(?:displayed|visible|present|shown)", 
           1, 2, -1);
       
        register.add("verify", 
           "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:validate|verify|assert|check)\\s+(?:that\\s+)?(?:the\\s+)?(.+?)\\s+[\"']([^\"']+)[\"']\\s+(?:is\\s+)?(?:displayed|visible|present)", 
           1, 2, -1);
       
        register.add("verify_not", 
           "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:validate|verify|assert|check|should\\s+be)\\s+(?:that\\s+)?(?:the\\s+)?(.+?)\\s+(?:is\\s+|are\\s+)?not\\s+(?:displayed|visible|present)(?:\\s+[\"']([^\"']+)[\"'])?", 
           1, 2, -1);
       
        register.add("verify", 
           "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:validate|verify|assert|check|should\\s+be)\\s+(?:that\\s+)?(?:the\\s+)?(.+?)\\s+(?:is\\s+|are\\s+)?(?:displayed|visible|present|equals|contains)(?:\\s+[\"']([^\"']+)[\"'])?", 
           1, 2, -1);

        // ========================================
        // Handles: Verify "name@example.com" with Email placeholder
        // Pattern: Verify  "value" with FieldName placeholder
        register.add("verify_placeholder", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:validate|verify|assert|check)\\s+[\"']([^\"']+)[\"']\\s+(?:with|in|for)\\s+(.+?)\\s+(?:place\\s*holder|placeholder|place holder)$", 
            2, 1, -1);
            
        // Handles: Verify Full Name placeholder value "Full Name"
        // Pattern: Verify FieldName placeholder value "value"
        register.add("verify_placeholder", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:validate|verify|assert|check)\\s+(.+?)\\s+(?:place\\s*holder|placeholder|place holder)\\s+(?:value|text|is)?\\s*[\"']([^\"']+)[\"']$", 
            1, 2, -1);
        
        // Verify value in field: Verify "Alice" is filled in first name field
        register.add("verify_value", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:validate|verify|assert|check)\\s+[\"']?([^\"']+)[\"']?\\s+(?:is\\s+filled\\s+in|is|appears\\s+in)\\s+(?:the\\s+)?(.+?)(?:\\s+field|\\s+box|\\s+input)?$", 
            2, 1, -1);
            
        
        // Negative verification
        register.add("verify_not", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:validate|verify|assert|check)\\s+[\"']([^\"']+)[\"']\\s+(?:message|text)?\\s*(?:should\\s+not\\s+be|should\\s+not)\\s*(?:display|displayed|present|shown|visible)", 
            -1, 1, -1);
        
        register.add("verify_not", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:validate|verify|assert|check)\\s+[\"']([^\"']+)[\"']\\s+(?:this\\s+)?(?:text|message)\\s+(?:should\\s+)?not\\s+(?:be\\s+)?(?:display|displayed|present|shown|visible)", 
            -1, 1, -1);
        
        // (State verification patterns moved up before verify_value)
        

        // ========================================
        // FILL/INPUT PATTERNS
        // ========================================
        // Pattern 1: "Enter 'value' in element" (strict, most common)
        register.add("fill", 
            "(?i)^(?:I|user|we|he|she|they)?\\s*(?:enter|fill|type|input)\\s+[\"']([^\"']+)[\"']\\s+(?:into|in|to|for)\\s+(?:the\\s+)?[\"']?([^\"']+)[\"']?", 
            2, 1, -1);
        
        // Pattern 2: Natural language with filler words
        // "Enter given value 'X' in element", "Enter the text 'X' in element"
        // Pattern 2: Natural language with filler words
        // "Enter given value 'X' in element", "Enter the text 'X' in element"
        register.add("fill", 
            "(?i)^(?:I|user|we|he|she|they)?\\s*(?:enter|fill|type|input)\\s+(?:given\\s+value|the\\s+text|the\\s+value|given\\s+text|value|text)?\\s*[\"']([^\"']+)[\"']\\s+(?:into|in|to|for)\\s+(?:the\\s+)?[\"']?([^\"']+)[\"']?", 
            2, 1, -1);
        
        // Pattern 3: "Fill element with 'value'" with variations
        // Supports: Set/Update/Change element with given value/the text/the value 'X'
        register.add("fill", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:fill|enter|type|input|set|update|change)\\s+(?:the\\s+)?(.+?)\\s+with\\s+(?:given\\s+value|the\\s+text|the\\s+value|given\\s+text|value|text)?\\s*[\"']([^\"']+)[\"']", 
            1, 2, -1);
        
        // Pattern 4: Backward compatibility - "Fill element: 'value'" or "Fill element 'value'"
        register.add("fill", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:fill|enter|type|input|write)\\s+(?:the\\s+)?([\\w\\s\\-]+?)(?:[: ])?\\s+[\"']([^\"']+)[\"']", 
            1, 2, -1);
        
        // Nested Menu Selection
        // Example: Select "Main Item 2 > SUB SUB LIST > Sub Sub Item 1" from menu
        register.add("select_menu", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:select|choose|click|navigate\\s+to|open)\\s+[\"']?([^\"']+)[\"']?\\s+(?:from|in|using|via)\\s+(?:the\\s+)?(?:navigation\\s+|sidebar\\s+|top\\s+)?(?:menu|navbar|nav|menu\\s*bar)$", 
            -1, 1, -1);

        // Multi-value select (MUST BE FIRST): Select "A" and "B" and "C" from "Dropdown"
        register.add("select_multi", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:select|choose)\\s+[\"']([^\"']+)[\"'](?:\\s+and\\s+[\"'][^\"']+[\"'])+\\s+(?:from|in|for)\\s+(?:the\\s+)?[\"']?([^\"']+)[\"']?", 
            2, 1, -1);
        
        // Single value select
        register.add("select", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:select|choose)\\s+[\"']([^\"']+)[\"']\\s+(?:from|in|for)\\s+(?:the\\s+)?[\"']?([^\"']+)[\"']?", 
            2, 1, -1);
        
        register.add("select", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:select|choose)\\s+(?:the\\s+)?[\"']?([^\"']+)[\"']?\\s+(?:from|in|for)\\s+[\"']([^\"']+)[\"']", 
            1, 2, -1);
        
        register.add("select", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:set|change)\\s+(?:the\\s+)?(?:dropdown\\s+|select\\s+)?[\"']?([^\"']+)[\"']?\\s+to\\s+[\"']([^\"']+)[\"']", 
            1, 2, -1);
        
        // ========================================
        // SELECTABLE LIST PATTERNS (Non-Dropdown)
        // ========================================
        // Examples: "Select 'Cras justo odio' from list", "Select multiple items 'One;Two;Three'"
        register.add("multiselect_item", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:select|choose)\\s+(?:multiple\\s+items\\s+)?[\"']([^\"']+)[\"'](?:\\s+from\\s+(?:the\\s+)?(?:list|grid))?$", 
            -1, 1, -1);

        // Verification for selectable lists
        register.add("verify_selected", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:validate|verify|check)\\s+(?:that\\s+)?(?:items?\\s+)?[\"']([^\"']+)[\"']\\s+(?:is|are)\\s+selected$", 
            -1, 1, -1);

        // Deselect patterns (for multiselect dropdowns)
        register.add("deselect", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:deselect|remove|unselect|clear)\\s+[\"']([^\"']+)[\"']\\s+(?:from|in|for)\\s+(?:the\\s+)?[\"']?([^\"']+)[\"']?", 
            2, 1, -1);

        // Positive verification with various phrasings
        register.add("verify", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:validate|verify|assert|check)\\s+[\"']([^\"']+)[\"']\\s+(?:this\\s+)?(?:text|message|item)\\s+(?:is\\s+)?(?:present|shown|displayed|visible|selected)", 
            -1, 1, -1);
        
        // ========================================
        // CHECKBOX PATTERNS
        // ========================================
        register.add("check", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:check|tick|mark)\\s+(?:the\\s+)?(?:checkbox\\s+|box\\s+)?[\"']?([^\"']+)[\"']?", 
            1, -1, -1);
        
        register.add("uncheck", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:uncheck|untick|unmark)\\s+(?:the\\s+)?(?:checkbox\\s+|box\\s+)?[\"']?([^\"']+)[\"']?", 
            1, -1, -1);
        
        // ========================================
        // CLICK PATTERNS (order matters - more specific first)
        // ========================================
        register.add("double_click", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*double\\s+(?:click|tap)\\s+(?:on\\s+)?(?:the\\s+)?[\"']?([^\"']+)[\"']?", 
            1, -1, -1);
        
        register.add("right_click", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*right\\s+(?:click|tap)\\s+(?:on\\s+)?(?:the\\s+)?[\"']?([^\"']+)[\"']?", 
            1, -1, -1);
        
        register.add("click", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:click|tap|press|hit)\\s+(?:on\\s+)?(?:the\\s+)?[\"']?([^\"']+)[\"']?", 
            1, -1, -1);

        // Fallback for click: "Click on anything..."
        register.add("click", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:click|press|tap)(?:\\s+on)?\\s+(.+?)$", 
            1, -1, -1);
    }
    
    /**
     * Registers all table and frame-related patterns.
     * These are more complex patterns used by SmartStepParser for table operations,
     * window/tab management, alerts, iframes, modals, and multiselect lists.
     * 
     * @param register Function that accepts (actionType, regex, groupMap)
     */
    public static void registerTablePatterns(TablePatternRegistrar register) {
        
        // ========================================
        // URL VERIFICATION PATTERNS
        // ========================================
        // Verify URL exactly matches: "Verify Current URL exactly matches 'https://example.com'"
        register.add("verify_url",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*" +
            "(?:verify|check|assert)\\s+(?:current\\s+|page\\s+)?url\\s+" +
            "(?:exactly\\s+)?(?:matches|is|equals)\\s+" +
            "[\"']([^\"']+)[\"']",
            Map.of("value", 1));
        
        // Verify URL contains: "Verify URL contains 'search'"
        register.add("verify_url",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*" +
            "(?:verify|check|assert)\\s+(?:current\\s+|page\\s+)?url\\s+" +
            "contains\\s+" +
            "[\"']([^\"']+)[\"']",
            Map.of("value", 1));
        
        // ========================================
        // SLIDER / RANGE INPUT (Top Priority in Table Patterns)
        // ========================================
        // Set slider value: "Set slider to '75'", "Move Volume to '80'", "Adjust slider '90'"
        register.add("set_slider",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*" +
            "(?:set|move|adjust|slide|drag|change)\\s+" +
            "(?:(?:the|a|an)\\s+)?" +
            "((?:.+?\\s+)?(?:slider|range|volume|brightness|zoom|percentage|offset|pos|position|level|intensity|value|speed|rate|progress))\\s*" +
            "(?:slider)?\\s*" +
            "(?:to|at)\\s+" +
            "[\"']([^\"']+)[\"']",
            Map.of("elementName", 1, "value", 2));

        // ========================================
        // TABLE STRUCTURE VALIDATION
        // ========================================
        register.add("table_visible", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:the\\s+)?[\"']?([^\"']+)[\"']?\\s+table\\s+(?:is\\s+)?(?:visible|displayed|present)",
            Map.of("tableName", 1));
            
        register.add("table_has_column",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:the\\s+)?[\"']?([^\"']+)[\"']?\\s+table\\s+should\\s+(?:have|contain)\\s+column\\s+[\"']([^\"']+)[\"']",
            Map.of("tableName", 1, "columnName", 2));
            
        register.add("table_row_count",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:the\\s+)?[\"']?([^\"']+)[\"']?\\s+table\\s+should\\s+have\\s+(?:at least|exactly|at most)\\s+(\\d+)\\s+rows?",
            Map.of("tableName", 1, "rowCount", 2));
        
        // ========================================
        // ROW LEVEL OPERATIONS
        // ========================================
        register.add("row_exists",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*a\\s+row\\s+should\\s+exist\\s+where\\s+[\"']([^\"']+)[\"']\\s+is\\s+[\"']([^\"']+)[\"']",
            Map.of("columnName", 1, "value", 2));
            
        register.add("row_not_exists",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*a\\s+row\\s+should\\s+not\\s+exist\\s+where\\s+[\"']([^\"']+)[\"']\\s+is\\s+[\"']([^\"']+)[\"']",
            Map.of("columnName", 1, "value", 2));
        
        register.add("row_added_with_value",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*verify\\s+(?:new\\s+)?row\\s+is\\s+(?:added|created|inserted)\\s+with\\s+[\"']([^\"']+)[\"']\\s+in\\s+(?:the\\s+)?[\"']?([^\"']+)[\"']?\\s+column",
            Map.of("value", 1, "columnName", 2));
            
        register.add("row_cell_validation",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*in\\s+the\\s+row\\s+where\\s+[\"']([^\"']+)[\"']\\s+is\\s+[\"']([^\"']+)[\"'],\\s+[\"']([^\"']+)[\"']\\s+should\\s+be\\s+[\"']([^\"']+)[\"']",
            Map.of("conditionColumn", 1, "conditionValue", 2, "targetColumn", 3, "expectedValue", 4));
        
        // ========================================
        // CELL VALIDATION
        // ========================================
        register.add("cell_value_by_position",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:the\\s+)?cell\\s+at\\s+row\\s+(\\d+)\\s+and\\s+column\\s+[\"']([^\"']+)[\"']\\s+should\\s+be\\s+[\"']([^\"']+)[\"']",
            Map.of("rowNumber", 1, "columnName", 2, "expectedValue", 3));
        
        
        // ========================================
        // ROW ACTIONS - Enhanced for ALL natural language variations
        // ========================================
        
        // Pattern 1: Click with quoted button name
        // Supports: Click "Edit" in/for/on row where/with/having Name is "Vinoth"
        register.add("click_in_row",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*clicks?\\s+[\"']([^\"']+)[\"']\\s+(?:in|for|on)\\s+(?:the\\s+)?row\\s+(?:where|with|having|that\\s+has)\\s+[\"']?([^\"']+?)[\"']?\\s+(?:column\\s+)?(?:value\\s+)?(?:is|=|equals?)\\s+[\"']([^\"']+)[\"']",
            Map.of("buttonName", 1, "conditionColumn", 2, "conditionValue", 3));
        
        // Pattern 2: Click with natural button description (flexible)
        // Supports: Click on/the Edit Button/icon/link in/for/on row where/with Name is "Vinoth"
        register.add("click_specific_in_row",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*clicks?\\s+(?:on|one|the|a|an)?\\s*(.+?)\\s+(?:in|for|on)\\s+(?:the\\s+)?row\\s+(?:where|with|having|that\\s+has)\\s+[\"']?([^\"']+?)[\"']?\\s+(?:column\\s+)?(?:value\\s+)?(?:is|=|equals?)\\s+[\"']([^\"']+)[\"']",
            Map.of("buttonName", 1, "conditionColumn", 2, "conditionValue", 3));
        
        // Pattern 3: Click by row position (first, last, 3rd, row 3)
        // Supports: Click Edit button in first/last/3rd/row 3
        register.add("click_in_row_position",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*clicks?\\s+(?:on|the)?\\s*(.+?)\\s+(?:in|at)\\s+(?:the\\s+)?(first|last|\\d+(?:st|nd|rd|th)?|row\\s+\\d+)\\s+row",
            Map.of("buttonName", 1, "rowPosition", 2));
        
        // Pattern 4: Direct action on row (Edit/Delete the row where...)
        // Supports: Edit/Delete/Remove the row where Name is "Vinoth"
        register.add("direct_row_action",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(edit|delete|remove|update|modify)\\s+(?:the\\s+)?row\\s+(?:where|with|having|that\\s+has)\\s+[\"']?([^\"']+?)[\"']?\\s+(?:column\\s+)?(?:value\\s+)?(?:is|=|equals?)\\s+[\"']([^\"']+)[\"']",
            Map.of("actionType", 1, "conditionColumn", 2, "conditionValue", 3));
            
        // Pattern 5: Select checkbox with all variations
        // Supports: Select checkbox in/for/on row where/with/having Name column value is "Vinoth"
        register.add("select_checkbox_in_row",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*selects?\\s+(?:the\\s+)?checkbox\\s+(?:in|for|on)\\s+(?:the\\s+)?row\\s+(?:where|with|having|that\\s+has)\\s+[\"']?([^\"']+?)[\"']?\\s+(?:column\\s+)?(?:value\\s+)?(?:is|=|equals?)\\s+[\"']([^\"']+)[\"']",
            Map.of("conditionColumn", 1, "conditionValue", 2));
        
        register.add("get_row_values",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:get|extract|retrieve|fetch)\\s+all\\s+(?:column\\s+)?values?\\s+(?:from\\s+(?:the\\s+)?row\\s+)?(?:where|with|having)\\s+[\"']?([^\"']+)[\"']?\\s+(?:is|=|equals?)\\s+[\"']([^\"']+)[\"']",
            Map.of("conditionColumn", 1, "conditionValue", 2));
        
        register.add("verify_row_not_exists",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:validate|verify|check|ensure)\\s+(?:that\\s+)?(?:the\\s+)?row\\s+should\\s+not\\s+(?:be\\s+)?(?:present|exist)\\s+where\\s+[\"']?([^\"']+)[\"']?\\s+is\\s+[\"']([^\"']+)[\"']",
            Map.of("conditionColumn", 1, "conditionValue", 2));

        // State verification in rows
        register.add("verify_enabled",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:validate|verify|assert|check)\\s+(?:that\\s+)?(?:the\\s+)?(.+?)\\s+(?:is\\s+not\\s+disabled|should\\s+be\\s+enabled)\\s+in\\s+(?:the\\s+)?(?:row|record)\\s+where\\s+[\"']([^\"']+)[\"']\\s+is\\s+[\"']([^\"']+)[\"']",
            Map.of("elementName", 1, "conditionColumn", 2, "conditionValue", 3));

        register.add("verify_disabled",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:validate|verify|assert|check|ensure)\\s+(?:that\\s+)?(?:the\\s+)?(.+?)\\s+(?:is\\s+not\\s+enabled|should\\s+be\\s+disabled)\\s+in\\s+(?:the\\s+)?(?:row|record)\\s+where\\s+[\"']([^\"']+)[\"']\\s+is\\s+[\"']([^\"']+)[\"']",
            Map.of("elementName", 1, "conditionColumn", 2, "conditionValue", 3));

        register.add("verify_enabled",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:validate|verify|assert|check)\\s+(?:that\\s+)?(?:the\\s+)?(.+?)\\s+(?:is\\s+)?(?:enabled|isEnabled|active|clickable|interactive)\\s+in\\s+(?:the\\s+)?(?:row|record)\\s+where\\s+[\"']([^\"']+)[\"']\\s+is\\s+[\"']([^\"']+)[\"']",
            Map.of("elementName", 1, "conditionColumn", 2, "conditionValue", 3));

        register.add("verify_disabled",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:validate|verify|assert|check|ensure)\\s+(?:that\\s+)?(?:the\\s+)?(.+?)\\s+(?:is\\s+)?(?:disabled|isDisabled|greyed out|grayed out|inactive|read-only|readonly|restricted)\\s+in\\s+(?:the\\s+)?(?:row|record)\\s+where\\s+[\"']([^\"']+)[\"']\\s+is\\s+[\"']([^\"']+)[\"']",
            Map.of("elementName", 1, "conditionColumn", 2, "conditionValue", 3));

        // Selection verification in rows
        register.add("verify_not_selected",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:validate|verify|assert|check)\\s+(?:that\\s+)?(?:the\\s+)?(.+?)\\s+(?:is\\s+not\\s+selected|is\\s+not\\s+checked|is\\s+not\\s+chosen|is\\s+unchecked|is\\s+off|should\\s+not\\s+be\\s+selected)\\s+in\\s+(?:the\\s+)?(?:row|record)\\s+where\\s+[\"']([^\"']+)[\"']\\s+is\\s+[\"']([^\"']+)[\"']",
            Map.of("elementName", 1, "conditionColumn", 2, "conditionValue", 3));

        register.add("verify_selected",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:validate|verify|assert|check)\\s+(?:that\\s+)?(?:the\\s+)?(.+?)\\s+(?:is\\s+)?(?:selected|checked|on|chosen|should\\s+be\\s+selected)\\s+in\\s+(?:the\\s+)?(?:row|record)\\s+where\\s+[\"']([^\"']+)[\"']\\s+is\\s+[\"']([^\"']+)[\"']",
            Map.of("elementName", 1, "conditionColumn", 2, "conditionValue", 3));

        // ========================================
        // BROWSER LIFECYCLE
        // ========================================
        register.add("close_browser",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:close|quit|exit)\\s+(?:the\\s+)?browser",
            Map.of());
        
        // ========================================
        // WINDOW/TAB MANAGEMENT
        // ========================================
        register.add("switch_to_new_window",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:switch|navigate|go)\\s+to\\s+(?:the\\s+)?(?:new|second|latest)\\s+(?:window|tab)",
            Map.of());


        
        register.add("switch_to_main_window",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:switch|navigate|go)\\s+(?:back\\s+)?to\\s+(?:the\\s+)?(?:main|first|original|parent)\\s+(?:window|tab)",
            Map.of());
        
        register.add("close_current_window",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:close|shut)\\s+(?:the\\s+)?(?:current|this)\\s+(?:window|tab)",
            Map.of());
        
        register.add("verify_window_count", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:validate|verify|check|ensure)\\s+(?:the\\s+)?(?:current\\s+)?(?:tab|window)\\s+count\\s+(?:is|equals|should\\s+be)\\s+(\\d+)", 
            Map.of("value", 1));
            
        register.add("verify_window_exists", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:validate|verify|check|ensure)\\s+(?:that\\s+)?(?:the\\s+)?(?:original|main|first|parent)\\s+tab\\s+(?:still\\s+)?exists", 
            Map.of());

        register.add("verify_window_exists", 
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:validate|verify|check|ensure)\\s+(?:that\\s+)?(?:a\\s+)?(?:new|second|latest)\\s+(?:window|tab)\\s+(?:exists|is\\s+open|opened|created|present)", 
            Map.of());
            
        register.add("close_window",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:close|shut)\\s+(?:the\\s+)?(?:second|new|latest)\\s+(?:window|tab)",
            Map.of());

        // ========================================
        // ALERT HANDLING
        // ========================================
        register.add("accept_alert",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:accept|click ok on|confirm|click ok|ok)\\s+(?:the\\s+)?(?:alert|confirm|dialog)",
            Map.of());
        
        register.add("accept_alert",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:accept|confirm)\\s+(?:the\\s+)?(?:alert|confirm)\\s+(?:with\\s+message|saying|that says)\\s+[\"']([^\"']+)[\"']",
            Map.of("value", 1));
        
        register.add("accept_alert",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:verify|check|assert)\\s+(?:the\\s+)?(?:alert|confirm)\\s+(?:says|message is|contains|shows)\\s+[\"']([^\"']+)[\"']",
            Map.of("value", 1));
        
        register.add("accept_alert",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:verify|check)\\s+(?:and\\s+)?(?:accept|confirm)\\s+(?:the\\s+)?(?:alert|confirm)\\s+(?:with|saying|shows)\\s+[\"']([^\"']+)[\"']",
            Map.of("value", 1));
            
        register.add("dismiss_alert",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:dismiss|cancel|close|click cancel|click cancel on)\\s+(?:the\\s+)?(?:alert|confirm|dialog|popup alert)",
            Map.of());
            
        register.add("prompt_alert",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:enter|type|input)\\s+[\"']([^\"']+)[\"']\\s+(?:in|into|to)\\s+(?:the\\s+)?prompt",
            Map.of("value", 1));

        register.add("prompt_alert",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:accept|confirm|click ok on)\\s+(?:the\\s+)?prompt",
            Map.of());
            
        register.add("prompt_alert",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:dismiss|cancel)\\s+(?:the\\s+)?prompt",
            Map.of());

        // ========================================
        // IFRAME MANAGEMENT
        // ========================================
        register.add("switch_to_frame",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:switch|focus|go)\\s+to\\s+(?:the\\s+)?(?:iframe|frame)\\s+[\"']?([^\"']+)[\"']?",
            Map.of("frameName", 1));
            
        register.add("switch_to_main_frame",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:switch|focus|go)\\s+(?:back\\s+)?to\\s+(?:the\\s+)?(?:main\\s+content|top\\s+frame|parent\\s+frame)",
            Map.of());

        // ========================================
        // MODAL DIALOGS
        // ========================================
        register.add("verify_modal_visible",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:validate|verify|check|ensure)\\s+(?:that\\s+)?(?:the\\s+)?modal\\s+(?:dialog\\s+)?(?:is\\s+)?(?:visible|displayed|present)",
            Map.of());
            
        register.add("verify_modal_visible",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:validate|verify|check|ensure)\\s+(?:that\\s+)?(?:the\\s+)?modal\\s+(?:dialog\\s+)?with\\s+(?:title|header)\\s+[\"']([^\"']+)[\"']\\s+(?:is\\s+)?(?:visible|displayed|present)",
            Map.of("value", 1));

        register.add("verify_modal_not_visible",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:validate|verify|check|ensure)\\s+(?:that\\s+)?(?:the\\s+)?modal\\s+(?:dialog\\s+)?(?:is\\s+)?not\\s+(?:visible|displayed|present)",
            Map.of());

        register.add("verify_modal_not_visible",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:validate|verify|check|ensure)\\s+(?:that\\s+)?(?:the\\s+)?modal\\s+(?:dialog\\s+)?with\\s+(?:title|header)\\s+[\"']([^\"']+)[\"']\\s+(?:is\\s+)?not\\s+(?:visible|displayed|present)",
            Map.of("value", 1));

        register.add("close_modal",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:close|dismiss|exit|shut|hide)\\s+(?:the\\s+)?(?:modal|dialog|dialog box|popup)",
            Map.of());

        // ========================================
        // MULTISELECT LISTS
        // ========================================
        register.add("multiselect_item",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:select|choose|pick)\\s+[\"']([^\"']+)[\"']\\s+(?:from|in)\\s+(?:the\\s+)?(list|grid)",
            Map.of("value", 1));
        
        register.add("multiselect_item",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:select|choose|pick)\\s+(?:multiple\\s+)?(?:items?|values?)\\s+[\"']([^\"']+)[\"']",
            Map.of("value", 1));
        
        // ========================================
        // DESELECT / REMOVE FROM MULTISELECT/AUTOCOMPLETE
        // ========================================
        // Remove/deselect options from multiselect or autocomplete
        // Example: "remove 'Green' from Type multiple color names"
        register.add("deselect",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:remove|deselect|clear|delete)\\s+[\"']([^\"']+)[\"']\\s+from\\s+(.+?)$",
            Map.of("value", 1, "elementName", 2));
        
        // ========================================
        // SLIDER / RANGE INPUT
        // PROGRESS BAR / MONITORING
        // ========================================
        // Progress bar verification removed (moved to registerAllPatterns)
        
        // ========================================
        // MOUSE ACTIONS / TOOLTIPS
        // ========================================
        // Hover: "Hover over the 'Submit' button", "Mouse over 'Profile' icon"
        // Hover: "hover over 'Electronics'", "mouse over 'Powerbank'", "move mouse to 'Cart'", "point to 'Menu'"
        register.add("hover",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*" +
            "(?:hover|mouse\\s*over|move\\s+mouse\\s+to|point\\s+to|focus\\s+on|places?\\s+cursor\\s+on)\\s+(?:over\\s+|to\\s+)?(?:the\\s+)?" +
            "[\"']?([^\"']+)[\"']?",
            Map.of("elementName", 1));

        // Verify Tooltip: "Verify tooltip of 'Button' contains 'You hovered'"
        register.add("verify_tooltip",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*" +
            "(?:verify|check)\\s+tooltip\\s+(?:of|for)\\s+" +
            "[\"']?([^\"']+)[\"']?\\s+" +
            "(?:contains|is)\\s+" +
            "[\"']([^\"']+)[\"']",
            Map.of("elementName", 1, "value", 2));
        
        // Verify tooltip appears with text: "Verify tooltip 'More information' appears"
        register.add("verify_tooltip",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*" +
            "(?:verify|check)\\s+tooltip\\s+" +
            "[\"']([^\"']+)[\"']\\s+" +
            "(?:appears|is displayed|is visible|shows|is shown)",
            Map.of("value", 1));
        
        // Verify tooltip disappears: "Verify tooltip disappears"
        register.add("verify_tooltip",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*" +
            "(?:verify|check)\\s+tooltip\\s+" +
            "(?:disappears|is hidden|is not visible|hides|is not displayed)",
            Map.of("value", ""));
        
        // Overly broad pattern - commented out to prevent false matches
        // register.add("multiselect_item",
        //     "^(?:when|and)?\\s*(?:multi-?select|select)\\s+(.+)",
        //     Map.of("value", 1));
        
        register.add("verify_selected",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:verify|check|assert)\\s+(?:that\\s+)?(?:items?\\s+)?[\"']([^\"']+)[\"']\\s+(?:is|are)\\s+selected",
            Map.of("value", 1));
        
        register.add("verify_selected",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:verify|check|assert)\\s+(?:that\\s+)?(?:items?|values?)\\s+[\"']([^\"']+)[\"']\\s+(?:is|are)\\s+selected",
            Map.of("value", 1));
        
        register.add("verify_not_selected",
            "(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:verify|check|assert)\\s+(?:that\\s+)?(?:items?\\s+)?[\"']([^\"']+)[\"']\\s+(?:is|are)\\s+not\\s+selected",
            Map.of("value", 1));
        
        // Note: Additional patterns for sorting, filtering, pagination can be added here
        // They follow the same structure as above
    }
    
    
    /**
     * Functional interface for basic pattern registration (used by StepPlanner)
     */
    @FunctionalInterface
    public interface PatternRegistrar {
        void add(String actionType, String regex, int elementGroup, int valueGroup, int rowAnchorGroup);
    }
    
    /**
     * Functional interface for table pattern registration (used by SmartStepParser)
     */
    @FunctionalInterface
    public interface TablePatternRegistrar {
        void add(String actionType, String regex, java.util.Map<String, Object> groupMap);
    }
    
    /**
     * Get list of combined action patterns (for step validation)
     */
    public static List<Pattern> getCombinedActions() {
        // Combined actions are multi-step patterns like "click and switch window"
        List<Pattern> patterns = new ArrayList<>();
        patterns.add(Pattern.compile("(?i)^(?:given|when|then|and|but)?\\s*(?:I|user|we|he|she|they)?\\s*(?:click|tap)\\s+(?:on\\s+)?(?:the\\s+)?[\"']?([^\"']+?)[\"']?\\s*(?:link|button|icon|element|img)?\\s*(?:and|&)\\s*(?:switch|navigate|go)\\s+to\\s+(?:the\\s+)?(?:new|second|latest)\\s+(?:window|tab)"));
        return patterns;
    }
    
    /**
     * Get all registered patterns (for step validation)
     */
    public static Map<String, Pattern> getAllPatterns() {
        Map<String, Pattern> allPatterns = new HashMap<>();
        
        // Collect all patterns by registering them
        registerAllPatterns((actionType, regex, elementGroup, valueGroup, rowAnchorGroup) -> {
            allPatterns.put(actionType + "_" + allPatterns.size(), Pattern.compile(regex));
        });
        
        return allPatterns;
    }
}
