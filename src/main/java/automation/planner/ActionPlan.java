package automation.planner;

public class ActionPlan {

    private String actionType;      
    private String target;          
    private String value;           
    private String keyword;         
    private int priority = 0;       
    private String elementName;     
    private String locatorStrategy; 
    private boolean executed = false;  
    private boolean isNegated = false;  // For negative verification (e.g., "not displayed")
    private String rowAnchor;
    private String frameAnchor;
    private String parentAnchor;
    private java.util.Map<String, Object> metadata;  // For intelligent processing

    public ActionPlan(String actionType, String target) {
        this.actionType = actionType;
        this.target = target;
        this.metadata = new java.util.HashMap<>();
    }
    
    public ActionPlan() {
        this.metadata = new java.util.HashMap<>();
    }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public String getLocatorStrategy() { return locatorStrategy; }
    public void setLocatorStrategy(String locatorStrategy) { this.locatorStrategy = locatorStrategy; }

    public boolean isExecuted() { return executed; }
    public void setExecuted(boolean executed) { this.executed = executed; }

    public String getElementName() { return elementName; }
    public void setElementName(String elementName) { this.elementName = elementName; }

    public String getRowAnchor() { return rowAnchor; }
    public void setRowAnchor(String rowAnchor) { this.rowAnchor = rowAnchor; }

    public String getFrameAnchor() { return frameAnchor; }
    public void setFrameAnchor(String frameAnchor) { this.frameAnchor = frameAnchor; }
    
    public String getParentAnchor() { return parentAnchor; }
    public void setParentAnchor(String parentAnchor) { this.parentAnchor = parentAnchor; }
    
    public boolean isNegated() { return isNegated; }
    public void setNegated(boolean negated) { this.isNegated = negated; }
    
    // Metadata methods for intelligent processing
    public void setMetadataValue(String key, Object value) {
        metadata.put(key, value);
    }
    
    public Object getMetadataValue(String key) {
        return metadata.get(key);
    }
    
    public boolean hasMetadata(String key) {
        return metadata.containsKey(key);
    }
    
    // Validation
    public boolean isValid() {
        return actionType != null && !actionType.equals("unknown");
    }

    @Override
    public String toString() {
        return "ActionPlan{" +
                "actionType='" + actionType + '\'' +
                ", elementName='" + elementName + '\'' +
                ", target='" + target + '\'' +
                ", value='" + value + '\'' +
                ", keyword='" + keyword + '\'' +
                ", priority=" + priority +
                ", locatorStrategy='" + locatorStrategy + '\'' +
                ", rowAnchor='" + rowAnchor + '\'' +
                ", frameAnchor='" + frameAnchor + '\'' +
                ", parentAnchor='" + parentAnchor + '\'' +
                ", isNegated=" + isNegated +
                ", executed=" + executed +
                '}';
    }
}
