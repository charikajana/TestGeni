package automation.browser.locator.cache;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a cached locator entry with metadata.
 * Stores selector, strategy, and validation information.
 * 
 * @author Chari
 * @version 2.0
 */
public class CachedLocator {
    
    @JsonProperty("elementKey")
    private String elementKey;
    
    @JsonProperty("selector")
    private String selector;
    
    @JsonProperty("locatorStrategy")
    private String locatorStrategy;
    
    // Multiple locator strategies for fallback (ordered by priority)
    @JsonProperty("allLocators")
    private Map<String, String> allLocators; // strategy -> selector mapping
    
    @JsonProperty("pageUrl")
    private String pageUrl;
    
    @JsonProperty("createdAt")
    private String createdAt;
    
    @JsonProperty("lastAccessedAt")
    private String lastAccessedAt;
    
    @JsonProperty("ttlMillis")
    private long ttlMillis;
    
    @JsonProperty("hitCount")
    private int hitCount;
    
    @JsonProperty("elementAttributes")
    private Map<String, String> elementAttributes;
    
    @JsonProperty("verified")
    private boolean verified;
    
    /**
     * Default constructor for Jackson
     */
    public CachedLocator() {
        this.elementAttributes = new HashMap<>();
        this.allLocators = new HashMap<>();
    }
    
    /**
     * Full constructor
     */
    public CachedLocator(String elementKey, String selector, String locatorStrategy,
                        String pageUrl, long ttlMillis, Map<String, String> elementAttributes) {
        this.elementKey = elementKey;
        this.selector = selector;
        this.locatorStrategy = locatorStrategy;
        this.pageUrl = pageUrl;
        this.createdAt = Instant.now().toString();
        this.lastAccessedAt = this.createdAt;
        this.ttlMillis = ttlMillis;
        this.hitCount = 0;
        this.elementAttributes = elementAttributes != null ? new HashMap<>(elementAttributes) : new HashMap<>();
        this.allLocators = new HashMap<>();
        this.allLocators.put(locatorStrategy, selector); // Add primary locator
        this.verified = true;
    }
    
    /**
     * Check if this cache entry has expired
     */
    @JsonIgnore
    public boolean isExpired() {
        Instant created = Instant.parse(createdAt);
        long ageMillis = Instant.now().toEpochMilli() - created.toEpochMilli();
        return ageMillis > ttlMillis;
    }
    
    /**
     * Get age of this cache entry in seconds
     */
    @JsonIgnore
    public long getAgeSeconds() {
        Instant created = Instant.parse(createdAt);
        return (Instant.now().toEpochMilli() - created.toEpochMilli()) / 1000;
    }
    
    /**
     * Calculate freshness score (1.0 = new, 0.0 = expired)
     */
    @JsonIgnore
    public double getFreshnessScore() {
        Instant created = Instant.parse(createdAt);
        long ageMillis = Instant.now().toEpochMilli() - created.toEpochMilli();
        double freshnessRatio = 1.0 - ((double) ageMillis / ttlMillis);
        return Math.max(0.0, Math.min(1.0, freshnessRatio));
    }
    
    /**
     * Record a cache hit
     */
    public void recordHit() {
        this.lastAccessedAt = Instant.now().toString();
        this.hitCount++;
    }
    
    /**
     * Check if locator matches expected attributes (for verification)
     */
    public boolean matchesAttributes(Map<String, String> actualAttributes) {
        if (elementAttributes.isEmpty()) {
            return true; // No verification needed
        }
        
        // Check if key attributes match
        for (Map.Entry<String, String> expected : elementAttributes.entrySet()) {
            String actualValue = actualAttributes.get(expected.getKey());
            if (actualValue == null || !actualValue.equals(expected.getValue())) {
                return false;
            }
        }
        
        return true;
    }
    
    // Getters and setters
    public String getElementKey() { return elementKey; }
    public void setElementKey(String elementKey) { this.elementKey = elementKey; }
    
    public String getSelector() { return selector; }
    public void setSelector(String selector) { this.selector = selector; }
    
    public String getLocatorStrategy() { return locatorStrategy; }
    public void setLocatorStrategy(String locatorStrategy) { this.locatorStrategy = locatorStrategy; }
    
    public String getPageUrl() { return pageUrl; }
    public void setPageUrl(String pageUrl) { this.pageUrl = pageUrl; }
    
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    
    /**
     * Get createdAt as Instant (helper method, not serialized)
     */
    @JsonIgnore
    public Instant getCreatedAtInstant() { 
        return createdAt != null ? Instant.parse(createdAt) : Instant.now(); 
    }
    
    public String getLastAccessedAt() { return lastAccessedAt; }
    public void setLastAccessedAt(String lastAccessedAt) { this.lastAccessedAt = lastAccessedAt; }
    
    public long getTtlMillis() { return ttlMillis; }
    public void setTtlMillis(long ttlMillis) { this.ttlMillis = ttlMillis; }
    
    public int getHitCount() { return hitCount; }
    public void setHitCount(int hitCount) { this.hitCount = hitCount; }
    
    public Map<String, String> getElementAttributes() { return elementAttributes; }
    public void setElementAttributes(Map<String, String> elementAttributes) { 
        this.elementAttributes = elementAttributes; 
    }
    
    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
    
    public Map<String, String> getAllLocators() { return allLocators; }
    public void setAllLocators(Map<String, String> allLocators) { this.allLocators = allLocators; }
    
    /**
     * Add an alternative locator strategy
     */
    public void addLocatorStrategy(String strategy, String selectorValue) {
        if (this.allLocators == null) {
            this.allLocators = new HashMap<>();
        }
        this.allLocators.put(strategy, selectorValue);
    }
    
    /**
     * Get all available selectors in priority order
     * Priority: id > data-testid > name > aria-label > css > xpath
     */
    public Map<String, String> getOrderedLocators() {
        Map<String, String> ordered = new HashMap<>();
        String[] priorityOrder = {"id", "data-testid", "data-test", "data-cy", "name", 
                                 "aria-label", "role", "placeholder", "text", "css", "xpath"};
        
        for (String strategy : priorityOrder) {
            if (allLocators != null && allLocators.containsKey(strategy)) {
                ordered.put(strategy, allLocators.get(strategy));
            }
        }
        
        return ordered;
    }
    
    @Override
    public String toString() {
        return String.format(
            "CachedLocator{key='%s', selector='%s', strategy='%s', hits=%d, age=%ds, freshness=%.2f}",
            elementKey, selector, locatorStrategy, hitCount, getAgeSeconds(), getFreshnessScore()
        );
    }
}
