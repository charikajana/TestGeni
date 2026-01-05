package automation.browser.locator.core;

import automation.utils.LoggerUtil;
import automation.utils.SelectorUtil;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class LocatorFactory {

    private static final LoggerUtil logger = LoggerUtil.getLogger(LocatorFactory.class);
    
    private final Page page;
    private String lastSelector;
    private String lastTag;

    public LocatorFactory(Page page) {
        this.page = page;
    }
    
    /**
     * Record the details of the last discovered element to the action plan
     */
    public void recordMatch(automation.planner.ActionPlan plan) {
        if (lastSelector != null) {
            plan.setMetadataValue("found_selector", lastSelector);
        }
        if (lastTag != null) {
            plan.setMetadataValue("found_element_type", lastTag);
        }
    }

    public Locator createLocator(ElementCandidate element, double score, String parsedType) {
        return createLocator(element, score, parsedType, null);
    }

    public Locator createLocator(ElementCandidate element, double score, String parsedType, Locator scope) {
         String foundId = element.id;
         String foundTag = element.tag;
         String foundText = element.text;
         String foundFor = element.forAttr;

         logger.debug("Found Winner: <{}> Text:'{}' ID:'{}' (Score: {})", foundTag, foundText, foundId, score);
         
         this.lastTag = foundTag;
         String safeText = foundText.replace("\"", "\\\"").replace("\n", " ").trim();
         if (safeText.length() > 50) safeText = safeText.substring(0, 47) + "...";
         this.lastSelector = (foundId != null && !foundId.isEmpty()) ? foundTag + "#" + foundId : foundTag + ":has-text(\"" + safeText + "\")";

         Locator finalLocator = null;
         
         // Helper to create base locator (either from page or scope)
         if (foundId != null && !foundId.isEmpty() && !SelectorUtil.isDynamic(foundId)) {
             // Use tag + id and filter by text to disambiguate if IDs are reused (common in DemoQA)
             Locator base = (scope != null) ? scope.locator(foundTag + "#" + foundId) : page.locator(foundTag + "#" + foundId);
             if (foundText != null && !foundText.isEmpty() && foundText.length() < 100 && !"progressbar".equals(parsedType) && !"select".equals(foundTag)) {
                 finalLocator = base.filter(new Locator.FilterOptions().setHasText(foundText));
             } else {
                 finalLocator = base;
             }
         } 
         else if ("button".equals(foundTag) || "a".equals(foundTag)) {
             // For buttons and links with dynamic IDs, prioritize Text-based exact matches
             if (foundText != null && !foundText.isEmpty()) {
                 if (scope != null) {
                    finalLocator = scope.getByText(foundText, new Locator.GetByTextOptions().setExact(true));
                 } else {
                    finalLocator = page.getByText(foundText, new Page.GetByTextOptions().setExact(true));
                 }
                 logger.debug("Prioritizing stable text locator for dynamic-id {}: '{}'", foundTag, foundText);
             } else {
                 // Fallback to tag-based if no text
                 finalLocator = (scope != null) ? scope.locator(foundTag) : page.locator(foundTag);
             }
         } 
         else if ("progressbar".equals(parsedType) || "progressbar".equals(element.role)) {
             // Priority for progress bars: Role or Tag, NOT text (which changes constantly)
             finalLocator = (scope != null) ? scope.locator("[role='progressbar']") : page.locator("[role='progressbar']");
         }
         else if (foundText != null && !foundText.isEmpty()) {
             if (foundText.length() > 100) {
                 String prefix = foundText.substring(0, 80);
                 Locator base = (scope != null) ? scope.locator(foundTag) : page.locator(foundTag);
                 finalLocator = base.filter(new Locator.FilterOptions().setHasText(prefix));
                 logger.debug("Using prefix filter for long text element ({} chars)", foundText.length());
             } else {
                 if (score >= 150) {
                     if (scope != null) {
                         finalLocator = scope.getByText(foundText, new Locator.GetByTextOptions().setExact(true));
                     } else {
                         finalLocator = page.getByText(foundText, new Page.GetByTextOptions().setExact(true));
                     }
                 } else {
                     finalLocator = (scope != null) ? scope.getByText(foundText) : page.getByText(foundText);
                 }
             }
         }
         else if (!element.label.isEmpty()) {
             finalLocator = (scope != null) ? scope.getByLabel(element.label) : page.getByLabel(element.label);
         }
         else if (!element.name.isEmpty()) {
             finalLocator = (scope != null) ? scope.locator("[name='" + element.name + "']") : page.locator("[name='" + element.name + "']");
         }
         else if (!element.placeholder.isEmpty()) {
             finalLocator = (scope != null) ? scope.getByPlaceholder(element.placeholder) : page.getByPlaceholder(element.placeholder);
         }
         else {
             finalLocator = (scope != null) ? scope.locator(foundTag) : page.locator(foundTag);
         }

         // Ensure finalLocator is not null before proceeding
         if (finalLocator == null) {
             finalLocator = (scope != null) ? scope.locator(foundTag) : page.locator(foundTag);
         }

         boolean isFill = "input".equals(parsedType);
         boolean isSelect = "select".equals(parsedType);
         boolean isSlider = "slider".equals(parsedType);
         boolean isField = "field".equals(parsedType);

         // Refine for FIELD type (value reading/verification) if we matched a label or wrapper
         // This ensures verification actions find the actual input, not the label
         if (isField && !"input".equals(foundTag) && !"textarea".equals(foundTag) && !"select".equals(foundTag)) {
             // 1. If label with 'for', use that
             if ("label".equals(foundTag) && foundFor != null && !foundFor.isEmpty()) {
                 logger.debug("Refining label match to linked input field #{}", foundFor);
                 return page.locator("#" + foundFor).first();
             }
             // 2. Look for nested input/textarea (common in date pickers)
             Locator nested = finalLocator.locator("input, textarea").first();
             if (nested.count() > 0) {
                 logger.debug("Refining wrapper match to nested input for field verification");
                 return nested.first();
             }
             
             // 3. Look for sibling input (via parent)
             Locator parent = finalLocator.locator("xpath=..");
             Locator sibling = parent.locator("input, textarea").first();
             if (sibling.count() > 0) {
                 logger.debug("Refining match to sibling input for field verification");
                 return sibling.first();
             }

             // 4. Look for cousin input (via grandparent)
             Locator grandParent = finalLocator.locator("xpath=../..");
             Locator cousin = grandParent.locator("input, textarea").first();
             if (cousin.count() > 0) {
                 logger.debug("Refining match to cousin input for field verification");
                 return cousin.first();
             }
             
             // If no input found, continue with the original element (might be a div with contenteditable, etc.)
             logger.debug("No input field found near label/wrapper '{}', using original element for field verification", foundText);
         }

         // Refine for SLIDER actions if we matched a label or wrapper
         if (isSlider && !"input".equals(foundTag)) {
             // 1. If label with 'for', use that
             if ("label".equals(foundTag) && foundFor != null && !foundFor.isEmpty()) {
                 logger.debug("Refining label match to linked slider #{}", foundFor);
                 return page.locator("#" + foundFor).first();
             }
             // 2. Look for nested slider
             Locator nested = finalLocator.locator("input[type='range'], [role='slider']").first();
             if (nested.count() > 0) {
                 logger.debug("Refining wrapper match to nested slider");
                 return nested.first();
             }
             
             // 3. Look for sibling slider (via parent)
             Locator parent = finalLocator.locator("xpath=..");
             Locator sibling = parent.locator("input[type='range'], [role='slider']").first();
             if (sibling.count() > 0) {
                 logger.debug("Refining match to sibling slider");
                 return sibling.first();
             }

             // 4. Look for parent's next sibling's nested slider (common in form layouts)
             Locator parentNextSibling = parent.locator("xpath=following-sibling::*[1]").first();
             if (parentNextSibling.count() > 0) {
                 Locator nestedInSibling = parentNextSibling.locator("input[type='range'], [role='slider']").first();
                 if (nestedInSibling.count() > 0) {
                     logger.debug("Found slider in next sibling of label container, refining to it");
                     return nestedInSibling.first();
                 }
             }
             
             // 5. Look for cousin slider (via grandparent)
             Locator grandParent = finalLocator.locator("xpath=../..");
             Locator cousin = grandParent.locator("input[type='range'], [role='slider']").first();
             if (cousin.count() > 0) {
                 logger.debug("Refining match to cousin slider");
                 return cousin.first();
             }
         }

         // Refine for PROGRESSBAR actions if we matched a container
         if ("progressbar".equals(parsedType)) {
             String role = element.role != null ? element.role.toLowerCase() : "";
             if (!"progressbar".equals(role)) {
                 // Look for nested progress bar
                 Locator nested = finalLocator.locator("[role='progressbar']").first();
                 if (nested.count() > 0) {
                     logger.debug("Refining container match to nested progress bar");
                     return nested.first();
                 }
                 // Look for sibling
                 Locator sibling = finalLocator.locator("xpath=..").locator("[role='progressbar']").first();
                 if (sibling.count() > 0) {
                     logger.debug("Refining match to sibling progress bar");
                     return sibling.first();
                 }
             }
         }
         
         // Refine for FILL actions if we matched a non-input wrapper
         if (isFill && !"input".equals(foundTag) && !"textarea".equals(foundTag)) {
             // 1. If label with 'for', use that
             if ("label".equals(foundTag) && foundFor != null && !foundFor.isEmpty()) {
                 logger.debug("Refining label match to linked input #{}", foundFor);
                 return page.locator("#" + foundFor).first();
             }
             // 2. Look for nested input/textarea
             Locator nested = finalLocator.locator("input, textarea").first();
             if (nested.count() > 0) {
                 logger.debug("Refining wrapper match to nested input");
                 return nested.first();
             }
             
             // 3. Look for sibling input (via parent)
             Locator parent = finalLocator.locator("xpath=..");
             Locator sibling = parent.locator("input, textarea").first();
             if (sibling.count() > 0) {
                 logger.debug("Refining match to sibling input");
                 return sibling.first();
             }

             // 4. Look for cousin input (via grandparent)
             Locator grandParent = finalLocator.locator("xpath=../..");
             Locator cousin = grandParent.locator("input, textarea").first();
             if (cousin.count() > 0) {
                 logger.debug("Refining match to cousin input");
                 return cousin.first();
             }

             // 5. Look for input in next row (common in horizontal form layouts where labels and inputs are in separate rows)
             Locator nextRow = grandParent.locator("xpath=following-sibling::*[1]");
             if (nextRow.count() > 0) {
                 Locator nestedInNextRow = nextRow.locator("input, textarea").first();
                 if (nestedInNextRow.count() > 0) {
                     logger.debug("Found input in next row sibling of label row, refining to it");
                     return nestedInNextRow.first();
                 }
             }
             
             logger.debug("Match found ({}) but not a valid input/textarea. Discarding", foundTag);
             return null;
         }

         // Refine for SELECT actions if we matched a non-select wrapper
         if (isSelect && !"select".equals(foundTag)) {
             // 1. If label with 'for', use that
             if ("label".equals(foundTag) && foundFor != null && !foundFor.isEmpty()) {
                 logger.debug("Refining label match to linked select #{}", foundFor);
                 return page.locator("#" + foundFor).first();
             }
             // 2. Look for nested select
             Locator nested = finalLocator.locator("select").first();
             if (nested.count() > 0) {
                 logger.debug("Refining wrapper match to nested select");
                 return nested.first();
             }
             
             // 3. Look for sibling select
             Locator parent = finalLocator.locator("xpath=..");
             Locator sibling = parent.locator("select").first();
             if (sibling.count() > 0) {
                 logger.debug("Refining match to sibling select");
                 return sibling.first();
             }

             // 4. Before checking cousins, check for custom dropdowns (framework-agnostic)
             logger.debug("No <select> found in immediate vicinity. Checking for custom dropdown patterns");
             
             // 4a. Check for custom dropdown container as direct sibling of the label
             Locator dropdownSibling = finalLocator.locator(
                 "xpath=following-sibling::*[1][" +
                 "contains(@class, 'container') or " +
                 "contains(@class, '-container') or " +
                 "contains(@class, 'select') or " +
                 "contains(@class, 'dropdown') or " +
                 "@role='combobox' or " +
                 "@role='listbox' or " +
                 "@data-select" +
                 "]"
             ).first();
             if (dropdownSibling.count() > 0) {
                 logger.debug("Found custom dropdown container as next sibling of label, refining to it");
                 return dropdownSibling.first();
             }
             
             // 4a-ii. Check parent's next sibling
             Locator parentSibling = finalLocator.locator(
                 "xpath=../following-sibling::*[1][" +
                 "contains(@class, 'container') or " +
                 "contains(@class, '-container') or " +
                 "contains(@class, 'select') or " +
                 "contains(@class, 'dropdown') or " +
                 "@role='combobox' or " +
                 "@role='listbox'" +
                 "]"
             ).first();
             if (parentSibling.count() > 0) {
                 logger.debug("Found custom dropdown container as next sibling of label's parent, refining to it");
                 return parentSibling.first();
             }
             
             // 4b. Check for custom dropdown or native select in parent's next sibling
            Locator parentNextSibling = parent.locator("xpath=following-sibling::*[1]").first();
            if (parentNextSibling.count() > 0) {
                // Check for native select
                Locator nestedSelect = parentNextSibling.locator("select").first();
                if (nestedSelect.count() > 0) {
                    logger.debug("Found native select in parent's next sibling, refining to it");
                    return nestedSelect.first();
                }
                
                // Check for custom dropdowns
                Locator nestedDropdown = parentNextSibling.locator(
                    "div[class*='container'], " +
                    "div[class*='-container'], " +
                    "div[class*='select'], " +
                    "div[class*='dropdown'], " +
                    "[role='combobox'], " +
                    "[role='listbox']"
                ).first();
                if (nestedDropdown.count() > 0) {
                    logger.debug("Found custom dropdown container in parent's next sibling, refining to it");
                    return nestedDropdown.first();
                }
            }
             
             // 4c. Last resort: Check for cousin select
             Locator grandParent = finalLocator.locator("xpath=../..");
             Locator cousin = grandParent.locator("select").first();
             if (cousin.count() > 0) {
                 logger.debug("Refining match to cousin select (fallback)");
                 return cousin.first();
             }
             
             // 5. Return the original wrapper and let SelectAction detect and handle it
             logger.debug("No specific custom dropdown pattern found. Returning wrapper for custom dropdown detection");
         }
 
         return (finalLocator != null) ? finalLocator.first() : null;
    }
}
