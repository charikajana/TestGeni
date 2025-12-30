package automation.browser.actions.input;

import automation.browser.SmartLocator;
import automation.browser.actions.BrowserAction;
import automation.planner.ActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Selects a date relative to today (e.g., "25 days from today").
 * Example: "selects arrival date 25 days from today"
 * 
 * Strategy:
 * 1. Calculate the target date
 * 2. Find the date picker field
 * 3. Click to open calendar
 * 4. Select the calculated date
 */
public class SelectRelativeDateAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(SelectRelativeDateAction.class);
    
    @Override
    public boolean execute(Page page, SmartLocator smartLocator, ActionPlan plan) {
        String elementName = plan.getElementName();
        String value = plan.getValue();
        
        // Parse the value (should be like "25" from "25 days from today")
        int daysOffset = Integer.parseInt(value);
        
        // Calculate target date
        LocalDate targetDate = LocalDate.now().plusDays(daysOffset);
        
        logger.info("Select Relative Date -> {} = {} days from today ({})", 
            elementName, daysOffset, targetDate);
        
        try {
            // Find the date input field
            Locator dateField = smartLocator.waitForSmartElement(elementName, "input", null, null);
            
            if (dateField == null) {
                logger.error("Could not find date field: {}", elementName);
                return false;
            }
            
            // Try different date input strategies
            boolean success = tryDirectInput(dateField, targetDate, page) ||
                             tryCalendarPicker(page, dateField, targetDate);
            
            if (success) {
                logger.success("SUCCESS: Selected date {} in {}", targetDate, elementName);
                return true;
            } else {
                logger.error("Could not select date using any strategy");
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Failed to select relative date: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Strategy 1: Direct input (for text-based date inputs)
     */
    private boolean tryDirectInput(Locator dateField, LocalDate targetDate, Page page) {
        try {
            // Try common date formats
            List<String> formats = new ArrayList<>();
            formats.add("MM/dd/yyyy");  // US format
            formats.add("dd/MM/yyyy");  // European format
            formats.add("yyyy-MM-dd");  // ISO format
            
            for (String format : formats) {
                try {
                    String dateString = targetDate.format(DateTimeFormatter.ofPattern(format));
                    logger.debug("Trying direct input with format: {} -> {}", format, dateString);
                    
                    dateField.clear();
                    dateField.fill(dateString);
                    dateField.press("Enter");
                    
                    page.waitForTimeout(300);
                    
                    // Check if the value was accepted
                    String currentValue = dateField.inputValue();
                    if (currentValue != null && !currentValue.isEmpty()) {
                        logger.debug("Direct input successful with format: {}", format);
                        return true;
                    }
                } catch (Exception e) {
                    // Try next format
                    continue;
                }
            }
            
        } catch (Exception e) {
            logger.debug("Direct input failed: {}", e.getMessage());
        }
        return false;
    }
    
    /**
     * Strategy 2: Calendar picker (for visual date pickers)
     */
    private boolean tryCalendarPicker(Page page, Locator dateField, LocalDate targetDate) {
        try {
            logger.debug("Attempting calendar picker strategy...");
            
            // Click the date field to open calendar
            dateField.click();
            page.waitForTimeout(500);
            
            // Look for calendar popup
            // Common selectors for calendar widgets
            Locator calendar = page.locator(".calendar,.datepicker,.date-picker,[role='dialog']").first();
            
            if (calendar.count() == 0) {
                logger.debug("No calendar popup found");
                return false;
            }
            
            // Try to find and click the date
            int dayOfMonth = targetDate.getDayOfMonth();
            String monthYear = targetDate.format(DateTimeFormatter.ofPattern("MMMM yyyy"));
            
            logger.debug("Looking for date: {} in {}", dayOfMonth, monthYear);
            
            // Common patterns for date cells
            Locator dateCell = calendar.locator(
                String.format("td,div,span:has-text('%d')", dayOfMonth)
            ).first();
            
            if (dateCell.count() > 0) {
                dateCell.click();
                logger.debug("Clicked date cell for day {}", dayOfMonth);
                return true;
            }
            
        } catch (Exception e) {
            logger.debug("Calendar picker failed: {}", e.getMessage());
        }
        return false;
    }
}
