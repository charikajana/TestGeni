package automation.browser.actions.input;

import automation.browser.SmartLocator;
import automation.browser.actions.BrowserAction;
import automation.context.TestContext;
import automation.planner.ActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Enters a previously stored booking reference.
 * Example: "user enters booking reference"
 * 
 * Strategy:
 * 1. Retrieve booking reference from TestContext
 * 2. Find the reference field
 * 3. Fill with stored value
 */
public class EnterStoredReferenceAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(EnterStoredReferenceAction.class);
    
    @Override
    public boolean execute(Page page, SmartLocator smartLocator, ActionPlan plan) {
        logger.info("Enter Stored Reference -> Retrieving from context");
        
        try {
            // Get booking reference from context
            TestContext context = TestContext.getInstance();
            String bookingRef = context.getLastBookingReference();
            
            if (bookingRef == null || bookingRef.isEmpty()) {
                logger.error("No booking reference found in context");
                logger.info("TIP: Ensure a booking reference was stored earlier using 'store the Booking Reference Number'");
                return false;
            }
            
            logger.debug("Retrieved booking reference: {}", bookingRef);
            
            // Find the booking reference field
            Locator refField = findReferenceField(smartLocator);
            
            if (refField == null) {
                logger.error("Could not find booking reference field");
                return false;
            }
            
            // Fill the field
            refField.clear();
            refField.fill(bookingRef);
            
            logger.success("SUCCESS: Entered booking reference: {}", bookingRef);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to enter stored reference: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Find the booking reference input field
     */
    private Locator findReferenceField(SmartLocator smartLocator) {
        // Try common variations
        String[] referenceVariants = {
            "booking reference",
            "reference number",
            "booking number",
            "reference",
            "booking id",
            "confirmation number"
        };
        
        for (String variant : referenceVariants) {
            Locator field = smartLocator.findSmartElement(variant, "input", null, null);
            if (field != null && field.count() > 0) {
                logger.debug("Found reference field using: {}", variant);
                return field;
            }
        }
        
        return null;
    }
}
