package automation.planner;

import java.util.List;
import java.util.ArrayList;

/**
 * Represents a composite action plan that contains multiple sub-actions.
 * Used when a single step contains multiple actions chained together.
 * 
 * Example:
 * "When Enter FirstName and Enter LastName and Click Submit"
 * 
 * This will create a CompositeActionPlan with 3 sub-actions:
 * 1. Enter FirstName
 * 2. Enter LastName
 * 3. Click Submit
 */
public class CompositeActionPlan extends ActionPlan {
    
    private final List<ActionPlan> subActions;
    
    public CompositeActionPlan(String originalStep, List<ActionPlan> subActions) {
        super("composite", originalStep);
        this.subActions = new ArrayList<>(subActions);
        this.setLocatorStrategy("composite-multi-action");
    }
    
    /**
     * Returns the list of sub-actions to be executed sequentially.
     */
    public List<ActionPlan> getSubActions() {
        return new ArrayList<>(subActions);
    }
    
    /**
     * Returns the number of sub-actions.
     */
    public int getSubActionCount() {
        return subActions.size();
    }
    
    /**
     * Checks if this is a composite action plan.
     */
    public boolean isComposite() {
        return true;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CompositeActionPlan{");
        sb.append("step='").append(getTarget()).append("'");
        sb.append(", subActionCount=").append(subActions.size());
        sb.append(", subActions=[");
        
        for (int i = 0; i < subActions.size(); i++) {
            if (i > 0) sb.append(", ");
            ActionPlan sub = subActions.get(i);
            sb.append(sub.getActionType());
        }
        
        sb.append("]}");
        return sb.toString();
    }
}
