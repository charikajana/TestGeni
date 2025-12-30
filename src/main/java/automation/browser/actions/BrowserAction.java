package automation.browser.actions;

import automation.browser.SmartLocator;
import automation.planner.ActionPlan;
import com.microsoft.playwright.Page;

public interface BrowserAction {
    boolean execute(Page page, SmartLocator locator, ActionPlan plan);
}
