package automation.browser;

import automation.browser.locator.core.DomScanner;
import automation.browser.locator.core.ElementCandidate;
import automation.browser.locator.core.LocatorFactory;
import automation.browser.locator.core.CandidateScorer;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Frame;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
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
        return waitForSmartElement(name, type, null, null);
    }

    public Locator waitForSmartElement(String name, String type, Locator scope) {
        return waitForSmartElement(name, type, scope, null);
    }

    public Locator waitForSmartElement(String name, String type, Locator scope, String frameAnchor) {
        return waitForSmartElement(name, type, scope, frameAnchor, false);
    }

    /**
     * Wait for an element to appear, with optional scope and frame anchor
     */
    public Locator waitForSmartElement(String name, String type, Locator scope, String frameAnchor, boolean includeHidden) {
        long deadline = System.currentTimeMillis() + 30000; 
        int maxRetries = 60; 
        int retryCount = 0;
        long lastLogTime = 0;
        
        while (System.currentTimeMillis() < deadline && retryCount < maxRetries) {
            Locator loc = findSmartElement(name, type, scope, frameAnchor, includeHidden);
            if (loc != null && (includeHidden || loc.isVisible())) {
                return loc;
            }
            
            retryCount++;
            
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastLogTime > 5000) {
                logger.waiting(5);
                logger.debug("Still waiting for element: '{}' (attempt {}/{})", name, retryCount, maxRetries);
                lastLogTime = currentTime;
            }
            
            try {
                Thread.sleep(500); 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        
        logger.failure("TIMEOUT: Element '{}' not found after {} attempts ({}s)", name, retryCount, 30);
        if (scope != null) {
            logger.warning("Searched within row scope - element may not exist in this row");
        }
        if (frameAnchor != null) {
            logger.warning("Searched within iframe '{}' - verify iframe existence", frameAnchor);
        }
        logger.info("Tip: Check if the step syntax is correct and element exists on the page");
        
        return null;
    }

    public Locator findSmartElement(String name, String parsedType) {
        return findSmartElement(name, parsedType, null, null);
    }
    
    public Locator findSmartElement(String name, String parsedType, Locator scope) {
        return findSmartElement(name, parsedType, scope, null);
    }

    public Locator findSmartElement(String name, String parsedType, Locator scope, String frameAnchor) {
        return findSmartElement(name, parsedType, scope, frameAnchor, false);
    }

    public Locator findSmartElement(String name, String parsedType, Locator scope, String frameAnchor, boolean includeHidden) {
        if (name == null) return null;

        // INTELLIGENT TYPE EXTRACTION
        // Extract element type descriptor from user's natural language
        // E.g., "Home link" -> name="Home", type="link"
        //       "Submit button" -> name="Submit", type="button"
        //       "Username input field" -> name="Username", type="input"
        // Note: "box" is NOT a descriptor because it's part of names like "Text Box", "Search Box"
        String cleanName = name.trim();
        String detectedType = parsedType; // Start with passed type
        
        // Check if user provided a type descriptor at the end
        // Note: We exclude "box" since it's commonly part of element names (Text Box, Search Box, etc.)
        if (cleanName.matches("(?i).*\\s+(link|button|icon|checkbox|check box|radio|element|field|input|dropdown|drop down|select|textarea|text area|slider|range|progress bar|progressbar)$")) {
            // Extract the type descriptor
            String[] parts = cleanName.split("\\s+");
            // Handle multi-word hints like "progress bar"
            String typeHint;
            if (cleanName.toLowerCase().endsWith("progress bar")) {
                typeHint = "progressbar";
            } else {
                typeHint = parts[parts.length - 1].toLowerCase();
            }
            
            // Map natural language to element types for DOM scanning
            String mappedType = switch (typeHint) {
                case "link" -> "link";
                case "button" -> "button";
                case "checkbox", "check box" -> "checkbox";
                case "radio" -> "radio";
                case "input", "field" -> "input";
                case "dropdown", "drop down", "select" -> "select";
                case "textarea", "text area" -> "textarea";
                case "slider", "range" -> "slider";
                case "progressbar", "progress bar" -> "progressbar";
                case "icon" -> "element";
                default -> parsedType; // Use original if no match
            };
            
            // Only override if we detected a valid type
            if (mappedType != null && !mappedType.equals(parsedType)) {
                logger.debug("Detected element type hint: '{}' -> type='{}'", typeHint, mappedType);
                detectedType = mappedType;
            }
            
            // Extract clean name without the type descriptor
            cleanName = cleanName.replaceAll("(?i)\\s+(link|button|icon|checkbox|radio|element|field|input|dropdown|select|textarea|slider|range|progress bar|progressbar)$", "").trim();
        }
        
        // Use the clean name and detected type for searching
        final String searchName = cleanName;
        final String searchType = detectedType;
        
        // 1. If frame anchor is provided, narrow search to that frame
        if (frameAnchor != null) {
            Frame frame = findFrame(frameAnchor);
            if (frame != null) {
                logger.debug("Scoping search to iframe: '{}'", frameAnchor);
                return findInContext(searchName, searchType, frame, null, includeHidden);
            } else {
                logger.warning("Target iframe '{}' not found. Searching globally...", frameAnchor);
            }
        }

        // 2. Normal search (Scoped or Page)
        Locator loc = findInContext(searchName, searchType, null, scope, includeHidden);
        if (loc != null) return loc;

        // 3. Automatic Frame Traversal: If not found in main page, search all frames
        if (scope == null) {
            logger.debug("Element '{}' not found in main page. Searching across all iframes...", searchName);
            for (Frame frame : page.frames()) {
                if (frame == page.mainFrame()) continue; // Already searched
                if (frame.isDetached()) continue;
                
                loc = findInContext(searchName, searchType, frame, null, includeHidden);
                if (loc != null) {
                    logger.success("Found element '{}' inside iframe: '{}'", searchName, frame.name().isEmpty() ? frame.url() : frame.name());
                    return loc;
                }
            }
        }

        return null;
    }

    /**
     * Searches for a frame by name, ID, or title
     */
    public Frame findFrame(String frameAnchor) {
        for (Frame frame : page.frames()) {
            if (frame.name().equalsIgnoreCase(frameAnchor)) return frame;
            
            // Check ID or other attributes via evaluation if name doesn't match
            try {
                if (frame.isDetached()) continue;
                String frameId = String.valueOf(frame.evaluate("() => window.frameElement ? window.frameElement.id : ''"));
                if (frameAnchor.equalsIgnoreCase(frameId)) return frame;
                
                if (frame.isDetached()) continue;
                String frameTitle = String.valueOf(frame.evaluate("() => window.frameElement ? window.frameElement.title : ''"));
                if (frameAnchor.equalsIgnoreCase(frameTitle)) return frame;
            } catch (Exception e) {
                // Ignore errors in detached or cross-origin frames
            }
        }
        return null;
    }

    private Locator findInContext(String name, String parsedType, Frame frame, Locator scope, boolean includeHidden) {
        logger.analysis("Analyzing DOM context for target: '{}' (Type: {}){}{}", name, parsedType, (frame != null ? " [Frame]" : (scope != null ? " [Scoped]" : " [Page]")), (includeHidden ? " [Include Hidden]" : ""));

        List<ElementCandidate> elements;
        if (scope != null) {
            elements = docScanner.scan(scope, includeHidden);
        } else if (frame != null) {
            elements = docScanner.scan(frame, includeHidden);
        } else {
            elements = docScanner.scan(page, includeHidden);
        }

        double bestScore = 0.0;
        ElementCandidate bestElement = null;

        for (ElementCandidate el : elements) {
            double score = scorer.score(el, name, parsedType);
            if (score > bestScore) {
                bestScore = score;
                bestElement = el;
            }
        }

        // Debug: Show top candidates if no strong match
        if (bestScore <= 30 && (scope != null || frame != null)) { // Added frame context for debug logging
            logger.debug("Elements found in context:");
            elements.stream()
                .limit(10)
                .forEach(el -> {
                    double s = scorer.score(el, name, parsedType);
                    logger.debug("   - {} {} {} {} -> score={}",
                        el.tag,
                        (el.text.isEmpty() ? "" : " text='" + el.text.substring(0, Math.min(20, el.text.length())) + "'"),
                        (el.title.isEmpty() ? "" : " title='" + el.title + "'"),
                        (el.label.isEmpty() ? "" : " aria-label='" + el.label + "'"),
                        s);
                });
        }

        if (bestScore > 30 && bestElement != null) {
            // Updated locator factory needed to support Frame
            return createLocator(bestElement, bestScore, parsedType, frame, scope);
        }
        
        logger.debug("No strong match found for '{}' in current context", name);
        return null;
    }

    /**
     * Local createLocator wrapper that handles Frame context
     */
    private Locator createLocator(ElementCandidate element, double score, String parsedType, Frame frame, Locator scope) {
        if (frame != null) {
            // Temporarily use a new factory scoped to the frame
            // LocatorFactory frameFactory = new LocatorFactory(page); // We need a way to tell factory to use frame
            // Actually, LocatorFactory currently uses 'page.locator'
            // I should update LocatorFactory to accept a Frame or have it use scope correctly
            
            // For now, let's use a trick: if it's a frame, we return a locator from the frame
            // But LocatorFactory handles the complex logic.
            return locatorFactory.createLocator(element, score, parsedType, (scope != null ? scope : frame.locator(":root").first()));
            // This is a bit hacky. Better to update LocatorFactory.
        }
        
        return locatorFactory.createLocator(element, score, parsedType, scope);
    }
    /**
     * Updates the page instance used by the locator.
     * This is essential when switching between windows/tabs.
     */
    public void setPage(Page newPage) {
        if (this.page != newPage) {
            logger.debug("SmartLocator switching to new page context");
            this.page = newPage;
            // Re-initialize factory with new page
            this.locatorFactory = new LocatorFactory(newPage);
        }
    }
}
