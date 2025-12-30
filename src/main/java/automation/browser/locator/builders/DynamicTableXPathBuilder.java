package automation.browser.locator.builders;

import automation.utils.LoggerUtil;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Dynamic XPath Builder for Table Row Finding
 * Works with ANY table structure: HTML tables, React tables, AG-Grid, Material tables, etc.
 */
public class DynamicTableXPathBuilder {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(DynamicTableXPathBuilder.class);
    
    private Page page;
    private String rowSelector;
    private String cellSelector;
    private boolean detected = false;
    
    public DynamicTableXPathBuilder(Page page) {
        this.page = page;
        detectTableStructure();
    }
    
    /**
     * Automatically detects the table structure on the page
     */
    private void detectTableStructure() {
        logger.info("Detecting table structure...");
        
        // Try different table structures - MOST SPECIFIC FIRST
        // ARIA Grid with gridcell (works with React Table, AG-Grid, Material-UI, etc.)
        if (tryStructure("*[role='row']", "*[role='gridcell']", "ARIA Grid Table")) return;
        
        // Div-based tables with class patterns (fallback if no ARIA roles)
        if (tryStructure("div[class*='rt-tr']", "div[class*='rt-td']", "Div-Based Table (class)")) return;
        
        // Standard HTML Table
        if (tryStructure("tr", "td", "HTML Table")) return;
        
        // Generic div-based (last resort)
        if (tryStructure("div[class*='row']", "div[class*='cell'], div[class*='td']", "Div Table")) return;
        
        logger.warning("Could not detect table structure, using fallback");
        rowSelector = "*[role='row'], tr, div[class*='row']";
        cellSelector = "*[role='gridcell'], div[class*='rt-td'], td, div[class*='cell']";
        detected = true;
    }
    
    private boolean tryStructure(String rowSel, String cellSel, String description) {
        Locator rows = page.locator(rowSel);
        if (rows.count() > 0) {
            // Check the SECOND row if available (first might be header)
            Locator rowToCheck = rows.count() > 1 ? rows.nth(1) : rows.first();
            Locator cells = rowToCheck.locator(cellSel);
            if (cells.count() > 0) {
                this.rowSelector = rowSel;
                this.cellSelector = cellSel;
                this.detected = true;
                logger.success("Detected: {}", description);
                logger.debug("   Row selector: {}", rowSel);
                logger.debug("   Cell selector: {}", cellSel);
                return true;
            }
        }
        return false;
    }
    
    /**
     * Builds XPath to find row by column value
     * @param columnName Name of the column (e.g., "First Name")
     * @param columnValue Value to find (e.g., "John")
     * @return Complete XPath string
     */
    public String buildRowXPath(String columnName, String columnValue) {
        if (!detected) {
            detectTableStructure();
        }
        
        // Find column index first
        int columnIndex = findColumnIndex(columnName);
        if (columnIndex < 0) {
            logger.failure("Column '{}' not found", columnName);
            return null;
        }
        
        logger.debug("Column '{}' found at index: {}", columnName, columnIndex);
        
        // Build dynamic XPath
        String xpath = buildDynamicXPath(columnIndex, columnValue);
        logger.debug("Generated XPath: {}", xpath);
        
        return xpath;
    }
    
    /**
     * Finds the index of a column by its header text
     */
    private int findColumnIndex(String columnName) {
        // Try multiple header selectors
        String[] headerSelectors = {
            "th",                           // Standard HTML
            "[role='columnheader']",        // ARIA
            "div[class*='rt-th']",         // Div-based tables
            "div[class*='header-cell']",   // Generic
            "*[class*='thead'] *[class*='cell']"  // Generic nested
        };
        
        for (String selector : headerSelectors) {
            Locator headers = page.locator(selector);
            if (headers.count() == 0) continue;
            
            for (int i = 0; i < headers.count(); i++) {
                try {
                    String headerText = headers.nth(i).innerText().trim();
                    if (headerText.equalsIgnoreCase(columnName)) {
                        return i + 1; // XPath is 1-indexed
                    }
                } catch (Exception e) {
                    // Continue to next
                }
            }
        }
        
        return -1;
    }
    
    /**
     * Builds XPath string dynamically based on detected structure
     */
    private String buildDynamicXPath(int columnIndex, String value) {
        // Escape single quotes in value
        String escapedValue = value.replace("'", "\\'");
        
        // Extract tag name from selector (e.g., "div[role='row']" -> "div")
        String rowTag = extractTagName(rowSelector);
        String cellTag = extractTagName(cellSelector);
        
        // Build flexible XPath that works with the detected structure
        return String.format(
            "//%s[%s][.//%s[%s][%d][contains(., '%s')]]",
            rowTag,
            extractCondition(rowSelector),
            cellTag,
            extractCondition(cellSelector),
            columnIndex,
            escapedValue
        );
    }
    
    private String extractTagName(String selector) {
        // CRITICAL: Check for comma FIRST to handle "td, *[role='gridcell']" correctly
        if (selector.contains(",")) {
            selector = selector.substring(0, selector.indexOf(",")).trim();
        }
        
        // Now extract tag name from the cleaned selector
        if (selector.contains("[")) {
            return selector.substring(0, selector.indexOf("[")).trim();
        }
        if (selector.contains(".")) {
            return selector.substring(0, selector.indexOf(".")).trim();
        }
        
        return selector.trim();
    }
    
    private String extractCondition(String selector) {
        // Handle comma-separated selectors - use only the first one
        String firstSelector = selector.contains(",") ? selector.split(",")[0].trim() : selector;
        
        // Convert CSS selector to XPath condition
        if (firstSelector.contains("[role='row']")) {
            return "@role='row'";
        }
        if (firstSelector.contains("[role='gridcell']")) {
            return "@role='gridcell'";
        }
        if (firstSelector.contains("[class*='rt-tr']")) {
            return "contains(@class, 'rt-tr')";
        }
        if (firstSelector.contains("[class*='rt-td']")) {
            return "contains(@class, 'rt-td')";
        }
        if (firstSelector.contains("[class*='row']")) {
            return "contains(@class, 'row')";
        }
        if (firstSelector.contains("[class*='cell']")) {
            return "contains(@class, 'cell')";
        }
        
        // Default: just match tag (for simple tags like 'td', 'tr', etc.)
        return "true()";
    }
    
    /**
     * Finds a row using the built XPath
     */
    public Locator findRow(String columnName, String columnValue) {
        String xpath = buildRowXPath(columnName, columnValue);
        if (xpath == null) return null;
        
        Locator row = page.locator(xpath).first();
        
        if (row.count() > 0) {
            String preview = row.innerText().replaceAll("\n", " | ");
            logger.success("Row found!");
            logger.debug("   Content: {}", preview.substring(0, Math.min(100, preview.length())));
            return row;
        }
        
        logger.failure("No row found matching XPath");
        return null;
    }
}
