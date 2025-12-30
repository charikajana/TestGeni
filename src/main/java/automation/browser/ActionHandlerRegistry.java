package automation.browser;

import automation.browser.actions.*;
import automation.browser.actions.navigation.*;
import automation.browser.actions.click.*;
import automation.browser.actions.input.*;
import automation.browser.actions.select.*;
import automation.browser.actions.verify.*;
import automation.browser.actions.table.*;
import automation.browser.actions.utils.*;
import automation.browser.actions.window.*;
import automation.browser.actions.alert.*;
import automation.browser.actions.frame.*;
import automation.browser.actions.modal.*;
import automation.browser.actions.list.*;
import automation.browser.actions.keyboard.*;
import automation.browser.actions.scroll.ScrollAction;
import automation.browser.actions.slider.*;
import automation.browser.actions.mouse.*;
import automation.browser.actions.progress.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Central registry for all browser action handlers.
 * Organizes action handler registration by category for better maintainability.
 */
public class ActionHandlerRegistry {
    
    private final Map<String, BrowserAction> handlers;
    
    public ActionHandlerRegistry() {
        this.handlers = new HashMap<>();
        registerAllHandlers();
    }
    
    /**
     * Register all action handlers organized by category
     */
    private void registerAllHandlers() {
        registerNavigationActions();
        registerInputActions();
        registerClickActions();
        registerSelectActions();
        registerVerifyActions();
        registerTableActions();
        registerWindowActions();
        registerAlertActions();
        registerFrameActions();
        registerModalActions();
        registerListActions();
        registerKeyboardActions();
        registerScrollActions();
        registerSliderActions();
        registerProgressActions();
        registerMouseActions();
        registerUtilityActions();
        registerBrowserLifecycleActions();
    }
    
    private void registerNavigationActions() {
        handlers.put("navigate", new NavigateAction());
        handlers.put("navigate_app", new NavigateToAppAction());
        handlers.put("refresh_page", new RefreshPageAction());
        handlers.put("browser_back", new BackAction());
        handlers.put("browser_forward", new ForwardAction());
        handlers.put("select_menu", new SelectMenuAction());
    }
    
    private void registerInputActions() {
        handlers.put("fill", new FillAction());
        handlers.put("fill_autocomplete", new FillAutocompleteAction());
        handlers.put("fill_credentials", new FillCredentialsAction());
        handlers.put("fill_form_section", new FillFormSectionAction());
        handlers.put("select_date_relative", new SelectRelativeDateAction());
        handlers.put("set_date", new SetDateAction());
    }
    
    private void registerClickActions() {
        handlers.put("click", new ClickAction());
        handlers.put("double_click", new DoubleClickAction());
        handlers.put("right_click", new RightClickAction());
    }
    
    private void registerSelectActions() {
        handlers.put("select", new SelectAction());
        handlers.put("select_with_criteria", new SelectWithCriteriaAction());
        handlers.put("deselect", new DeselectAction());
        handlers.put("check", new CheckAction());
        handlers.put("uncheck", new UncheckAction());
    }
    
    private void registerVerifyActions() {
        handlers.put("verify", new VerifyTextAction());
        handlers.put("verify_not", new VerifyNotTextAction());
        handlers.put("verify_enabled", new VerifyEnabledAction());
        handlers.put("verify_disabled", new VerifyDisabledAction());
        handlers.put("verify_validation", new VerifyValidationAction());
        handlers.put("verify_value", new VerifyValueAction());
        handlers.put("verify_placeholder", new VerifyPlaceholderAction());
        handlers.put("verify_url", new VerifyURLAction());
        handlers.put("verify_page_title", new VerifyTitleAction());
        
        // Verify checked/selected
        VerifyCheckedAction checkVerify = new VerifyCheckedAction();
        handlers.put("verify_selected", checkVerify);
        handlers.put("verify_not_selected", checkVerify);
        handlers.put("verify_checked", checkVerify);
        handlers.put("verify_unchecked", checkVerify);
    }
    
    private void registerTableActions() {
        handlers.put("row_added_with_value", new VerifyRowAddedAction());
        handlers.put("get_row_values", new GetRowValuesAction());
        handlers.put("click_in_row", new ClickAction());
        handlers.put("click_specific_in_row", new ClickAction());
        handlers.put("verify_row_not_exists", new VerifyRowNotExistsAction());
        handlers.put("select_checkbox_in_row", new SelectCheckboxInRowAction());
        handlers.put("click_in_row_position", new ClickInRowPositionAction());
        handlers.put("direct_row_action", new DirectRowActionHandler());
    }
    
    private void registerWindowActions() {
        WindowManagementAction windowMgmt = new WindowManagementAction();
        handlers.put("switch_to_new_window", windowMgmt);
        handlers.put("switch_to_main_window", windowMgmt);
        handlers.put("close_current_window", windowMgmt);
        handlers.put("close_window", windowMgmt);
        handlers.put("verify_window_count", windowMgmt);
        handlers.put("verify_window_exists", windowMgmt);
        handlers.put("click_and_switch_window", new ClickAndSwitchAction());
    }
    
    private void registerAlertActions() {
        handlers.put("verify_alert", new AcceptAlertAction());
        handlers.put("accept_alert", new AcceptAlertAction());
        handlers.put("dismiss_alert", new DismissAlertAction());
        handlers.put("prompt_alert", new PromptAlertAction());
        handlers.put("dismiss_prompt", new DismissAlertAction());
    }
    
    private void registerFrameActions() {
        SwitchFrameAction frameMgmt = new SwitchFrameAction();
        handlers.put("switch_to_frame", frameMgmt);
        handlers.put("switch_to_main_frame", frameMgmt);
    }
    
    private void registerModalActions() {
        handlers.put("verify_modal_visible", new VerifyModalAction());
        handlers.put("verify_modal_not_visible", new VerifyModalAction());
        handlers.put("close_modal", new CloseModalAction());
    }
    
    private void registerListActions() {
        handlers.put("multiselect_item", new MultiselectAction());
    }
    
    private void registerKeyboardActions() {
        handlers.put("press_key", new PressKeyAction());
    }
    
    private void registerScrollActions() {
        handlers.put("scroll", new ScrollAction());
    }
    
    private void registerSliderActions() {
        handlers.put("set_slider", new SetSliderAction());
    }
    
    private void registerProgressActions() {
        handlers.put("wait_for_progress", new WaitForProgressAction());
    }
    
    private void registerMouseActions() {
        handlers.put("hover", new HoverAction());
        handlers.put("verify_tooltip", new VerifyTooltipAction());
    }
    
    private void registerUtilityActions() {
        // Wait actions
        WaitAction waitAction = new WaitAction();
        handlers.put("wait", waitAction);
        handlers.put("wait_time", waitAction);
        handlers.put("wait_page", waitAction);
        handlers.put("wait_appear", waitAction);
        handlers.put("wait_disappear", waitAction);
        
        // Screenshot
        handlers.put("screenshot", new ScreenshotAction());
        
        // Advanced actions
        handlers.put("toggle_setting", new ToggleSettingAction());
        handlers.put("enter_stored_reference", new EnterStoredReferenceAction());
    }
    
    private void registerBrowserLifecycleActions() {
        handlers.put("close_browser", new CloseBrowserAction());
    }
    
    /**
     * Get the complete map of action handlers
     */
    public Map<String, BrowserAction> getHandlers() {
        return handlers;
    }
    
    /**
     * Get a specific handler by action type
     */
    public BrowserAction getHandler(String actionType) {
        return handlers.get(actionType);
    }
}
