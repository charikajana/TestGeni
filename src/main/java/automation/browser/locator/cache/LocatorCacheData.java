package automation.browser.locator.cache;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Root data structure for JSON serialization of locator cache.
 * Contains metadata and all cached locators.
 * 
 * @author Chari
 * @version 2.0
 */
public class LocatorCacheData {
    
    @JsonProperty("version")
    private String version;
    
    @JsonProperty("lastSaved")
    private String lastSaved;
    
    @JsonProperty("totalEntries")
    private int totalEntries;
    
    @JsonProperty("locators")
    private Map<String, CachedLocator> locators;
    
    /**
     * Default constructor
     */
    public LocatorCacheData() {
        this.locators = new ConcurrentHashMap<>();
    }
    
    // Getters and setters
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    
    public String getLastSaved() { return lastSaved; }
    public void setLastSaved(String lastSaved) { this.lastSaved = lastSaved; }
    
    public int getTotalEntries() { return totalEntries; }
    public void setTotalEntries(int totalEntries) { this.totalEntries = totalEntries; }
    
    public Map<String, CachedLocator> getLocators() { return locators; }
    public void setLocators(Map<String, CachedLocator> locators) { this.locators = locators; }
}
