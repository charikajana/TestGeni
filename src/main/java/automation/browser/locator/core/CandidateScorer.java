package automation.browser.locator.core;

import automation.utils.FuzzyMatch;

public class CandidateScorer {

    public double score(ElementCandidate el, String targetName, String parsedType) {
        String name = targetName.trim();
        String lowerName = name.toLowerCase();
        // pre-calculate clean name? Or do it here. 
        // Optimization: Clean name could be passed in, but doing it here keeps interface simple.
        // Enhanced clean name - remove common suffixes like "icon", "button", etc.
        String cleanName = lowerName;
        boolean changed;
        do {
            changed = false;
            String before = cleanName;
            cleanName = cleanName.replaceAll("\\s+(button|btn|link|input|field|tab|icon|radio|checkbox|dropdown|select|box|menu|card|item|element|option|header|title|label|slider|range)$", "").trim();
            if (!before.equals(cleanName)) changed = true;
        } while (changed);

        boolean isFill = "input".equals(parsedType);
        boolean isCheck = "check".equals(parsedType) || "checkbox".equals(parsedType) || "radio".equals(parsedType);
        boolean isClick = "button".equals(parsedType) || "click".equals(parsedType);
        boolean isSlider = "slider".equals(parsedType);
        
        double score = 0.0;
        String text = el.text.trim();
        String lowerText = text.toLowerCase();
        String lowerTitle = el.title.toLowerCase();
        String lowerLabel = el.label.toLowerCase();
        String lowerRole = el.role.toLowerCase();
        String lowerType = el.type.toLowerCase();
        
        // ========== TIER 1: EXACT MATCHES (140-150 points) ==========
        // Clean text for comparison (no newlines/tabs)
        String cleanElText = text.replaceAll("\\s+", " ").trim().toLowerCase();
        String cleanTarget = name.replaceAll("\\s+", " ").trim().toLowerCase();
        String cleanTargetCleaned = cleanName.replaceAll("\\s+", " ").trim().toLowerCase();
        
        boolean matchedExact = false;
        if (cleanTarget.equals(cleanElText) || cleanTargetCleaned.equals(cleanElText)) {
            score += 150;
            matchedExact = true;
        } else if (name.equalsIgnoreCase(el.label) || cleanName.equalsIgnoreCase(lowerLabel)) {
            score += 150;
            matchedExact = true;
        } else if (name.equalsIgnoreCase(el.placeholder) || cleanName.equalsIgnoreCase(el.placeholder)) {
            score += 140;
            matchedExact = true;
        } else if (name.equalsIgnoreCase(el.title) || cleanName.equalsIgnoreCase(lowerTitle)) {
            score += 140;
            matchedExact = true;
        }
        
        // Boost for specific indicator tags when looking for form elements
        if (isFill || "select".equals(parsedType) || isSlider) {
            String tag = el.tag.toLowerCase();
            if (tag.equals("label") || tag.equals("b") || tag.equals("strong") || tag.equals("p") || tag.equals("span")) {
                score += 30; // Boost label-like elements
            }
        }
        
        // ========== TIER 2: BIDIRECTIONAL CONTAINS (100-120 points) ==========
        if (!matchedExact) {
            if (!el.title.isEmpty() && (lowerName.contains(lowerTitle) || lowerTitle.contains(lowerName))) {
                score += 120;
            } else if (!el.label.isEmpty() && (lowerName.contains(lowerLabel) || lowerLabel.contains(lowerName))) {
                score += 120;
            } else if (!text.isEmpty() && (lowerText.contains(lowerName) || lowerText.contains(cleanName))) {
                score += 110;
            } else if (name.equalsIgnoreCase(el.id) || cleanName.equalsIgnoreCase(el.id)) {
                score += 100;
            } else if (name.equalsIgnoreCase(el.name) || cleanName.equalsIgnoreCase(el.name)) {
                score += 100;
            }
        }

        // Fuzzy matching only if no solid match yet
        if (score < 100) {
             if (text.toLowerCase().contains(lowerName) || text.toLowerCase().contains(cleanName)) score += 45; 
             if (el.id.toLowerCase().contains(lowerName) || el.id.toLowerCase().contains(cleanName)) score += 45; 
             if (el.name.toLowerCase().contains(lowerName) || el.name.toLowerCase().contains(cleanName)) score += 45; 
             if (el.title.toLowerCase().contains(lowerName) || el.title.toLowerCase().contains(cleanName)) score += 35; 
             
             try {
                if (FuzzyMatch.ratio(name, text) > 85 || FuzzyMatch.ratio(cleanName, text) > 85) score += 30;
                if (FuzzyMatch.ratio(name, el.id) > 85 || FuzzyMatch.ratio(cleanName, el.id) > 85) score += 30;
                if (FuzzyMatch.ratio(name, el.title) > 85 || FuzzyMatch.ratio(cleanName, el.title) > 85) score += 30;
             } catch (Throwable t) {
             }
        }

        // ========== PENALTY FOR SEARCH/FILTER FIELDS ==========
        // Search fields often have generic names like 's', 'search', or placeholders with 'search'/'filter'/'type'
        // These should NOT be selected when looking for named form fields like "Name", "Email", etc.
        String lowerPlaceholder = el.placeholder.toLowerCase();
        String lowerElName = el.name.toLowerCase();
        if (isFill && score < 100) { // Only penalize if not a strong match already
            boolean isSearchField = false;
            
            // Common search field indicators
            if ("s".equals(lowerElName) || "search".equals(lowerElName) || "q".equals(lowerElName) || "query".equals(lowerElName)) {
                isSearchField = true;
            }
            
            // Placeholder text indicates search/filter
            if (lowerPlaceholder.contains("search") || lowerPlaceholder.contains("filter") || 
                lowerPlaceholder.contains("type here") || lowerPlaceholder.contains("start typing")) {
                isSearchField = true;
            }
            
            // Heavy penalty for search fields when looking for specific named fields
            if (isSearchField && !lowerName.contains("search") && !lowerName.contains("filter")) {
                score -= 150; // This will make search fields score negative if they don't strongly match
            }
        }

        // ========== TIER 3: TYPE-SPECIFIC BOOSTS AND PENALTIES ==========
        if (isFill) {
            String tag = el.tag.toLowerCase();
            if (("input".equals(tag) || "textarea".equals(tag)) && !"checkbox".equals(el.type) && !"radio".equals(el.type) && !"range".equals(el.type)) {
                score += 50;
            } else {
                score -= 100; // Penalize non-inputs for FILL
            }
        }
        if (isCheck) {
            if ("input".equals(el.tag) && ("checkbox".equals(el.type) || "radio".equals(el.type))) {
                score += 100;
            } else {
                score -= 100; // Penalize non-checkboxes for CHECK
            }
        }
        if (isClick) {
            if ("button".equals(el.tag) || "a".equals(el.tag) || "submit".equals(el.type) || el.className.contains("btn") || "button".equals(lowerRole)) {
                score += 50;
            }
            // No penalty for click as almost anything can be clicked
        } else {
            // General boost for interactive tags even if action type is unknown
            if ("button".equals(el.tag) || "a".equals(el.tag) || "submit".equals(el.type) || "button".equals(lowerRole)) {
                score += 10;
            }
        }
        if (isSlider) {
            if ("range".equals(lowerType) || "slider".equals(lowerRole)) {
                score += 500; // Huge boost for ACTUAL sliders
            } else if (el.className.toLowerCase().contains("slider") || el.className.toLowerCase().contains("range")) {
                score += 100; // Moderate boost for potential custom sliders
            } else {
                score -= 200; // HEAVY penalty for non-slider elements when a slider is requested
            }
        }
        if ("progressbar".equals(lowerType) || "progressbar".equals(lowerRole)) {
            score += 500; // Found exact role
        } else if (el.className.toLowerCase().contains("progress")) {
            score += 100; // Likely a progress component
        }
        
        // ========== TIER 4: VISIBILITY BOOST AND LENGTH PENALTY ==========
        // Substantial boost for visible elements to prefer them over hidden duplicates
        if (el.visible) {
            score += 100;
        }
        
        // Penalty for giant containers (too much text compared to target)
        if (text.length() > 100 && text.length() > name.length() * 5) {
            score -= 150; // Heavy penalty for "giant" containers like root div
        }
        
        return score;
    }
}
