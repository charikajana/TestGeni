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
            // Find the date input field
            // We search for input, but also support broad search if it's a custom component
            Locator dateField = smartLocator.waitForSmartElement(elementName, "input", null, null);
            
            if (dateField == null) {
                // Try searching without the 'input' restriction in case it's a div acting as a button
                dateField = smartLocator.waitForSmartElement(elementName, null, null, null);
            }
            
            if (dateField == null) {
                logger.error("Could not find date field: {}", elementName);
                return false;
            }
            
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
            
            boolean success;
            if (isReadOnly) {
                // Skip direct input for read-only fields
                success = tryJSSet(dateField, targetDate) ||
                         tryCalendarPicker(page, dateField, targetDate);
            } else {
                // Try all strategies including direct input
                success = tryDirectInput(dateField, targetDate, page) ||
                         tryJSSet(dateField, targetDate) ||
                         tryCalendarPicker(page, dateField, targetDate);
            }
            
            if (success) {
                logger.success("Successfully set date to {} for {}", targetDate, elementName);
                return true;
            } else {
                logger.error("Failed to set date using all available strategies");
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
            // Try formatting based on what common inputs expect
            String dateString = targetDate.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
            logger.debug("Trying direct input: {}", dateString);
            
            dateField.focus();
            dateField.press("Control+A");
            dateField.press("Backspace");
            dateField.fill(dateString);
            dateField.press("Enter");
            
            page.waitForTimeout(500);
            
            // Check if input has value
            String val = dateField.inputValue();
            return val != null && !val.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean tryJSSet(Locator dateField, LocalDate targetDate) {
        try {
            String dateString = targetDate.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
            logger.debug("Trying JavaScript value set: {}", dateString);
            dateField.evaluate("el => { " +
                "el.value = '" + dateString + "'; " +
                "el.dispatchEvent(new Event('change', {bubbles: true})); " +
                "el.dispatchEvent(new Event('input', {bubbles: true})); " +
                "}");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean tryCalendarPicker(Page page, Locator dateField, LocalDate targetDate) {
        try {
            logger.debug("Trying Calendar Picker UI strategy...");
            
            // Safety check: ensure page is still open
            if (page.isClosed()) {
                logger.debug("Page is closed, cannot interact with calendar");
                return false;
            }
            
            // Focus and click to trigger popup
            dateField.scrollIntoViewIfNeeded();
            dateField.click();
            page.waitForTimeout(1000); // Give calendar time to animate/render
            
            // 1. Identify the Calendar Container
            // We use a broad set of common selectors, including ARIA roles
            Locator calendar = page.locator(
                ".react-datepicker, .ui-datepicker, .flatpickr-calendar, .datepicker, " +
                ".ds-datepicker, .calendar, [role='dialog'], [role='grid'], .popover, " +
                ".dropdown-menu, .datepick-popup"
            ).filter(new Locator.FilterOptions().setHas(page.locator("text=" + targetDate.getDayOfMonth()))).first();

            if (!calendar.isVisible()) {
                // Try finding any visible element with a high z-index or absolute position that appears after click
                calendar = page.locator("div:visible").filter(new Locator.FilterOptions().setHas(page.locator("text=" + targetDate.getDayOfMonth()))).last();
            }

            if (!calendar.isVisible()) {
                logger.debug("No visible calendar container found");
                return false;
            }
            
            // 2. Handle Month/Year Selection
            handleMonthYearSelection(calendar, targetDate);
            
            // 3. Selection Strategy for the Day
            // A. Try exact aria-label (Best for Accessibility-compliant sites like DemoQA)
            String fullMonthName = targetDate.format(DateTimeFormatter.ofPattern("MMMM"));
            String ariaDay = targetDate.format(DateTimeFormatter.ofPattern("MMMM d"));
            
            Locator dayByAria = calendar.locator(String.format("[aria-label*='%s'], [title*='%s']", ariaDay, ariaDay)).first();
            if (dayByAria.isVisible()) {
                dayByAria.click();
                return true;
            }
            
            // B. Try matching role="gridcell" or "option" with the day text
            Locator cell = calendar.locator(String.format("[role='gridcell']:text-is('%1$d'), [role='option']:text-is('%1$d'), .day:text-is('%1$d')", targetDate.getDayOfMonth())).first();
            if (cell.isVisible()) {
                cell.click();
                return true;
            }
            
            // C. Generic numeric text click within container (excluding header/sidebar)
            // We look for elements that look like days (small width/height)
            Locator genericDay = calendar.locator(String.format("text-is('%d')", targetDate.getDayOfMonth()))
                .filter(new Locator.FilterOptions().setHasNot(calendar.locator("select, .header, .month-nav")))
                .first();
                
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
            // Try standard SELECT elements
            Locator monthSelect = calendar.locator("select[class*='month'], select[class*='Month']").first();
            if (monthSelect.isVisible()) {
                // Try both index (0 or 1 based) and text
                try { monthSelect.selectOption(String.valueOf(targetDate.getMonthValue() - 1)); } catch (Exception e) {}
            }
            
            Locator yearSelect = calendar.locator("select[class*='year'], select[class*='Year']").first();
            if (yearSelect.isVisible()) {
                yearSelect.selectOption(String.valueOf(targetDate.getYear()));
            }

            // If no selects, maybe it's a "Click to switch" type header (like Flatpickr)
            // This part is complex and usually requires specific library knowledge
            // For now, we rely on the month/year being correct by default or handled by previous direct input/JS set
        } catch (Exception e) {
            logger.debug("Month/Year adjustment failed, attempting to continue with day selection");
        }
    }
}
