package automation.browser;

import automation.browser.locator.core.DomScanner;
import automation.browser.locator.core.ElementCandidate;
import automation.browser.locator.core.LocatorFactory;
import automation.browser.locator.core.CandidateScorer;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Frame;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import automation.planner.ActionPlan;
import java.util.List;

public class SmartLocator {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(SmartLocator.class);
    
    private Page page;
    private final DomScanner docScanner;
    private final CandidateScorer scorer;
    private LocatorFactory locatorFactory;

    public SmartLocator(Page page) {
        this.page = page;
        this.docScanner = new DomScanner();
        this.scorer = new CandidateScorer();
        this.locatorFactory = new LocatorFactory(page);
    }

    public Locator waitForSmartElement(String name, String type) {
        return waitForSmartElement(name, type, null, null, null, false);
    }

    public Locator waitForSmartElement(String name, String type, Locator scope) {
        return waitForSmartElement(name, type, scope, null, null, false);
    }

    public Locator waitForSmartElement(String name, String type, Locator scope, String frameAnchor) {
        return waitForSmartElement(name, type, scope, frameAnchor, null, false);
    }

    public Locator waitForSmartElement(String name, String type, Locator scope, String frameAnchor, String parentAnchor) {
        return waitForSmartElement(name, type, scope, frameAnchor, parentAnchor, false);
    }

    public Locator waitForSmartElement(String name, String type, Locator scope, String frameAnchor, boolean includeHidden) {
        return waitForSmartElement(name, type, scope, frameAnchor, null, includeHidden);
    }

    public Locator waitForSmartElement(String name, String type, Locator scope, String frameAnchor, String parentAnchor, boolean includeHidden) {
        long deadline = System.currentTimeMillis() + 5000; 
        int maxRetries = 20; 
        int retryCount = 0;
        
        while (System.currentTimeMillis() < deadline && retryCount < maxRetries) {
            Locator loc = findSmartElement(name, type, scope, frameAnchor, parentAnchor, includeHidden);
            if (loc != null) {
                if (includeHidden || loc.isVisible()) {
                    return loc;
                } else {
                    if (retryCount > 10) {
                        logger.warning("Element '{}' found but remains invisible. Returning for interaction attempts.", name);
                        return loc;
                    }
                }
            }
            retryCount++;
            try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return null; }
        }
        
