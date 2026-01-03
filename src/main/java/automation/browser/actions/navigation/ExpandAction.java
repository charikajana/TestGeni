package automation.browser.actions.navigation;

import automation.browser.SmartLocator;
import automation.browser.actions.BrowserAction;
import automation.planner.ActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * A highly generic action handler for expanding collapsible sections.
 * Supports accordions, tree nodes, sidebars, and more.
 */
public class ExpandAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(ExpandAction.class);
    
    @Override
    public boolean execute(Page page, SmartLocator locator, ActionPlan plan) {
        String elementName = plan.getElementName();
        if (elementName == null || elementName.isEmpty()) {
            logger.failure("Expand action requires an element name");
            return false;
        }
        
        logger.info("Expanding: '{}'", elementName);
        Locator allMatches = locator.buildSmartLocator(elementName, "button", null, plan.getFrameAnchor(), true);
        
        if (allMatches == null || allMatches.count() == 0) {
            logger.failure("Could not find any element matching '{}' to expand", elementName);
            return false;
        }
        
        String filterScript = 
            "(elements, targetName) => {" +
            "  const norm = s => (s || '').trim().toLowerCase();" +
            "  const t = norm(targetName);" +
            "  return elements.map((el, index) => {" +
            "    let score = 0;" +
            "    const text = norm(el.innerText || el.textContent);" +
            "    const name = norm(el.getAttribute('name') || el.getAttribute('aria-label') || el.id);" +
            "    if (text === t || name === t) score += 1000;" +
            "    else if (text.startsWith(t)) score += 500;" +
            "    else if (text.includes(t)) score += 200;" +
            "    if (el.tagName === 'SUMMARY' || el.tagName === 'DETAILS') score += 500;" +
            "    if (el.hasAttribute('aria-expanded')) score += 400;" +
            "    if (el.hasAttribute('aria-controls')) score += 300;" +
            "    if (el.closest('label')?.htmlFor) score += 300;" +
            "    if (el.querySelector('i, svg, .icon, .arrow, .chevron, .toggle, .plus, .expand')) score += 150;" +
            "    if (['button', 'link', 'menuitem', 'tab', 'treeitem'].includes(el.getAttribute('role'))) score += 100;" +
            "    if (['div', 'section', 'nav', 'li'].includes(el.tagName.toLowerCase()) && text !== t) score -= 400;" +
            "    const rect = el.getBoundingClientRect();" +
            "    if (rect.width > 0 && rect.height > 0) score += 50;" +
            "    return { index, score };" +
            "  }).filter(x => x.score > 0).sort((a,b) => b.score - a.score).map(x => x.index);" +
            "}";
            
        Object sortedIndicesObj = allMatches.evaluateAll(filterScript, elementName);
        java.util.List<Integer> sortedIndices = new java.util.ArrayList<>();
        if (sortedIndicesObj instanceof java.util.List) {
             for(Object item : (java.util.List<?>) sortedIndicesObj) sortedIndices.add(((Number)item).intValue());
        }

        int limit = Math.min(sortedIndices.size(), 5);
        long globalDeadline = System.currentTimeMillis() + 35000;
        int candidatesTested = 0;

        for (int i = 0; i < limit; i++) {
            if (System.currentTimeMillis() > globalDeadline) break;
            
            Locator element = allMatches.nth(sortedIndices.get(i));
            try {
                if (!element.isVisible()) continue;
                candidatesTested++;
                
                String tag = element.evaluate("el => el.tagName").toString();
                String textSnippet = element.innerText().split("\n")[0].trim();
                
                // Get pre-interaction score for logging
                Object rawScore = element.evaluate(
                    "(el, t) => {" +
                    "  const norm = s => (s || '').trim().toLowerCase();" +
                    "  const text = norm(el.innerText || el.textContent);" +
                    "  const name = norm(el.getAttribute('name') || el.getAttribute('aria-label') || el.id);" +
                    "  const tNorm = norm(t);" +
                    "  let s = 0;" +
                    "  if (text === tNorm || name === tNorm) s += 1000;" +
                    "  else if (text.includes(tNorm)) s += 200;" +
                    "  return s;" +
                    "}", elementName);
                
                logger.debug("Testing candidate #{} [{}]: '{}' (Text-Match Score: {})", candidatesTested, tag, textSnippet, rawScore);

                Boolean result = (Boolean) element.evaluate(getSmartScript(), elementName);
                if (Boolean.TRUE.equals(result)) {
                    logger.success("Successfully expanded '{}' (Confirmed via candidate #{})", elementName, candidatesTested);
                    locator.recordMatch(plan);
                    return true;
                }
            } catch (Exception e) {
                logger.debug("Candidate #{} failed: {}", candidatesTested, e.getMessage());
            }
        }
        
        logger.failure("Universal expansion failed for '{}'", elementName);
        return false;
    }

    private String getSmartScript() {
        return "(el, targetName) => {" +
               "  const norm = s => (s || '').trim().toLowerCase();" +
               "  const t = norm(targetName);" +
               "  const isVisible = e => {" +
               "    if (!e) return false;" +
               "    const s = window.getComputedStyle(e);" +
               "    return s.display !== 'none' && s.visibility !== 'hidden' && e.offsetWidth > 0;" +
               "  };" +
               "  \n" +
               "  const checkState = () => {" +
               "    // Priority: aria-expanded\n" +
               "    if (el.getAttribute('aria-expanded') === 'true') return true;" +
               "    if (el.tagName === 'DETAILS' && el.open) return true;" +
               "    \n" +
               "    // Priority: Sub-content visibility (ARIA controls)\n" +
               "    const controls = el.getAttribute('aria-controls') || el.getAttribute('aria-owns');" +
               "    if (controls && isVisible(document.getElementById(controls))) return true;" +
               "    \n" +
               "    // Priority: Immediate next sibling visibility (Deque/Common pattern)\n" +
               "    const next = el.nextElementSibling;" +
               "    if (next && (next.tagName === 'CODE' || next.tagName === 'PRE' || next.classList.contains('compblock') || next.classList.contains('content'))) {" +
               "      if (isVisible(next)) return true;" +
               "    }" +
               "    \n" +
               "    // Priority: Nested group visibility\n" +
               "    const group = el.querySelector('ul, ol, [role=\"group\"], [role=\"menu\"], .submenu, .collapse.show');" +
               "    if (group && isVisible(group)) return true;" +
               "    \n" +
               "    // Priority: Parent container checkbox/radio hack\n" +
               "    const container = el.closest('li') || el.closest('section') || el.parentElement;" +
               "    const cb = container?.querySelector('input[type=\"checkbox\"], input[type=\"radio\"]');" +
               "    if (cb && cb.checked) return true;" +
               "    \n" +
               "    // Fallback: Class-based open indicators (riskier)\n" +
               "    const openClasses = ['expanded', 'open', 'is-open', 'active', 'show'];" +
               "    const myClasses = [...el.classList, ...(el.parentElement?.classList || [])];" +
               "    if (myClasses.some(c => openClasses.some(o => c.toLowerCase().includes(o)))) {" +
               "       // Only trust class-based if we have aria-expanded check above or exact text match later\n" +
               "       return true;" +
               "    }\n" +
               "    return false;" +
               "  };" +
               "  \n" +
               "  const text = norm(el.innerText || el.textContent);" +
               "  const name = norm(el.getAttribute('name') || el.getAttribute('aria-label') || el.id);" +
               "  const isExactMatch = (text === t || name === t || text.includes(t));" +
               "  \n" +
               "  // If already expanded AND matches the target text, return success\n" +
               "  if (checkState() && isExactMatch) return true;" +
               "  \n" +
               "  // If it doesn't match the target text and it's just some random button, skip it to avoid false positive\n" +
               "  if (!isExactMatch && !el.hasAttribute('aria-expanded')) return false;" +
               "  \n" +
               "  // Interaction\n" +
               "  try {\n" +
               "    const icon = el.querySelector('i, svg, [class*=\"icon\"], [class*=\"arrow\"], [class*=\"plus\"]');\n" +
               "    if (icon && isVisible(icon)) icon.click();\n" +
               "    else el.click();\n" +
               "    \n" +
               "    // Label support\n" +
               "    if (el.tagName === 'LABEL' && el.htmlFor) {\n" +
               "      const cb = document.getElementById(el.htmlFor);\n" +
               "      if (cb) cb.checked = true;\n" +
               "    }\n" +
               "  } catch(e) {}\n" +
               "  \n" +
               "  return new Promise(resolve => {\n" +
               "    let start = Date.now();\n" +
               "    const poll = () => {\n" +
               "      if (checkState()) return resolve(true);\n" +
               "      if (Date.now() - start > 2500) return resolve(false);\n" +
               "      requestAnimationFrame(poll);\n" +
               "    };\n" +
               "    poll();\n" +
               "  });" +
               "}";
    }
}
