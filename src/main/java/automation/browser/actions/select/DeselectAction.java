package automation.browser.actions.select;

import automation.browser.actions.BrowserAction;
import automation.browser.SmartLocator;
import automation.planner.ActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Handles deselecting options from multiselect dropdowns.
 * Works with chips/tags from: React-Select, Angular Material, Vue, etc.
 */
public class DeselectAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(DeselectAction.class);
    
    @Override
    public boolean execute(Page page, SmartLocator locator, ActionPlan plan) {
        String dropdownLabel = plan.getElementName();
        String optionToRemove = plan.getValue();
        
        logger.info("Deselecting option: '{}' from dropdown: '{}'", optionToRemove, dropdownLabel);
        
        // Step 1: Find the dropdown wrapper
        Locator dropdownWrapper = locator.waitForSmartElement(dropdownLabel, "select", null, plan.getFrameAnchor());
        
        if (dropdownWrapper == null) {
            logger.failure("Dropdown element not found: {}", dropdownLabel);
            return false;
        }
        
        // Step 2: Determine if it's a native <select> or custom dropdown
        String tagName = (String) dropdownWrapper.evaluate("el => el.tagName.toLowerCase()");
        
        if ("select".equals(tagName)) {
            // For native multiselect, deselect the option
            return handleNativeDeselect(dropdownWrapper, optionToRemove, dropdownLabel);
        } else {
            // For custom dropdowns (all frameworks), click the remove button on the selected chip
            return handleCustomDeselect(page, dropdownWrapper, optionToRemove, dropdownLabel);
        }
    }
    
    /**
     * Deselect option from native HTML <select multiple> element
     */
    private boolean handleNativeDeselect(Locator select, String optionText, String label) {
        try {
            logger.debug("Native <select> multiselect detected");
            
            // For native select, we need to find the option and click it to deselect
            // Or use JavaScript to deselect
            Object result = select.evaluate("(el, optionText) => {" +
                "  for (let opt of el.options) {" +
                "    if (opt.text === optionText || opt.value === optionText) {" +
                "      opt.selected = false;" +
                "      return true;" +
                "    }" +
                "  }" +
                "  return false;" +
                "}", optionText);
            
            if (Boolean.TRUE.equals(result)) {
                logger.success("Deselected '{}' from dropdown '{}'", optionText, label);
                return true;
            } else {
                logger.failure("Option '{}' not found in dropdown '{}'", optionText, label);
                return false;
            }
            
        } catch (Exception e) {
            logger.failure("Could not deselect option: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Deselect option from custom dropdown (React-Select multiselect)
     * Strategy: Find the selected chip/tag with the option text and click its "x" button
     */
    private boolean handleCustomDeselect(Page page, Locator wrapper, String optionText, String label) {
        try {
            logger.debug("Custom multiselect dropdown detected");
            
            // Get the wrapper's ID or class to scope our searches
            String wrapperId = (String) wrapper.evaluate("el => el.id || ''");
            String wrapperClass = (String) wrapper.evaluate("el => el.className || ''");
            
            logger.debug("Wrapper ID: '{}', Class: '{}'", wrapperId, wrapperClass);
            
            // Strategy 1: Multi-select uses chips/tags with remove buttons
            // Generic pattern that works across frameworks:
            // - React-Select: div[class*='multiValue']
            // - Angular Material: mat-chip
            // - Vue/Bootstrap: span.badge, span.tag
            // Strategy: Look for elements with 'multi', 'chip', 'tag', 'badge' in class + option text
            
            // Find the chip/tag containing the option text (framework-agnostic selectors)
            String chipSelector = String.format(
                "div[class*='multiValue']:has-text(\"%s\"), " +
                "mat-chip:has-text(\"%s\"), " +
                "span[class*='chip']:has-text(\"%s\"), " +
                "span[class*='tag']:has-text(\"%s\"), " +
                "span[class*='badge']:has-text(\"%s\")",
                optionText, optionText, optionText, optionText, optionText
            );
            Locator chip = wrapper.locator(chipSelector).first();
            
            if (chip.count() > 0 && chip.isVisible()) {
                logger.debug("Found selected chip for '{}'", optionText);
                
                // Find the remove button within this chip (works across frameworks)
                // Patterns: SVG icons, close buttons, aria-label="remove", or clickable divs
                Locator removeButton = chip.locator(
                    "svg, " +
                    "button, " +
                    "div[role='button'], " +
                    "*[aria-label*='remove'], " +
                    "*[aria-label*='close'], " +
                   "*[aria-label*='delete'], " +
                    "*[class*='Remove'], " +
                    "*[class*='remove'], " +
                    "*[class*='close'], " +
                    "*[class*='clear'], " +
                    "*[class*='delete']"
                ).first();
                
                if (removeButton.count() > 0) {
                    logger.debug("Clicking remove button");
                    removeButton.click(new Locator.ClickOptions().setTimeout(5000));
                    
                    // Wait a longer moment for the chip to be removed and dropdown to stabilize
                    Thread.sleep(800);
                    
                    logger.success("Deselected '{}' from dropdown '{}'", optionText, label);
                    return true;
                } else {
                    // Maybe the entire chip is clickable? Try clicking the chip itself
                    logger.debug("No specific remove button found, trying to click chip");
                    chip.click(new Locator.ClickOptions().setTimeout(5000));
                    Thread.sleep(800);
                    logger.success("Deselected '{}' from dropdown '{}'", optionText, label);
                    return true;
                }
            }
            
            // Strategy 2: Try finding by exact text match with remove button nearby
            Locator chipByText = wrapper.locator(String.format("//*[contains(@class, 'multi') and contains(text(), '%s')]/..", optionText)).first();
            if (chipByText.count() > 0) {
                Locator removeBtn = chipByText.locator("*[contains(@class, 'remove')]").first();
                if (removeBtn.count() > 0) {
                    logger.debug("Found remove button via XPath, clicking");
                    removeBtn.click();
                    Thread.sleep(300);
                    logger.success("Deselected '{}' from dropdown '{}'", optionText, label);
                    return true;
                }
            }
            
            // Strategy 3: Generic approach - find any element with the option text and a remove button
            Locator anyChip = page.locator(String.format("div:has-text(\"%s\")", optionText)).first();
            if (anyChip.count() > 0 && anyChip.isVisible()) {
                // Look for SVG close icon or remove div
                Locator genericRemove = anyChip.locator("svg, div[role='button']").last();
                if (genericRemove.count() > 0) {
                    logger.debug("Found generic remove button, clicking");
                    genericRemove.click();
                    Thread.sleep(300);
                    logger.success("Deselected '{}' from dropdown '{}'", optionText, label);
                    return true;
                }
            }
            
            logger.failure("Could not find remove button for option '{}'", optionText);
            logger.warning("The option might not be selected, or the structure is different than expected");
            return false;
            
        } catch (Exception e) {
            logger.error("Custom dropdown deselect failed: {}", e.getMessage(), e);
            return false;
        }
    }
}
