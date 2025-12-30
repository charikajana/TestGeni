package automation.reporting;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Captures complete execution details for each step.
 * Provides JSON serialization for logging and analytics.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StepExecutionReport {
    
    private static final ObjectMapper mapper = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT);
    
    @JsonProperty("stepName")
    private String stepName;
    
    @JsonProperty("status")
    private String status;  // PASSED, FAILED, SKIPPED
    
    @JsonProperty("action")
    private String action;  // click, fill, verify, etc.
    
    @JsonProperty("locatorIdentified")
    private LocatorDetails locatorIdentified;
    
    @JsonProperty("semanticLocator")
    private SemanticDetails semanticLocator;
    
    @JsonProperty("executionTime")
    private String executionTime;
    
    @JsonProperty("duration")
    private Long durationMs;
    
    @JsonProperty("errorMessage")
    private String errorMessage;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    @JsonProperty("validation")
    private ValidationResult validation;  // For verification/validation actions
    
    public StepExecutionReport() {
        this.metadata = new HashMap<>();
        this.executionTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
    
    // Fluent builder methods
    public StepExecutionReport stepName(String stepName) {
        this.stepName = stepName;
        return this;
    }
    
    public StepExecutionReport status(String status) {
        this.status = status;
        return this;
    }
    
    public StepExecutionReport action(String action) {
        this.action = action;
        return this;
    }
    
    public StepExecutionReport locatorIdentified(LocatorDetails locator) {
        this.locatorIdentified = locator;
        return this;
    }
    
    public StepExecutionReport semanticLocator(SemanticDetails semantic) {
        this.semanticLocator = semantic;
        return this;
    }
    
    public StepExecutionReport duration(Long durationMs) {
        this.durationMs = durationMs;
        return this;
    }
    
    public StepExecutionReport errorMessage(String error) {
        this.errorMessage = error;
        return this;
    }
    
    public StepExecutionReport addMetadata(String key, Object value) {
        this.metadata.put(key, value);
        return this;
    }
    
    public StepExecutionReport validation(ValidationResult validation) {
        this.validation = validation;
        return this;
    }
    
    /**
     * Convert to formatted JSON string
     */
    public String toJson() {
        try {
            return mapper.writeValueAsString(this);
        } catch (Exception e) {
            return "{\"error\": \"Failed to serialize: " + e.getMessage() + "\"}";
        }
    }
    
    /**
     * Convert to compact JSON (single line)
     */
    public String toCompactJson() {
        try {
            ObjectMapper compactMapper = new ObjectMapper();
            return compactMapper.writeValueAsString(this);
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
    
    // Getters (for Jackson)
    public String getStepName() { return stepName; }
    public String getStatus() { return status; }
    public String getAction() { return action; }
    public LocatorDetails getLocatorIdentified() { return locatorIdentified; }
    public SemanticDetails getSemanticLocator() { return semanticLocator; }
    public String getExecutionTime() { return executionTime; }
    public Long getDuration() { return durationMs; }
    public String getErrorMessage() { return errorMessage; }
    public Map<String, Object> getMetadata() { return metadata; }
    public ValidationResult getValidation() { return validation; }
    
    /**
     * Locator details
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LocatorDetails {
        @JsonProperty("elementType")
        private String elementType;
        
        @JsonProperty("elementText")
        private String elementText;
        
        @JsonProperty("selector")
        private String selector;
        
        @JsonProperty("matchStrategy")
        private String matchStrategy;  // EXACT, FUZZY, SEMANTIC, PATTERN
        
        @JsonProperty("score")
        private Double score;
        
        public LocatorDetails() {}
        
        public LocatorDetails elementType(String type) {
            this.elementType = type;
            return this;
        }
        
        public LocatorDetails elementText(String text) {
            this.elementText = text;
            return this;
        }
        
        public LocatorDetails selector(String selector) {
            this.selector = selector;
            return this;
        }
        
        public LocatorDetails matchStrategy(String strategy) {
            this.matchStrategy = strategy;
            return this;
        }
        
        public LocatorDetails score(Double score) {
            this.score = score;
            return this;
        }
        
        // Getters
        public String getElementType() { return elementType; }
        public String getElementText() { return elementText; }
        public String getSelector() { return selector; }
        public String getMatchStrategy() { return matchStrategy; }
        public Double getScore() { return score; }
    }
    
    /**
     * Semantic processing details
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SemanticDetails {
        @JsonProperty("intentExtracted")
        private String intentExtracted;  // CLICK, FILL, VERIFY, etc.
        
        @JsonProperty("targetDescription")
        private String targetDescription;
        
        @JsonProperty("valueProvided")
        private String valueProvided;
        
        @JsonProperty("intelligenceUsed")
        private Boolean intelligenceUsed;
        
        @JsonProperty("learningBoost")
        private Double learningBoost;
        
        @JsonProperty("candidatesEvaluated")
        private Integer candidatesEvaluated;
        
        public SemanticDetails() {}
        
        public SemanticDetails intentExtracted(String intent) {
            this.intentExtracted = intent;
            return this;
        }
        
        public SemanticDetails targetDescription(String target) {
            this.targetDescription = target;
            return this;
        }
        
        public SemanticDetails valueProvided(String value) {
            this.valueProvided = value;
            return this;
        }
        
        public SemanticDetails intelligenceUsed(Boolean used) {
            this.intelligenceUsed = used;
            return this;
        }
        
        public SemanticDetails learningBoost(Double boost) {
            this.learningBoost = boost;
            return this;
        }
        
        public SemanticDetails candidatesEvaluated(Integer count) {
            this.candidatesEvaluated = count;
            return this;
        }
        
        // Getters
        public String getIntentExtracted() { return intentExtracted; }
        public String getTargetDescription() { return targetDescription; }
        public String getValueProvided() { return valueProvided; }
        public Boolean getIntelligenceUsed() { return intelligenceUsed; }
        public Double getLearningBoost() { return learningBoost; }
        public Integer getCandidatesEvaluated() { return candidatesEvaluated; }
    }
    
    /**
     * Validation/Verification result details
     * Contains expected vs actual comparison for user logic
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ValidationResult {
        @JsonProperty("expected")
        private String expected;
        
        @JsonProperty("actual")
        private String actual;
        
        @JsonProperty("comparisonType")
        private String comparisonType;  // EXACT, CONTAINS, REGEX, NUMERIC, BOOLEAN
        
        @JsonProperty("match")
        private Boolean match;
        
        @JsonProperty("elementFound")
        private Boolean elementFound;
        
        @JsonProperty("elementVisible")
        private Boolean elementVisible;
        
        @JsonProperty("details")
        private String details;  // Additional context (e.g., "Text found in modal")
        
        public ValidationResult() {}
        
        public ValidationResult expected(String expected) {
            this.expected = expected;
            return this;
        }
        
        public ValidationResult actual(String actual) {
            this.actual = actual;
            return this;
        }
        
        public ValidationResult comparisonType(String type) {
            this.comparisonType = type;
            return this;
        }
        
        public ValidationResult match(Boolean match) {
            this.match = match;
            return this;
        }
        
        public ValidationResult elementFound(Boolean found) {
            this.elementFound = found;
            return this;
        }
        
        public ValidationResult elementVisible(Boolean visible) {
            this.elementVisible = visible;
            return this;
        }
        
        public ValidationResult details(String details) {
            this.details = details;
            return this;
        }
        
        // Getters
        public String getExpected() { return expected; }
        public String getActual() { return actual; }
        public String getComparisonType() { return comparisonType; }
        public Boolean getMatch() { return match; }
        public Boolean getElementFound() { return elementFound; }
        public Boolean getElementVisible() { return elementVisible; }
        public String getDetails() { return details; }
    }
}
