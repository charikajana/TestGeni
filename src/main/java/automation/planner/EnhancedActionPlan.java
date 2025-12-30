package automation.planner;

/**
 * Enhanced ActionPlan model to support advanced table operations.
 * Extended with new fields for column names, sort orders, page numbers, etc.
 */
public class EnhancedActionPlan extends ActionPlan {
    
    // Table-specific fields
    private String tableName;
    private String columnName;
    private String targetColumnName;  // For "in column X, verify Y"
    private String sortOrder;          // "ascending" or "descending"
    private Integer pageNumber;
    private Integer rowNumber;
    private String filterValue;
    private String comparisonOperator; // "equals", "contains", "greater than"
    
    // Row selection
    private String rowConditionColumn;  // "where Email is..."
    private String rowConditionValue;
    
    // Bulk operations
    private boolean isBulkAction = false;
    private String bulkActionType;      // "select_all", "deselect_all", "delete_selected"
    
    // Validation
    private String expectedValue;
    private Integer expectedRowCount;
    private boolean shouldExist = true;  // For "should exist" vs "should not exist"
    
    // Data extraction
    private java.util.Map<String, String> extractedData;  // Store extracted row data

    
    public EnhancedActionPlan(String actionType, String target) {
        super(actionType, target);
    }
    
    // Getters and Setters
    
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    
    public String getColumnName() { return columnName; }
    public void setColumnName(String columnName) { this.columnName = columnName; }
    
    public String getTargetColumnName() { return targetColumnName; }
    public void setTargetColumnName(String targetColumnName) { this.targetColumnName = targetColumnName; }
    
    public String getSortOrder() { return sortOrder; }
    public void setSortOrder(String sortOrder) { this.sortOrder = sortOrder; }
    
    public Integer getPageNumber() { return pageNumber; }
    public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }
    
    public Integer getRowNumber() { return rowNumber; }
    public void setRowNumber(Integer rowNumber) { this.rowNumber = rowNumber; }
    
    public String getFilterValue() { return filterValue; }
    public void setFilterValue(String filterValue) { this.filterValue = filterValue; }
    
    public String getComparisonOperator() { return comparisonOperator; }
    public void setComparisonOperator(String operator) { this.comparisonOperator = operator; }
    
    public String getRowConditionColumn() { return rowConditionColumn; }
    public void setRowConditionColumn(String column) { this.rowConditionColumn = column; }
    
    public String getRowConditionValue() { return rowConditionValue; }
    public void setRowConditionValue(String value) { this.rowConditionValue = value; }
    
    public boolean isBulkAction() { return isBulkAction; }
    public void setIsBulkAction(boolean bulk) { this.isBulkAction = bulk; }
    
    public String getBulkActionType() { return bulkActionType; }
    public void setBulkActionType(String type) { this.bulkActionType = type; }
    
    public String getExpectedValue() { return expectedValue; }
    public void setExpectedValue(String value) { this.expectedValue = value; }
    
    public Integer getExpectedRowCount() { return expectedRowCount; }
    public void setExpectedRowCount(Integer count) { this.expectedRowCount = count; }
    
    public boolean shouldExist() { return shouldExist; }
    public void setShouldExist(boolean exist) { this.shouldExist = exist; }
    
    public java.util.Map<String, String> getExtractedData() { return extractedData; }
    public void setExtractedData(java.util.Map<String, String> data) { this.extractedData = data; }

    
    @Override
    public String toString() {
        return super.toString() + 
               " | Table: " + tableName +
               " | Column: " + columnName +
               " | RowCondition: " + rowConditionColumn + "=" + rowConditionValue;
    }
}
