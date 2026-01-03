package automation.browser.locator.core;

import com.microsoft.playwright.Frame;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DomScanner {

    public List<ElementCandidate> scan(Page page, boolean includeHidden) {
        return scanInternal(page, null, null, includeHidden);
    }

    public List<ElementCandidate> scan(Page page) {
        return scanInternal(page, null, null, false);
    }
    
    public List<ElementCandidate> scan(Locator scope, boolean includeHidden) {
        return scanInternal(null, null, scope, includeHidden);
    }

    public List<ElementCandidate> scan(Locator scope) {
        return scanInternal(null, null, scope, false);
    }

    public List<ElementCandidate> scan(Frame frame, boolean includeHidden) {
        return scanInternal(null, frame, null, includeHidden);
    }

    public List<ElementCandidate> scan(Frame frame) {
        return scanInternal(null, frame, null, false);
    }

    @SuppressWarnings("unchecked")
    private List<ElementCandidate> scanInternal(Page page, Frame frame, Locator scope, boolean includeHidden) {
        // Robust JS that handles both Page/Frame (root is null/document) and Locator (root is the element)
        String js = "(rootOrHidden, maybeHidden) => {" +
                "  let base, includeHidden;" +
                "  if (typeof rootOrHidden === 'boolean') {" +
                "    base = document;" +
                "    includeHidden = rootOrHidden;" +
                "  } else {" +
                "    base = rootOrHidden || document;" +
                "    includeHidden = maybeHidden || false;" +
                "  }" +
                "  const candidates = Array.from(base.querySelectorAll('button, a, input, textarea, select, [role=\"button\"], label, li, span, div, p, h1, h2, h3, h4, h5, h6, b, strong, i, em'));" +
                        "  const getXPath = (el) => {" +
                "    if (!el) return null;" +
                "    if (el.id) return `//*[@id=\"${el.id}\"]`;" +
                "    const parts = [];" +
                "    while (el && el.nodeType === 1) {" +
                "      let index = 0;" +
                "      for (let sibling = el.previousSibling; sibling; sibling = sibling.previousSibling) {" +
                "        if (sibling.nodeType === 1 && sibling.tagName === el.tagName) index++;" +
                "      }" +
                "      parts.unshift(`${el.tagName.toLowerCase()}[${index + 1}]`);" +
                "      el = el.parentNode;" +
                "    }" +
                "    return parts.length ? `/${parts.join('/')}` : null;" +
                "  };" +
                "  " +
                "  return candidates.map(el => {" +
                "    const rect = el.getBoundingClientRect();" +
                "    const hasDimension = rect.width > 0 && rect.height > 0;" +
                "    const isStyleVisible = window.getComputedStyle(el).visibility !== 'hidden' && window.getComputedStyle(el).display !== 'none';" +
                "    const isVisible = hasDimension && isStyleVisible;" +
                "    if (!includeHidden && !isVisible) return null;" +
                "    " +
                "    let labelText = '';" +
                "    if (['input', 'select', 'textarea'].includes(el.tagName.toLowerCase())) {" +
                "      if (el.id) {" +
                "        const label = document.querySelector('label[for=\"' + el.id + '\"]');" +
                "        if (label) labelText = label.innerText || label.textContent;" +
                "      }" +
                "      if (!labelText) {" +
                "        const parentLabel = el.closest('label');" +
                "        if (parentLabel) labelText = parentLabel.innerText || parentLabel.textContent;" +
                "      }" +
                "      if (!labelText) labelText = el.getAttribute('aria-label') || '';" +
                "    }" +
                "" +
                "    return {" +
                "      tag: el.tagName ? el.tagName.toLowerCase() : ''," +
                "      id: el.id || ''," +
                "      forAttr: el.getAttribute ? el.getAttribute('for') || '' : ''," +
                "      name: el.name || ''," +
                "      text: (el.innerText || el.textContent || '').trim().substring(0, 1000)," +
                "      placeholder: el.placeholder || ''," +
                "      label: (labelText || el.getAttribute('aria-label') || '').trim()," +
                "      title: el.getAttribute ? el.getAttribute('title') || '' : ''," +
                "      type: el.type || ''," +
                "      role: el.getAttribute ? el.getAttribute('role') || '' : ''," +
                "      class: typeof el.className === 'string' ? el.className : (el.getAttribute ? el.getAttribute('class') || '' : '')," +
                "      dataQa: el.getAttribute ? el.getAttribute('data-qa') || '' : ''," +
                "      dataTestId: el.getAttribute ? el.getAttribute('data-testid') || '' : ''," +
                "      alt: el.alt || (el.getAttribute ? el.getAttribute('alt') || '' : '')," +
                "      xpath: getXPath(el)," +
                "      visible: isVisible" +
                "    };" +
                "  }).filter(item => item !== null);" +
                "}";

        Object result = new ArrayList<>();
        try {
            if (scope != null) {
                result = scope.evaluate(js, includeHidden);
            } else if (frame != null) {
                if (!frame.isDetached()) {
                    result = frame.evaluate(js, includeHidden);
                }
            } else {
                result = page.evaluate(js, includeHidden);
            }
        } catch (Exception e) {
            return new ArrayList<>();
        }

        if (result == null) return new ArrayList<>();
        List<Map<String, Object>> rawList = (List<Map<String, Object>>) result;
        List<ElementCandidate> candidates = new ArrayList<>();

        for (Map<String, Object> map : rawList) {
            ElementCandidate c = new ElementCandidate();
            c.tag = String.valueOf(map.getOrDefault("tag", ""));
            c.id = String.valueOf(map.getOrDefault("id", ""));
            c.text = String.valueOf(map.getOrDefault("text", ""));
            c.name = String.valueOf(map.getOrDefault("name", ""));
            c.placeholder = String.valueOf(map.getOrDefault("placeholder", ""));
            c.label = String.valueOf(map.getOrDefault("label", ""));
            c.title = String.valueOf(map.getOrDefault("title", ""));
            c.type = String.valueOf(map.getOrDefault("type", ""));
            c.role = String.valueOf(map.getOrDefault("role", ""));
            c.className = String.valueOf(map.getOrDefault("class", ""));
            c.forAttr = String.valueOf(map.getOrDefault("forAttr", ""));
            c.dataQa = String.valueOf(map.getOrDefault("dataQa", ""));
            c.dataTestId = String.valueOf(map.getOrDefault("dataTestId", ""));
            c.alt = String.valueOf(map.getOrDefault("alt", ""));
            c.xpath = String.valueOf(map.getOrDefault("xpath", ""));
            Object vis = map.get("visible");
            c.visible = vis != null && Boolean.parseBoolean(String.valueOf(vis));
            candidates.add(c);
        }
        return candidates;
    }
}
