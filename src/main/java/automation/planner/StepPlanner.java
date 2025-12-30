package automation.planner;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StepPlanner {

    private final List<StepPattern> patterns = new ArrayList<>();

    public StepPlanner() {
        PatternRegistry.registerAllPatterns(this::addPattern);
    }

    public ActionPlan plan(String step) {
        step = step.trim();
        ActionPlan plan = null;

        String keyword = extractKeyword(step);
        String cleanStep = step.replaceAll("^(?i)(Given|When|Then|And|But)\\s+", "");

        for (StepPattern p : patterns) {
            Matcher m = p.regex.matcher(cleanStep);
            if (m.find()) {
                plan = new ActionPlan(p.actionType, step);
                
                if (p.elementGroup != -1 && m.groupCount() >= p.elementGroup) {
                    plan.setElementName(stripQuotes(m.group(p.elementGroup)));
                }
                
                // Special handling for select_multi: extract ALL quoted values
                if ("select_multi".equals(p.actionType)) {
                    String dropdownName = plan.getElementName();
                    List<String> allQuoted = extractAllQuoted(cleanStep);
                    List<String> values = new ArrayList<>();
                    
                    for (String q : allQuoted) {
                        // All quoted strings EXCEPT the dropdown name are treated as values
                        if (!q.equalsIgnoreCase(dropdownName)) {
                            values.add(q);
                        }
                    }
                    
                    if (!values.isEmpty()) {
                        plan.setValue(String.join(";", values));
                    }
                    // Change action type to "select" so SelectAction can handle it
                    plan.setActionType("select");
                } else if (p.valueGroup != -1 && m.groupCount() >= p.valueGroup) {
                    plan.setValue(stripQuotes(m.group(p.valueGroup)));
                }

                if (p.rowAnchorGroup != -1 && m.groupCount() >= p.rowAnchorGroup) {
                    plan.setRowAnchor(stripQuotes(m.group(p.rowAnchorGroup)));
                }

                plan.setLocatorStrategy("regex-smart");
                plan.setKeyword(keyword);
                return plan;
            }
        }

        return fallbackPlan(step, keyword);
    }

    private ActionPlan fallbackPlan(String step, String keyword) {
        String cleanStep = step.replaceAll("^(?i)(Given|When|Then|And|But)\\s+", "");
        String lower = step.toLowerCase();
        ActionPlan plan = new ActionPlan("unknown", step);
        plan.setKeyword(keyword);

        if (lower.contains("click")) {
            plan.setActionType("click");
            String quoted = extractQuoted(step);
            if (quoted != null) {
                plan.setElementName(quoted);
            } else {
                plan.setElementName(cleanStep.replaceAll("^(?i)(?:click|tap|press|hit)(?:\\s+on)?\\s+", "").trim());
            }
        } else if (lower.contains("enter") || lower.contains("fill")) {
            plan.setActionType("fill");
            List<String> q = extractAllQuoted(step);
            if (!q.isEmpty()) plan.setValue(q.get(0)); 
            if (q.size() > 1) plan.setElementName(q.get(1));
        } else if (lower.contains("wait") || lower.contains("load")) {
            plan.setActionType("wait");
        } else if (lower.contains("screen") || lower.contains("shot")) {
            plan.setActionType("screenshot");
        }

        return plan;
    }

    private void addPattern(String action, String regex, int elementGroup, int valueGroup, int rowAnchorGroup) {
        patterns.add(new StepPattern(Pattern.compile(regex, Pattern.CASE_INSENSITIVE), action, elementGroup, valueGroup, rowAnchorGroup));
    }

    private String extractKeyword(String step) {
        String[] keys = {"Given", "When", "Then", "And", "But"};
        for (String k : keys) {
            if (step.trim().matches("(?i)^" + k + " .*")) return k;
        }
        return "And"; 
    }

    private String extractQuoted(String text) {
        Matcher m = Pattern.compile("[\"']([^\"']+)[\"']").matcher(text);
        return m.find() ? m.group(1) : null;
    }

    private String stripQuotes(String text) {
        if (text == null) return null;
        text = text.trim();
        if ((text.startsWith("\"") && text.endsWith("\"")) || (text.startsWith("'") && text.endsWith("'"))) {
            return text.substring(1, text.length() - 1);
        }
        return text;
    }

    private List<String> extractAllQuoted(String text) {
        List<String> res = new ArrayList<>();
        Matcher m = Pattern.compile("[\"']([^\"']+)[\"']").matcher(text);
        while (m.find()) res.add(m.group(1));
        return res;
    }

    private static class StepPattern {
        Pattern regex;
        String actionType;
        int elementGroup;
        int valueGroup;
        int rowAnchorGroup;

        StepPattern(Pattern regex, String actionType, int elementGroup, int valueGroup, int rowAnchorGroup) {
            this.regex = regex;
            this.actionType = actionType;
            this.elementGroup = elementGroup;
            this.valueGroup = valueGroup;
            this.rowAnchorGroup = rowAnchorGroup;
        }
    }
}
