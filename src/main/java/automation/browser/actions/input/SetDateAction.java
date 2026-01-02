package automation.browser.actions.input;

import automation.browser.SmartLocator;
import automation.browser.actions.BrowserAction;
import automation.planner.ActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Action to set a date in a date picker field.
 * Patterns:
 * - Set "05/20/2026" in "Select Date" 
 * - Select date "today" for "Arrival"
 */
public class SetDateAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(SetDateAction.class);
    
    @Override
    public boolean execute(Page page, SmartLocator smartLocator, ActionPlan plan) {
        String elementName = plan.getElementName();
        String dateText = plan.getValue();
        
        if (dateText == null || dateText.isEmpty()) {
            logger.error("No date value provided");
            return false;
        }
        
        LocalDate targetDate = parseDate(dateText);
        if (targetDate == null) {
            logger.error("Could not parse date: {}", dateText);
            return false;
        }
        
        logger.info("Setting date for '{}' to: {}", elementName, targetDate);
        
        try {
            Locator dateField = null;

            // 1. Try to use intelligent locator if already found during planning
            if (plan.hasMetadata("intelligent_locator")) {
                Locator intelligentLocator = (Locator) plan.getMetadataValue("intelligent_locator");
                String resolvedPageUrl = (String) plan.getMetadataValue("resolved_page_url");
                String currentPageUrl = page.url();
                
                if (intelligentLocator != null) {
                    if (resolvedPageUrl != null && !resolvedPageUrl.equals(currentPageUrl)) {
                        logger.warn("Page URL changed - discarding stale pre-resolved locator");
                    } else {
                        logger.debug("Using pre-resolved intelligent locator for: {}", elementName);
                        dateField = intelligentLocator;
                    }
                }
            }

            // 2. Find the date input field if not already found
            if (dateField == null) {
                dateField = smartLocator.waitForSmartElement(elementName, "input", null, plan.getFrameAnchor());
            }
            
            if (dateField == null) {
                logger.error("Could not find date field: {}", elementName);
                return false;
            }
            
            smartLocator.recordMatch(plan);
            
            // Interaction Strategy:
            // 1. Check if field is read-only first
            // 2. Direct Input (Fastest & most reliable if enabled)
            // 3. JS Fallback (Force set value)
            // 4. Calendar Picker (If typing is blocked)
            
            boolean isReadOnly = false;
            try {
                Object evalResult = dateField.evaluate("el => el.readOnly || el.hasAttribute('readonly')");
                // Safely handle the result - it might be a Boolean, or a complex object
                if (evalResult instanceof Boolean) {
                    isReadOnly = (Boolean) evalResult;
                } else if (evalResult != null) {
                    // If it's a complex object (like LinkedHashMap from some libraries), convert to string and check
                    isReadOnly = Boolean.parseBoolean(evalResult.toString());
                }
                if (isReadOnly) {
                    logger.debug("Field is read-only, skipping direct input");
                }
            } catch (Exception e) {
                // If we can't check, assume it's not read-only
                logger.debug("Could not determine readonly status: {}", e.getMessage());
            }
            
            boolean success = false;
            String expectedValue = targetDate.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
            
            // Try different strategies until one succeeds and is validated
            for (int attempt = 0; attempt < 1; attempt++) { // One pass through strategies
                if (isReadOnly) {
                    success = tryJSSet(dateField, targetDate) ||
                             tryCalendarPicker(page, dateField, targetDate);
                } else {
                    success = tryDirectInput(dateField, targetDate, page) ||
                             tryJSSet(dateField, targetDate) ||
                             tryCalendarPicker(page, dateField, targetDate);
                }
                
                // VALIDATION: Ensure current field value contains at least the day or some part of the date
                // Many date pickers format the date specifically, so we check for substring or year
                String actualValue = (String) dateField.evaluate("el => el.value || el.innerText || ''");
                String dayStr = String.valueOf(targetDate.getDayOfMonth());
                String yearStr = String.valueOf(targetDate.getYear());
                
                if (success && (actualValue.contains(dayStr) || actualValue.contains(yearStr))) {
                    logger.success("Successfully set and verified date {} for {}", targetDate, elementName);
                    return true;
                }
            }
            
            if (success) {
                // If success was true but validation failed, it's likely a false positive (like a div)
                logger.error("Date set reported success but validation failed! Actual value: '{}'", 
                    (String) dateField.evaluate("el => el.value || el.innerText || ''"));
                return false;
            } else {
                logger.error("Failed to set date for '{}' using all available strategies", elementName);
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Error setting date: {}", e.getMessage());
            return false;
        }
    }
    
    private LocalDate parseDate(String text) {
        String lower = text.toLowerCase().trim();
        
        // Handle numeric offsets (e.g. "30", "+30", "-5", "30 days", "30 days from today")
        if (lower.matches("^[+-]?\\d+(\\s+days?)?(\\s+from\\s+today)?$")) {
            try {
                String numOnly = lower.replaceAll("[^0-9+-]", "");
                if (!numOnly.isEmpty()) {
                    return LocalDate.now().plusDays(Long.parseLong(numOnly));
                }
            } catch (Exception e) {
                // Fall through
            }
        }

        if (lower.equals("today")) return LocalDate.now();
        if (lower.equals("tomorrow")) return LocalDate.now().plusDays(1);
        if (lower.equals("yesterday")) return LocalDate.now().minusDays(1);
        
        // Try common formats
        String[] formats = {"MM/dd/yyyy", "dd/MM/yyyy", "yyyy-MM-dd", "MMMM d, yyyy", "d MMMM yyyy", "MMM d, yyyy", "MMMM d yyyy"};
        for (String format : formats) {
            try {
                return LocalDate.parse(text, DateTimeFormatter.ofPattern(format));
            } catch (Exception e) {
                // Continue
            }
        }
        
        return null;
    }
    
    private boolean tryDirectInput(Locator dateField, LocalDate targetDate, Page page) {
        try {
            // Try common date formats
            String[] formats = {"MM/dd/yyyy", "dd/MM/yyyy", "yyyy-MM-dd", "dd-MM-yyyy", "MM-dd-yyyy"};
            
            for (String format : formats) {
                try {
                    String dateString = targetDate.format(DateTimeFormatter.ofPattern(format));
                    logger.debug("Trying direct input with format {}: {}", format, dateString);
                    
                    dateField.focus();
                    dateField.press("Control+A");
                    dateField.press("Backspace");
                    dateField.fill(dateString);
                    dateField.press("Enter");
                    
                    page.waitForTimeout(200);
                    
                    // Check if input has value and it's not empty
                    String val = dateField.inputValue();
                    if (val != null && !val.isEmpty()) {
                        logger.debug("Direct input success with format {}", format);
                        return true;
                    }
                } catch (Exception e) {
                    continue;
                }
            }
        } catch (Exception e) {
            logger.debug("Direct input failed: {}", e.getMessage());
        }
        return false;
    }
    
    private boolean tryJSSet(Locator dateField, LocalDate targetDate) {
        try {
            String[] formats = {"MM/dd/yyyy", "yyyy-MM-dd", "dd/MM/yyyy"};
            for (String format : formats) {
                String dateString = targetDate.format(DateTimeFormatter.ofPattern(format));
                logger.debug("Trying JavaScript force set ({}): {}", format, dateString);
                dateField.evaluate("(el, val) => { " +
                    "el.value = val; " +
                    "el.dispatchEvent(new Event('input', {bubbles: true})); " +
                    "el.dispatchEvent(new Event('change', {bubbles: true})); " +
                    "el.dispatchEvent(new Event('blur', {bubbles: true})); " +
                    "}", dateString);
            }
            return true; 
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean tryCalendarPicker(Page page, Locator dateField, LocalDate targetDate) {
        try {
            logger.debug("Trying Calendar Picker UI strategy...");
            
            if (page.isClosed()) return false;
            
            // Focus and click to trigger popup
            dateField.scrollIntoViewIfNeeded();
            dateField.click();
            page.waitForTimeout(500); 
            
            // 1. Identify the Calendar Container
            Locator calendar = page.locator(
                ".react-datepicker, .ui-datepicker, .flatpickr-calendar, .datepicker, " +
                ".ds-datepicker, .calendar, [role='dialog'], [role='grid'], .popover, " +
                ".dropdown-menu, .datepick-popup, .dp-popup, .vdp-datepicker"
            ).filter(new Locator.FilterOptions().setHas(page.locator("text=" + targetDate.getDayOfMonth()))).first();

            if (!calendar.isVisible()) {
                // Fallback: find any visible container with the day text
                calendar = page.locator("div:visible, section:visible").filter(new Locator.FilterOptions().setHas(page.locator("text=" + targetDate.getDayOfMonth()))).last();
            }

            if (!calendar.isVisible()) {
                logger.debug("No visible calendar container found");
                return false;
            }
            
            // 2. Handle Month/Year Selection
            handleMonthYearSelection(calendar, targetDate);
            
            // 3. Selection Strategy for the Day
            int day = targetDate.getDayOfMonth();
            
            // A. Try exact aria-label/title match (Highest precision)
            String ariaDay = targetDate.format(DateTimeFormatter.ofPattern("MMMM d")); 
            Locator dayByAria = calendar.locator(String.format("[aria-label*='%s'], [title*='%s'], [aria-label*=' %d ']", ariaDay, ariaDay, day)).first();
            if (dayByAria.isVisible()) {
                dayByAria.click();
                return true;
            }
            
            // B. Try matching roles or classes that explicitly look like days
            // We focus on elements that contain ONLY the day number to avoid matching headers
            String[] cellSelectors = {
                "[role='gridcell']", "[role='option']", ".react-datepicker__day", 
                ".day", ".ui-state-default", ".flatpickr-day", ".day-item"
            };
            
            for (String sel : cellSelectors) {
                Locator cell = calendar.locator(sel).filter(new Locator.FilterOptions().setHasText(String.valueOf(day))).first();
                // Check if it's the current month (avoid disabled/outside days if possible)
                if (cell.isVisible() && !cell.getAttribute("class").contains("outside") && !cell.getAttribute("class").contains("disabled")) {
                    cell.click();
                    return true;
                }
            }
            
            // C. Generic text match (last resort)
            Locator genericDay = calendar.locator(String.format("text=%d", day)).last();
            if (genericDay.isVisible()) {
                genericDay.click();
                return true;
            }
            
        } catch (Exception e) {
            logger.debug("Calendar picker failed: {}", e.getMessage());
        }
        return false;
    }

    private void handleMonthYearSelection(Locator calendar, LocalDate targetDate) {
        try {
            // Month Selectors
            Locator monthSelect = calendar.locator("select[class*='month'], select[class*='Month'], .month-select").first();
            if (monthSelect.isVisible()) {
                try { 
                    monthSelect.selectOption(new com.microsoft.playwright.options.SelectOption().setIndex(targetDate.getMonthValue() - 1)); 
                } catch (Exception e) {
                    try { monthSelect.selectOption(targetDate.format(DateTimeFormatter.ofPattern("MMMM"))); } catch (Exception e2) {}
                }
            }
            
            // Year Selectors
            Locator yearSelect = calendar.locator("select[class*='year'], select[class*='Year'], .year-select").first();
            if (yearSelect.isVisible()) {
                yearSelect.selectOption(String.valueOf(targetDate.getYear()));
            }
        } catch (Exception e) {
            logger.debug("Month/Year adjustment failed: {}", e.getMessage());
        }
    }
}
