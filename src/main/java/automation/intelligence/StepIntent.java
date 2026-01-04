package automation.intelligence;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the extracted intent from a natural language step.
 * Contains all semantic information needed for intelligent execution.
 */
public class StepIntent {
    
    private String originalStep;
    private String cleanStep;
    private IntentAnalyzer.ActionType actionType;
    private String targetDescription;
    private String value;
    private java.util.List<String> values = new java.util.ArrayList<>();
    private String elementType;
    private String parentReference; // NEW: For "Click X inside Y"
    private boolean isNegated = false;  // For negative assertions (e.g., "not displayed")
    private Map<String, String> modifiers;
    
    public StepIntent() {
        this.modifiers = new HashMap<>();
    }
    
    public void addValue(String val) {
        if (this.value == null) this.value = val;
        this.values.add(val);
    }
    
    public java.util.List<String> getValues() {
        return values;
    }
    
    public void setValues(java.util.List<String> values) {
        this.values = values;
        if (values != null && !values.isEmpty()) {
            this.value = values.get(0);
        }
    }
    
    // Getters and Setters
    
    public String getOriginalStep() {
        return originalStep;
    }
    
    public void setOriginalStep(String originalStep) {
        this.originalStep = originalStep;
    }
    
    public String getCleanStep() {
        return cleanStep;
    }
    
    public void setCleanStep(String cleanStep) {
        this.cleanStep = cleanStep;
    }
    
    public IntentAnalyzer.ActionType getActionType() {
        return actionType;
    }
    
    public void setActionType(IntentAnalyzer.ActionType actionType) {
        this.actionType = actionType;
    }
    
    public String getTargetDescription() {
        return targetDescription;
    }
    
    public void setTargetDescription(String targetDescription) {
        this.targetDescription = targetDescription;
    }
    
    public String getValue() {
        return value;
    }
    
    public void setValue(String value) {
        this.value = value;
    }
    
    public String getElementType() {
        return elementType;
    }
    
    public void setElementType(String elementType) {
        this.elementType = elementType;
    }
    
    public String getParentReference() {
        return parentReference;
    }
    
    public void setParentReference(String parentReference) {
        this.parentReference = parentReference;
    }
    
    public boolean isNegated() {
        return isNegated;
    }
    
    public void setNegated(boolean negated) {
        this.isNegated = negated;
    }
    
    public Map<String, String> getModifiers() {
        return modifiers;
    }
    
    public void setModifiers(Map<String, String> modifiers) {
        this.modifiers = modifiers;
    }
    
    public boolean hasModifier(String key) {
        return modifiers.containsKey(key);
    }
    
    public String getModifier(String key) {
        return modifiers.get(key);
    }
    
    @Override
    public String toString() {
        return String.format("StepIntent{action=%s, target='%s', value='%s', type='%s', negated=%s, modifiers=%s}",
            actionType, targetDescription, value, elementType, isNegated, modifiers);
    }
}