        logger.failure("TIMEOUT: Element '{}' not found", name);
        if (parentAnchor != null) logger.warning("Searched within parent: '{}'", parentAnchor);
        return null;
    }

    public Locator findSmartElement(String name, String type) {
        return findSmartElement(name, type, null, null, null, false);
    }

    public Locator findSmartElement(String name, String type, Locator scope, String frameAnchor) {
        return findSmartElement(name, type, scope, frameAnchor, null, false);
    }

    public Locator findSmartElement(String name, String type, Locator scope, String frameAnchor, boolean includeHidden) {
        return findSmartElement(name, type, scope, frameAnchor, null, includeHidden);
    }

    public Locator findSmartElement(String name, String type, Locator scope, String frameAnchor, String parentAnchor, boolean includeHidden) {
        DiscoveryParams params = resolveParams(name, type);
        
        // Handle parent scoping
        if (parentAnchor != null && scope == null) {
            scope = findSmartElement(parentAnchor, "element", null, frameAnchor, null, includeHidden);
        }

        // Search in frame if specified
        if (frameAnchor != null) {
            Frame frame = findFrame(frameAnchor);
            if (frame != null) return findInContext(params.name, params.type, frame, null, includeHidden);
        }

        // Search in current scope
        Locator loc = findInContext(params.name, params.type, null, scope, includeHidden);
        if (loc != null) return loc;

        // Traverse frames if not scoped
        if (scope == null) {
            for (Frame frame : page.frames()) {
                if (frame == page.mainFrame() || frame.isDetached()) continue;
                loc = findInContext(params.name, params.type, frame, null, includeHidden);
                if (loc != null) return loc;
            }
        }
        return null;
    }

    public Locator buildSmartLocator(String name, String type, Locator scope, String frameAnchor, String parentAnchor, boolean includeHidden) {
        DiscoveryParams params = resolveParams(name, type);
        
        if (parentAnchor != null && scope == null) {
            scope = findSmartElement(parentAnchor, "element", null, frameAnchor, null, includeHidden);
        }

        if (frameAnchor != null) {
            Frame frame = findFrame(frameAnchor);
            if (frame != null) return findMatchesInContext(params.name, params.type, frame, null, includeHidden);
        }

        Locator loc = findMatchesInContext(params.name, params.type, null, scope, includeHidden);
        if (loc != null && loc.count() > 0) return loc;

        if (scope == null) {
            for (Frame frame : page.frames()) {
                if (frame == page.mainFrame() || frame.isDetached()) continue;
                loc = findMatchesInContext(params.name, params.type, frame, null, includeHidden);
                if (loc != null && loc.count() > 0) return loc;
            }
        }
        return null;
    }

    // Helper for overloads
    public Locator buildSmartLocator(String name, String type, Locator scope, String frameAnchor, boolean includeHidden) {
        return buildSmartLocator(name, type, scope, frameAnchor, null, includeHidden);
    }

    private DiscoveryParams resolveParams(String name, String type) {
        if (name == null) return new DiscoveryParams("", type);
        String cleanName = name.trim();
        String detectedType = type;

        if (cleanName.matches("(?i).*\\s+(link|button|icon|checkbox|radio|element|field|input|dropdown|select|textarea|slider|range|progressbar)$")) {
            String[] parts = cleanName.split("\\s+");
            String typeHint = parts[parts.length - 1].toLowerCase();
            detectedType = switch (typeHint) {
                case "link" -> "link";
                case "button" -> "button";
                case "checkbox" -> "checkbox";
                case "radio" -> "radio";
                case "input", "field" -> "input";
                case "select", "dropdown" -> "select";
                case "textarea" -> "textarea";
                case "slider", "range" -> "slider";
                case "progressbar" -> "progressbar";
                case "icon" -> "element";
                default -> type;
            };
            cleanName = cleanName.replaceAll("(?i)\\s+(link|button|icon|checkbox|radio|element|field|input|dropdown|select|textarea|slider|range|progressbar)$", "").trim();
        }
        return new DiscoveryParams(cleanName, detectedType);
    }

    private static class DiscoveryParams {
        String name;
        String type;
        DiscoveryParams(String n, String t) { this.name = n; this.type = t; }
    }

    public Frame findFrame(String frameAnchor) {
        for (Frame frame : page.frames()) {
            if (frame.name().equalsIgnoreCase(frameAnchor)) return frame;
            try {
                if (frame.isDetached()) continue;
                String id = (String) frame.evaluate("() => window.frameElement ? window.frameElement.id : ''");
                if (frameAnchor.equalsIgnoreCase(id)) return frame;
                String title = (String) frame.evaluate("() => window.frameElement ? window.frameElement.title : ''");
                if (frameAnchor.equalsIgnoreCase(title)) return frame;
            } catch (Exception e) {}
        }
        return null;
    }

    private Locator findInContext(String name, String type, Frame frame, Locator scope, boolean includeHidden) {
        List<ElementCandidate> elements = (scope != null) ? docScanner.scan(scope, includeHidden) : 
                                          (frame != null) ? docScanner.scan(frame, includeHidden) : 
                                          docScanner.scan(page, includeHidden);
        double bestScore = 0.0;
        ElementCandidate best = null;
        for (ElementCandidate el : elements) {
            double s = scorer.score(el, name, type);
            if (s > bestScore) { bestScore = s; best = el; }
        }
        if (bestScore > 30 && best != null) return createLocator(best, bestScore, type, frame, scope);
        return null;
    }

    private Locator findMatchesInContext(String name, String type, Frame frame, Locator scope, boolean includeHidden) {
        List<ElementCandidate> elements = (scope != null) ? docScanner.scan(scope, includeHidden) : 
                                          (frame != null) ? docScanner.scan(frame, includeHidden) : 
                                          docScanner.scan(page, includeHidden);
        StringBuilder xpath = new StringBuilder();
        int count = 0;
        for (ElementCandidate el : elements) {
            if (scorer.score(el, name, type) > 30) {
                if (count++ > 0) xpath.append("| ");
                xpath.append("(").append(el.xpath).append(") ");
            }
        }
        if (count > 0) {
            String xp = xpath.toString().trim();
            return (scope != null) ? scope.locator("xpath=" + xp) : 
                   (frame != null) ? frame.locator("xpath=" + xp) : 
                   page.locator("xpath=" + xp);
        }
        return null;
    }

    private Locator createLocator(ElementCandidate el, double score, String type, Frame frame, Locator scope) {
        if (frame != null) return locatorFactory.createLocator(el, score, type, (scope != null ? scope : frame.locator(":root").first()));
        return locatorFactory.createLocator(el, score, type, scope);
    }

    public void setPage(Page p) { this.page = p; this.locatorFactory = new LocatorFactory(p); }
    public void recordMatch(ActionPlan p) { if (locatorFactory != null) locatorFactory.recordMatch(p); }
}
