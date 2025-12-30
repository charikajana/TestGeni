package automation.browser.actions.table;

import automation.browser.actions.BrowserAction;
import automation.browser.SmartLocator;
import automation.planner.ActionPlan;
import automation.planner.EnhancedActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Locator;

import java.util.HashMap;
import java.util.Map;

/**
 * Action to extract all column values from a table row based on a condition.
 * Example: "get all column values where name is 'John'"
 */
public class GetRowValuesAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(GetRowValuesAction.class);
    
    @Override
    public boolean execute(Page page, SmartLocator locator, ActionPlan plan) {
        if (!(plan instanceof EnhancedActionPlan)) {
            logger.failure("GetRowValuesAction requires EnhancedActionPlan");
            return false;
        }
        
        EnhancedActionPlan enhancedPlan = (EnhancedActionPlan) plan;
        
        String conditionColumn = enhancedPlan.getRowConditionColumn();
        String conditionValue = enhancedPlan.getRowConditionValue();
        
        if (conditionColumn == null || conditionValue == null) {
            logger.failure("Missing condition column or value for row extraction");
            return false;
        }
        
        logger.info("Extracting all column values from row where '{}' = '{}'", conditionColumn, conditionValue);
        
        try {
            // Detect table type and configuration
            TableConfig tableConfig = detectTableType(page);
            
            if (tableConfig == null) {
                logger.failure("No supported table found on the page");
                logger.error("   Tried: HTML, React Table, AG Grid, Material-UI, Ant Design, Tabulator, Angular Material");
                return false;
            }
            
            logger.success("Detected: {}", tableConfig.type);
            
            Locator table = tableConfig.table;
            Locator headerCells = tableConfig.headerCells;
            Locator rows = tableConfig.rows;
            
            int columnCount = headerCells.count();
            
            if (columnCount == 0) {
                logger.failure("No table headers found");
                return false;
            }
            
            logger.info("Found {} columns", columnCount);
            
            // Build column name to index mapping
            Map<String, Integer> columnIndexMap = new HashMap<>();
            for (int i = 0; i < columnCount; i++) {
                String headerText = headerCells.nth(i).textContent().trim();
                // Skip empty headers (common in React Tables for action columns)
                if (!headerText.isEmpty()) {
                    columnIndexMap.put(headerText.toLowerCase(), i);
                    logger.debug("   Column {}: {}", i, headerText);
                }
            }
            
            // Find the index of the condition column
            Integer conditionColumnIndex = columnIndexMap.get(conditionColumn.toLowerCase());
            if (conditionColumnIndex == null) {
                logger.failure("Column '{}' not found in table headers", conditionColumn);
                logger.error("   Available columns: {}", columnIndexMap.keySet());
                return false;
            }
            
            // Find all table rows based on table type
            int rowCount = rows.count();
            
            logger.info("Searching through {} rows...", rowCount);
            
            // Search for the row matching the condition
            for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
                Locator row = rows.nth(rowIndex);
                Locator cells = row.locator(tableConfig.cellSelector);
                
                if (cells.count() <= conditionColumnIndex) {
                    continue; // Skip rows with insufficient columns
                }
                
                String cellValue = cells.nth(conditionColumnIndex).textContent().trim();
                
                // Check if this row matches the condition
                if (cellValue.equalsIgnoreCase(conditionValue) || cellValue.contains(conditionValue)) {
                    logger.success("Found matching row at index {}", rowIndex);
                    
                    // Extract all column values from this row
                    Map<String, String> rowData = new HashMap<>();
                    
                    for (Map.Entry<String, Integer> entry : columnIndexMap.entrySet()) {
                        String columnName = entry.getKey();
                        int columnIndex = entry.getValue();
                        
                        if (cells.count() > columnIndex) {
                            String value = cells.nth(columnIndex).textContent().trim();
                            rowData.put(columnName, value);
                        }
                    }
                    
                    // Display the extracted data
                    logger.info("\n---------------------------------------------");
                    logger.info("| Extracted Row Data:");
                    logger.info("---------------------------------------------");
                    
                    // Get original column names for display
                    for (int i = 0; i < columnCount; i++) {
                        String originalColumnName = headerCells.nth(i).textContent().trim();
                        String value = rowData.get(originalColumnName.toLowerCase());
                        
                        if (!originalColumnName.isEmpty() && value != null) {
                            logger.info(String.format("| %-20s : %s", originalColumnName, value));
                        }
                    }
                    
                    logger.info("---------------------------------------------\n");
                    
                    // Store data in the plan for potential later use
                    enhancedPlan.setExtractedData(rowData);
                    
                    return true;
                }
            }
            
            logger.failure("No row found where '{}' = '{}'", conditionColumn, conditionValue);
            return false;
            
        } catch (Exception e) {
            logger.error("Failed to extract row values: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Configuration class to hold table-specific selectors
     */
    private static class TableConfig {
        String type;
        Locator table;
        Locator headerCells;
        Locator rows;
        String cellSelector;
        
        TableConfig(String type, Locator table, Locator headerCells, Locator rows, String cellSelector) {
            this.type = type;
            this.table = table;
            this.headerCells = headerCells;
            this.rows = rows;
            this.cellSelector = cellSelector;
        }
    }
    
    /**
     * Detects the type of table on the page and returns appropriate configuration
     * Supports: HTML tables, React Table, AG Grid, Material-UI, Ant Design, Tabulator, Angular Material
     */
    private TableConfig detectTableType(Page page) {
        Locator table;
        Locator headerCells;
        Locator rows;
        
        // 1. Standard HTML Table
        if (page.locator("table").count() > 0) {
            table = page.locator("table").first();
            headerCells = table.locator("thead th, thead td");
            if (headerCells.count() > 0) {
                rows = table.locator("tbody tr");
                return new TableConfig("Standard HTML Table", table, headerCells, rows, "td");
            }
        }
        
        // 2. React Table (react-table library)
        if (page.locator(".rt-table").count() > 0) {
            table = page.locator(".rt-table").first();
            headerCells = table.locator(".rt-thead .rt-th");
            if (headerCells.count() > 0) {
                rows = table.locator(".rt-tbody .rt-tr-group");
                return new TableConfig("React Table", table, headerCells, rows, ".rt-tr .rt-td");
            }
        }
        
        // 3. AG Grid
        if (page.locator(".ag-root").count() > 0 || page.locator(".ag-header").count() > 0) {
            table = page.locator(".ag-root, .ag-body-viewport").first();
            headerCells = page.locator(".ag-header-cell");
            if (headerCells.count() > 0) {
                rows = page.locator(".ag-row");
                return new TableConfig("AG Grid", table, headerCells, rows, ".ag-cell");
            }
        }
        
        // 4. Material-UI Table (MUI)
        if (page.locator(".MuiTable-root").count() > 0) {
            table = page.locator(".MuiTable-root").first();
            headerCells = table.locator(".MuiTableHead-root th");
            if (headerCells.count() > 0) {
                rows = table.locator(".MuiTableBody-root tr");
                return new TableConfig("Material-UI Table", table, headerCells, rows, "td");
            }
        }
        
        // 5. Ant Design Table
        if (page.locator(".ant-table").count() > 0) {
            table = page.locator(".ant-table").first();
            headerCells = table.locator(".ant-table-thead th");
            if (headerCells.count() > 0) {
                rows = table.locator(".ant-table-tbody tr");
                return new TableConfig("Ant Design Table", table, headerCells, rows, "td");
            }
        }
        
        // 6. Tabulator
        if (page.locator(".tabulator").count() > 0) {
            table = page.locator(".tabulator").first();
            headerCells = table.locator(".tabulator-header .tabulator-col");
            if (headerCells.count() > 0) {
                rows = table.locator(".tabulator-tableHolder .tabulator-row");
                return new TableConfig("Tabulator", table, headerCells, rows, ".tabulator-cell");
            }
        }
        
        // 7. Angular Material Table
        if (page.locator("mat-table, .mat-table").count() > 0) {
            table = page.locator("mat-table, .mat-table").first();
            headerCells = table.locator("mat-header-row .mat-header-cell, .mat-header-row .mat-header-cell");
            if (headerCells.count() > 0) {
                rows = table.locator("mat-row, .mat-row");
                return new TableConfig("Angular Material Table", table, headerCells, rows, "mat-cell, .mat-cell");
            }
        }
        
        // No supported table found
        return null;
    }
}
