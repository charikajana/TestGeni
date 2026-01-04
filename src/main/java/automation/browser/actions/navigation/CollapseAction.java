package automation.browser.actions.navigation;

import automation.browser.SmartLocator;
import automation.browser.actions.BrowserAction;
import automation.planner.ActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * A highly generic action handler for collapsing expanded sections.
 * Supports accordions, tree nodes, sidebars, and more.
 */
public class CollapseAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(CollapseAction.class);
    
    @Override
    public boolean execute(Page page, SmartLocator locator, ActionPlan plan) {
        String elementName = plan.getElementName();
        if (elementName == null || elementName.isEmpty()) {
            logger.failure("Collapse action requires an element name");
            return false;
        }
        
        logger.info("Collapsing: '{}'", elementName);
        Locator allMatches = locator.buildSmartLocator(elementName, "button", null, plan.getFrameAnchor(), plan.getParentAnchor(), true);
        
        if (allMatches == null || allMatches.count() == 0) {
            logger.failure("Could not find any element matching '{}' to collapse", elementName);
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
            "    if (el.closest('label')?.htmlFor) score += 300;" + // Critical for Checkbox Hack
            "    if (el.querySelector('i, svg, .icon, .arrow, .chevron, .toggle, .minus, .collapse')) score += 150;" +
            "    if (['div', 'section', 'nav', 'li'].includes(el.tagName.toLowerCase()) && text !== t) score -= 400;" +
            "    const rect = el.getBoundingClientRect();" +
            "    if (rect.width > 0 && rect.height > 0) score += 50;" +
            "    return { index, score };" +
            "  }).filter(x => x.score > 0).sort((a,b) => b.score - a.score).map(x => x.index);" +
            "}";
            
        Object sortedIndicesObj = allMatches.evaluateAll(filterScript, elementName);
        java.util.List<Integer> sortedIndices = new java.util.ArrayList<>();
        if (sortedIndicesObj instanceof java.util.List) {
             for(Object item : (java.util.List<?>) sortedIndicesObj) {
                 if (item instanceof Number) sortedIndices.add(((Number)item).intValue());
             }
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
                logger.debug("Testing candidate #{} [{}]: '{}'", candidatesTested, tag, textSnippet);

                Boolean result = (Boolean) element.evaluate(getSmartScript(), elementName);
                if (Boolean.TRUE.equals(result)) {
                    logger.success("Successfully collapsed '{}'", elementName);
                    locator.recordMatch(plan);
                    return true;
                }
            } catch (Exception e) {
                logger.debug("Candidate #{} failed: {}", candidatesTested, e.getMessage());
            }
        }
        
        logger.failure("Universal collapse failed for '{}'", elementName);
        return false;
    }

    private String getSmartScript() {
        return "(el, targetName) => {" +
               "  const isVisible = e => {" +
               "    if (!e) return false;" +
               "    const s = window.getComputedStyle(e);" +
               "    return s.display !== 'none' && s.visibility !== 'hidden' && e.offsetWidth > 0;" +
               "  };" +
               "  const checkState = () => {" +
               "    // Priority: Explicit aria states\n" +
               "    if (el.getAttribute('aria-expanded') === 'true') return true;" +
               "    if (el.tagName === 'DETAILS' && el.open) return true;" +
               "    \n" +
               "    // Priority: Checkbox hack (EvilTester/Menu pattern)\n" +
               "    const container = el.closest('li') || el.closest('section') || el.parentElement;" +
               "    const cb = container?.querySelector('input[type=\"checkbox\"], input[type=\"radio\"]');" +
               "    if (cb && cb.checked) return true;" +
               "    \n" +
               "    // Priority: Child/Sibling visibility\n" +
               "    const controls = el.getAttribute('aria-controls') || el.getAttribute('aria-owns');" +
               "    if (controls && isVisible(document.getElementById(controls))) return true;" +
               "    \n" +
               "    const next = el.nextElementSibling || el.parentElement?.nextElementSibling;" +
               "    if (next && (next.tagName === 'UL' || next.tagName === 'DIV' || next.tagName === 'CODE' || next.classList.contains('foldable')) && isVisible(next)) return true;" +
               "    \n" +
               "    // Priority: Internal group\n" +
               "    const group = el.querySelector('ul, [role=\"group\"], .submenu, .collapse.show');" +
               "    if (group && isVisible(group)) return true;" +
               "    \n" +
               "    // Fallback: Class names (EXCLUDING 'active' as it causes false positives for Page links)\n" +
               "    const openClasses = ['expanded', 'open', 'is-open', 'collapse.show', 'submenu-visible'];" +
               "    const myClasses = [...el.classList, ...(el.parentElement?.classList || [])];" +
               "    if (myClasses.some(cl => openClasses.some(o => cl.toLowerCase().includes(o)))) return true;" +
               "    \n" +
               "    return false;" +
               "  };" +
               "  \n" +
               "  if (!checkState()) return true; // Already collapsed\n" +
               "  \n" +
               "  try {" +
               "    // Detect checkbox hack and find correct toggle target\n" +
               "    const container = el.closest('li') || el.closest('section') || el.parentElement;" +
               "    const cb = container?.querySelector('input[type=\"checkbox\"], input[type=\"radio\"]');" +
               "    const labelForCb = cb && cb.id ? document.querySelector(`label[for=\"${cb.id}\"]`) : null;" +
               "    const icon = el.querySelector('i, svg, [class*=\"icon\"], [class*=\"arrow\"], [class*=\"chevron\"], [class*=\"toggle\"]');" +
               "    \n" +
               "    if (icon && isVisible(icon)) {" +
               "       icon.click();" +
               "    } else if (labelForCb && isVisible(labelForCb)) {" +
               "       labelForCb.click();" +
               "    } else if (el.tagName === 'A' && el.parentElement.tagName === 'LABEL') {" +
               "       // Focus on side menu chevrons/labels rather than potentially navigating links\n" +
               "       el.parentElement.click();" +
               "    } else {" +
               "       el.click();" +
               "    }" +
               "  } catch(e) {}" +
               "  \n" +
               "  return new Promise(resolve => {" +
               "    let start = Date.now();" +
               "    const poll = () => {" +
               "      if (!checkState()) return resolve(true);" +
               "      if (Date.now() - start > 3000) return resolve(false);" +
               "      requestAnimationFrame(poll);" +
               "    };\n" +
               "    poll();" +
               "  });" +
               "}";
    }
}
