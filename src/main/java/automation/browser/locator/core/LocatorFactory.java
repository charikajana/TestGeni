package automation.browser.locator.core;

import automation.utils.LoggerUtil;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class LocatorFactory {

    private static final LoggerUtil logger = LoggerUtil.getLogger(LocatorFactory.class);
    
    private final Page page;

    public LocatorFactory(Page page) {
        this.page = page;
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
         
         Locator finalLocator = null;
         
         // Helper to create base locator (either from page or scope)
         // Note: We cannot use ID if scoped, unless we assume ID is unique globally (which is true by spec but not always in reality).
         // Safer to use scope.locator("#id") if strict.
         
         if (foundId != null && !foundId.isEmpty() && !isDynamicId(foundId)) {
             // Use tag + id and filter by text to disambiguate if IDs are reused (common in DemoQA)
             Locator base = (scope != null) ? scope.locator(foundTag + "#" + foundId) : page.locator(foundTag + "#" + foundId);
             if (foundText != null && !foundText.isEmpty() && foundText.length() < 100 && !"progressbar".equals(parsedType)) {
                 finalLocator = base.filter(new Locator.FilterOptions().setHasText(foundText)).first();
             } else {
                 finalLocator = base.first();
             }
         } 
         else if ("button".equals(foundTag) || "a".equals(foundTag)) {
             // For buttons and links with dynamic IDs, prioritize Text-based exact matches
             if (foundText != null && !foundText.isEmpty()) {
                 if (scope != null) {
                    finalLocator = scope.getByText(foundText, new Locator.GetByTextOptions().setExact(true)).first();
                 } else {
                    finalLocator = page.getByText(foundText, new Page.GetByTextOptions().setExact(true)).first();
                 }
                 logger.debug("Prioritizing stable text locator for dynamic-id {}: '{}'", foundTag, foundText);
             }
         }
         else if ("progressbar".equals(parsedType) || "progressbar".equals(element.role)) {
             // Priority for progress bars: Role or Tag, NOT text (which changes constantly)
             finalLocator = (scope != null) ? scope.locator("[role='progressbar']").first() : page.locator("[role='progressbar']").first();
         }
         else if (foundText != null && !foundText.isEmpty() && foundText.length() < 100) {
             if (score >= 150) {
                 if (scope != null) {
                     finalLocator = scope.getByText(foundText, new Locator.GetByTextOptions().setExact(true)).first();
                 } else {
                     finalLocator = page.getByText(foundText, new Page.GetByTextOptions().setExact(true)).first();
                 }
             } else {
                 finalLocator = (scope != null) ? scope.getByText(foundText).first() : page.getByText(foundText).first();
             }
         }
         else if (!element.label.isEmpty()) {
             finalLocator = (scope != null) ? scope.getByLabel(element.label).first() : page.getByLabel(element.label).first();
         }
         else if (!element.name.isEmpty()) {
             finalLocator = (scope != null) ? scope.locator("[name='" + element.name + "']").first() : page.locator("[name='" + element.name + "']").first();
         }
         else if (!element.placeholder.isEmpty()) {
             finalLocator = (scope != null) ? scope.getByPlaceholder(element.placeholder).first() : page.getByPlaceholder(element.placeholder).first();
         }
         else {
             finalLocator = (scope != null) 
                 ? scope.locator(foundTag).filter(new Locator.FilterOptions().setHasText(foundText)).first()
                 : page.locator(foundTag).filter(new Locator.FilterOptions().setHasText(foundText)).first();
         }

         boolean isFill = "input".equals(parsedType);
         boolean isSelect = "select".equals(parsedType);
         boolean isSlider = "slider".equals(parsedType);

         // Refine for SLIDER actions if we matched a label or wrapper
         if (isSlider && !"input".equals(foundTag)) {
             // 1. If label with 'for', use that
             if ("label".equals(foundTag) && foundFor != null && !foundFor.isEmpty()) {
                 logger.debug("Refining label match to linked slider #{}", foundFor);
                 return page.locator("#" + foundFor);
             }
             // 2. Look for nested slider
             Locator nested = finalLocator.locator("input[type='range'], [role='slider']").first();
             if (nested.count() > 0) {
                 logger.debug("Refining wrapper match to nested slider");
                 return nested;
             }
             
             // 3. Look for sibling slider (via parent)
             Locator parent = finalLocator.locator("xpath=..");
             Locator sibling = parent.locator("input[type='range'], [role='slider']").first();
             if (sibling.count() > 0) {
                 logger.debug("Refining match to sibling slider");
                 return sibling;
             }

             // 4. Look for parent's next sibling's nested slider (common in form layouts)
             Locator parentNextSibling = parent.locator("xpath=following-sibling::*[1]").first();
             if (parentNextSibling.count() > 0) {
                 Locator nestedInSibling = parentNextSibling.locator("input[type='range'], [role='slider']").first();
                 if (nestedInSibling.count() > 0) {
                     logger.debug("Found slider in next sibling of label container, refining to it");
                     return nestedInSibling;
                 }
             }
             
             // 5. Look for cousin slider (via grandparent)
             Locator grandParent = finalLocator.locator("xpath=../..");
             Locator cousin = grandParent.locator("input[type='range'], [role='slider']").first();
             if (cousin.count() > 0) {
                 logger.debug("Refining match to cousin slider");
                 return cousin;
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
                     return nested;
                 }
                 // Look for sibling
                 Locator sibling = finalLocator.locator("xpath=..").locator("[role='progressbar']").first();
                 if (sibling.count() > 0) {
                     logger.debug("Refining match to sibling progress bar");
                     return sibling;
                 }
             }
         }
         
         // Refine for FILL actions if we matched a non-input wrapper
         if (isFill && !"input".equals(foundTag) && !"textarea".equals(foundTag)) {
             // 1. If label with 'for', use that
             if ("label".equals(foundTag) && foundFor != null && !foundFor.isEmpty()) {
                 logger.debug("Refining label match to linked input #{}", foundFor);
                 return page.locator("#" + foundFor);
             }
             // 2. Look for nested input/textarea
             Locator nested = finalLocator.locator("input, textarea").first();
             if (nested.count() > 0) {
                 logger.debug("Refining wrapper match to nested input");
                 return nested;
             }
             
             // 3. Look for sibling input (via parent)
             Locator parent = finalLocator.locator("xpath=..");
             Locator sibling = parent.locator("input, textarea").first();
             if (sibling.count() > 0) {
                 logger.debug("Refining match to sibling input");
                 return sibling;
             }

             // 4. Look for cousin input (via grandparent)
             Locator grandParent = finalLocator.locator("xpath=../..");
             Locator cousin = grandParent.locator("input, textarea").first();
             if (cousin.count() > 0) {
                 logger.debug("Refining match to cousin input");
                 return cousin;
             }
             
             logger.debug("Match found ({}) but not a valid input/textarea. Discarding", foundTag);
             return null;
         }

         // Refine for SELECT actions if we matched a non-select wrapper
         if (isSelect && !"select".equals(foundTag)) {
             // 1. If label with 'for', use that
             if ("label".equals(foundTag) && foundFor != null && !foundFor.isEmpty()) {
                 logger.debug("Refining label match to linked select #{}", foundFor);
                 return page.locator("#" + foundFor);
             }
             // 2. Look for nested select
             Locator nested = finalLocator.locator("select").first();
             if (nested.count() > 0) {
                 logger.debug("Refining wrapper match to nested select");
                 return nested;
             }
             
             // 3. Look for sibling select
             Locator parent = finalLocator.locator("xpath=..");
             Locator sibling = parent.locator("select").first();
             if (sibling.count() > 0) {
                 logger.debug("Refining match to sibling select");
                 return sibling;
             }

             // 4. Before checking cousins, check for custom dropdowns (framework-agnostic)
             // Works with: React-Select, Angular Material, Vue-Select, Bootstrap, etc.
             logger.debug("No <select> found in immediate vicinity. Checking for custom dropdown patterns");
             
             // 4a. Check for custom dropdown container as direct sibling of the label
             // Generic patterns that work across frameworks:
             // - Contains 'container', 'select', 'dropdown'
             // - Has role='combobox' or role='listbox'
             // - Has data-* attributes for selects
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
                 return dropdownSibling;
             }
             
             // 4a-ii. Check parent's next sibling (handles nested labels like <p><b>Text</b></p>)
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
                 return parentSibling;
             }
             
             // 4b. Check for custom dropdown or native select in parent's next sibling (nested layouts)
            // Pattern: <div><p><b>Label</b></p></div> <div><select>...</select></div>
            Locator parentNextSibling = parent.locator("xpath=following-sibling::*[1]").first();
            if (parentNextSibling.count() > 0) {
                // Check for native select
                Locator nestedSelect = parentNextSibling.locator("select").first();
                if (nestedSelect.count() > 0) {
                    logger.debug("Found native select in parent's next sibling, refining to it");
                    return nestedSelect;
                }
                
                // Check for custom dropdowns (framework-agnostic)
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
                    return nestedDropdown;
                }
            }
             
             // 4c. Last resort: Check for cousin select (but this might match unrelated elements)
             Locator grandParent = finalLocator.locator("xpath=../..");
             Locator cousin = grandParent.locator("select").first();
             if (cousin.count() > 0) {
                 logger.debug("Refining match to cousin select (fallback)");
                 return cousin;
             }
             
             // 5. Return the original wrapper and let SelectAction detect and handle it
             logger.debug("No specific custom dropdown pattern found. Returning wrapper for custom dropdown detection");
         }

         return finalLocator;
    }
    private boolean isDynamicId(String id) {
        if (id == null || id.isEmpty()) return false;
        // Detect DemoQA pattern: Short (5-8 chars) and contains mixed letters and numbers
        // e.g. "50r6O", "Z2p7q"
        if (id.length() >= 5 && id.length() <= 10) {
            boolean hasDigit = id.matches(".*\\d+.*");
            boolean hasLetter = id.matches(".*[a-zA-Z]+.*");
            if (hasDigit && hasLetter) return true;
        }
        return false;
    }
}
