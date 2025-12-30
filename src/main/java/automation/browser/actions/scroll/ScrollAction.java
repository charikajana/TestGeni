package automation.browser.actions.scroll;

import automation.browser.actions.BrowserAction;
import automation.browser.SmartLocator;
import automation.planner.ActionPlan;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Locator;
import automation.utils.LoggerUtil;

public class ScrollAction implements BrowserAction {

    private static final LoggerUtil logger = LoggerUtil.getLogger(ScrollAction.class);

    @Override
    public boolean execute(Page page, SmartLocator smartLocator, ActionPlan plan) {
        String target = plan.getElementName(); // Element to scroll TO or scroll WITHIN
        String value = plan.getValue();        // Amount or Direction (e.g. "bottom", "500", "down")
        
        // Metadata might contain explicit direction or type if set by pattern
        String direction = value != null ? value.toLowerCase() : "";
        
        try {
            // Case 1: Scroll Page to position (top/bottom/middle)
            // Pattern: "scroll to bottom" (target=null, value="bottom")
            if (target == null || target.equalsIgnoreCase("page") || target.equalsIgnoreCase("screen")) {
                return scrollPage(page, direction);
            }
            
            // Case 2: Scroll specific element (to view it) -> "Scroll to 'Contact Us'"
            // In this case, target is the element name. Value might be null.
            if (value == null || value.isEmpty()) {
                logger.info("Scrolling element '{}' into view", target);
                // Use findSmartElement instead of findElement
                Locator element = smartLocator.findSmartElement(target, null); 
                if (element != null) {
                    element.scrollIntoViewIfNeeded();
                    return true;
                }
                return false;
            }

            // Case 3: Scroll WITHIN an element or BY an amount
            // Pattern: "Scroll down 500 pixels" -> target="down" (misparsed?) or needs specific pattern mapping
            // Let's rely on specific patterns. 
            // If pattern is "Scroll down 500 px", we might map: validation="scroll_by", value="500", direction="down"
            
            // Handling mixed cases based on inputs:

            // Detect if "target" is actually a direction ("down", "up") implying page scroll by amount
            if (isDirection(target) && isNumeric(value)) {
                return scrollPageByAmount(page, target, Integer.parseInt(value));
            }

            // Scroll WITHIN target element: "Scroll (to) bottom of 'List'"
            // target='List', value='bottom'
            logger.info("Scrolling within element '{}' to '{}'", target, value);
            // Use findSmartElement
            Locator container = smartLocator.findSmartElement(target, null);
            if (container != null) {
                return scrollElement(container, value);
            }
            
        } catch (Exception e) {
            logger.error("Scroll failed: {}", e.getMessage());
        }
        return false;
    }

    private boolean scrollPage(Page page, String position) {
        logger.info("Scrolling page to {}", position);
        switch (position) {
            case "bottom":
            case "end":
                page.evaluate("window.scrollTo(0, document.body.scrollHeight)");
                return true;
            case "top":
            case "start":
            case "home":
                page.evaluate("window.scrollTo(0, 0)");
                return true;
            case "middle":
            case "center":
                page.evaluate("window.scrollTo(0, document.body.scrollHeight / 2)");
                return true;
            default:
                logger.warn("Unknown scroll position: {}", position);
                return false;
        }
    }

    private boolean scrollPageByAmount(Page page, String direction, int pixels) {
        logger.info("Scrolling page {} by {} pixels", direction, pixels);
        int x = 0;
        int y = 0;
        switch (direction.toLowerCase()) {
            case "down": y = pixels; break;
            case "up": y = -pixels; break;
            case "right": x = pixels; break;
            case "left": x = -pixels; break;
        }
        page.evaluate(String.format("window.scrollBy(%d, %d)", x, y));
        return true;
    }

    private boolean scrollElement(Locator element, String positionOrDirection) {
        // Handle "bottom", "top", "left", "right" for container scrolling
        switch (positionOrDirection.toLowerCase()) {
            case "bottom":
            case "down":
                element.evaluate("el => el.scrollTop = el.scrollHeight");
                return true;
            case "top":
            case "up":
                element.evaluate("el => el.scrollTop = 0");
                return true;
            case "right":
                element.evaluate("el => el.scrollLeft = el.scrollWidth");
                return true;
            case "left":
                element.evaluate("el => el.scrollLeft = 0");
                return true;
                
            // Could add specific amounts for element if needed, but keeping simple for now
            default:
                 logger.warn("Unknown element scroll position: {}", positionOrDirection);
                 return false;
        }
    }

    private boolean isDirection(String s) {
        if (s == null) return false;
        String l = s.toLowerCase();
        return l.equals("up") || l.equals("down") || l.equals("left") || l.equals("right");
    }

    private boolean isNumeric(String s) {
        return s != null && s.matches("\\d+");
    }
}
