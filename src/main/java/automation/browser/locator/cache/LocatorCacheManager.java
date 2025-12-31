package automation.browser.locator.cache;

import automation.utils.LoggerUtil;
import automation.utils.SelectorUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages locator cache with JSON persistence and self-healing capabilities.
 * 
 * Features:
 * - Stores successful element locators in JSON format
 * - Automatic cache invalidation when locators fail
 * - Self-healing: retries with SmartLocator and updates cache
 * - Thread-safe operations
 * - Zero impact on existing functionality (optional usage)
 * 
 * @author Chari
 * @version 2.0
 */
public class LocatorCacheManager {
    private static final LoggerUtil logger = LoggerUtil.getLogger(LocatorCacheManager.class);
    
    private static LocatorCacheManager instance;
    private final Map<String, CachedLocator> cache;
    private final ObjectMapper objectMapper;
    private final String cacheFilePath;
    
    // Configuration
    private boolean enabled = true;
    private long defaultTTLMillis = 3600000L; // 1 hour
    private int maxCacheSize = 1000;
    private boolean autoSave = true;
    
    /**
     * Private constructor for singleton pattern
     */
    private LocatorCacheManager() {
        this.cache = new ConcurrentHashMap<>();
        this.objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        
        // Default cache file location
        this.cacheFilePath = "CacheLocatorRepository/locator_cache.json";
        
        // Ensure directory exists
        try {
            File cacheDir = new File("CacheLocatorRepository");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
        } catch (Exception e) {
            logger.debug("Could not create CacheLocatorRepository: {}", e.getMessage());
        }
        
        // Load existing cache
        loadCache();
        
        // Register shutdown hook to save cache
        Runtime.getRuntime().addShutdownHook(new Thread(this::saveCache));
        
        logger.info("LocatorCacheManager initialized with cache file: {}", cacheFilePath);
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized LocatorCacheManager getInstance() {
        if (instance == null) {
            instance = new LocatorCacheManager();
        }
        return instance;
    }
    
    /**
     * Get cached locator for a given element description
     * 
     * @param elementKey Unique key for the element (e.g., "Login Page::Username Field")
     * @return Cached locator if valid, null otherwise
     */
    public CachedLocator getCachedLocator(String elementKey) {
        if (!enabled) {
            logger.debug("Cache is disabled, skipping lookup");
            return null;
        }
        
        CachedLocator cached = cache.get(elementKey);
        
        if (cached == null) {
            logger.debug("Cache MISS: {}", elementKey);
            return null;
        }
        
        // Check if expired
        if (cached.isExpired()) {
            logger.debug("Cache entry EXPIRED: {}", elementKey);
            cache.remove(elementKey);
            if (autoSave) saveCache();
            return null;
        }
        
        // Update access statistics
        cached.recordHit();
        logger.info("Cache HIT: {} (hits: {}, age: {}s)", 
            elementKey, cached.getHitCount(), cached.getAgeSeconds());
        
        return cached;
    }
    
    /**
     * Store a locator in cache
     * 
     * @param elementKey Unique identifier for the element
     * @param selector The CSS/XPath selector that worked
     * @param locatorStrategy Strategy used (id, css, xpath, etc.)
     * @param pageUrl Current page URL for context
     * @param elementAttributes Additional attributes for verification
     */
    public void cacheLocator(String elementKey, String selector, 
                            String locatorStrategy, String pageUrl, 
                            Map<String, String> elementAttributes) {
        if (!enabled) {
            return;
        }
        
        // FILTER: Do not cache locators that look dynamic or unstable
        if (SelectorUtil.isUnstableSelector(selector, locatorStrategy)) {
            logger.debug("Skipping cache for unstable/dynamic locator: {} ({})", selector, locatorStrategy);
            return;
        }

        // Check cache size limit
        if (cache.size() >= maxCacheSize) {
            logger.warn("Cache size limit reached ({}), cleaning old entries", maxCacheSize);
            cleanOldestEntries(maxCacheSize / 4); // Remove 25%
        }
        
        CachedLocator cachedLocator = new CachedLocator(
            elementKey,
            selector,
            locatorStrategy,
            pageUrl,
            defaultTTLMillis,
            elementAttributes
        );
        
        cache.put(elementKey, cachedLocator);
        logger.info("Cached locator: {} -> {} (strategy: {})", 
            elementKey, selector, locatorStrategy);
        
        if (autoSave) {
            saveCache();
        }
    }
    
    /**
     * Invalidate a cached locator (called when it fails)
     * 
     * @param elementKey The element key to invalidate
     */
    public void invalidateLocator(String elementKey) {
        CachedLocator removed = cache.remove(elementKey);
        if (removed != null) {
            logger.warn("INVALIDATED cache entry: {} (was: {})", 
                elementKey, removed.getSelector());
            if (autoSave) saveCache();
        }
    }
    
    /**
     * Update cached locator (self-healing)
     * This is called when an old locator fails and a new one is found
     * 
     * @param elementKey The element key
     * @param newSelector The new working selector
     * @param newStrategy The new strategy used
     * @param pageUrl Current page URL
     * @param elementAttributes Element attributes for verification
     */
    public void healLocator(String elementKey, String newSelector, 
                           String newStrategy, String pageUrl,
                           Map<String, String> elementAttributes) {
        CachedLocator old = cache.get(elementKey);
        
        if (old != null) {
            logger.warn("SELF-HEALING: {} | Old: {} | New: {}", 
                elementKey, old.getSelector(), newSelector);
        } else {
            logger.info("SELF-HEALING: {} | New locator discovered: {}", 
                elementKey, newSelector);
        }
        
        // Update with new locator
        cacheLocator(elementKey, newSelector, newStrategy, pageUrl, elementAttributes);
    }
    
    /**
     * Load cache from JSON file
     */
    public void loadCache() {
        File cacheFile = new File(cacheFilePath);
        
        if (!cacheFile.exists()) {
            logger.info("No existing cache file found, starting fresh");
            return;
        }
        
        try {
            LocatorCacheData data = objectMapper.readValue(cacheFile, LocatorCacheData.class);
            
            if (data.getLocators() != null) {
                cache.clear();
                cache.putAll(data.getLocators());
                
                // Remove expired entries
                int removed = cleanExpiredEntries();
                
                logger.info("Loaded {} locators from cache (removed {} expired)", 
                    cache.size(), removed);
            }
        } catch (IOException e) {
            logger.error("Failed to load cache from file: {}", e.getMessage());
        }
    }
    
    /**
     * Save cache to JSON file
     */
    public void saveCache() {
        try {
            // Ensure directory exists
            Path cachePath = Paths.get(cacheFilePath);
            Files.createDirectories(cachePath.getParent());
            
            // Create cache data structure
            LocatorCacheData data = new LocatorCacheData();
            data.setVersion("2.0");
            data.setLastSaved(Instant.now().toString());
            data.setLocators(cache);
            data.setTotalEntries(cache.size());
            
            // Write to file
            objectMapper.writeValue(new File(cacheFilePath), data);
            
            logger.debug("Cache saved: {} entries", cache.size());
            
        } catch (IOException e) {
            logger.error("Failed to save cache to file: {}", e.getMessage());
        }
    }
    
    /**
     * Clean expired entries from cache
     * 
     * @return Number of entries removed
     */
    public int cleanExpiredEntries() {
        int initialSize = cache.size();
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        int removed = initialSize - cache.size();
        
        if (removed > 0) {
            logger.info("Cleaned {} expired cache entries", removed);
            if (autoSave) saveCache();
        }
        
        return removed;
    }
    
    /**
     * Remove oldest entries to maintain cache size
     * 
     * @param countToRemove Number of entries to remove
     */
    private void cleanOldestEntries(int countToRemove) {
        cache.entrySet().stream()
            .sorted((e1, e2) -> e1.getValue().getCreatedAt().compareTo(e2.getValue().getCreatedAt()))
            .limit(countToRemove)
            .forEach(entry -> {
                cache.remove(entry.getKey());
                logger.debug("Removed old cache entry: {}", entry.getKey());
            });
    }
    
    /**
     * Get cache statistics
     */
    public CacheStatistics getStatistics() {
        long totalHits = cache.values().stream()
            .mapToLong(CachedLocator::getHitCount)
            .sum();
        
        long expiredCount = cache.values().stream()
            .filter(CachedLocator::isExpired)
            .count();
        
        double avgAge = cache.values().stream()
            .mapToLong(CachedLocator::getAgeSeconds)
            .average()
            .orElse(0.0);
        
        return new CacheStatistics(
            cache.size(),
            totalHits,
            expiredCount,
            avgAge
        );
    }
    
    /**
     * Clear entire cache
     */
    public void clearCache() {
        cache.clear();
        logger.info("Cache cleared");
        if (autoSave) saveCache();
    }
    
    // Configuration methods
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        logger.info("Cache {}", enabled ? "ENABLED" : "DISABLED");
    }
    
    public void setDefaultTTL(long ttlMillis) {
        this.defaultTTLMillis = ttlMillis;
        logger.info("Default TTL set to {}ms ({}s)", ttlMillis, ttlMillis / 1000);
    }
    
    public void setMaxCacheSize(int maxSize) {
        this.maxCacheSize = maxSize;
        logger.info("Max cache size set to {}", maxSize);
    }
    
    public void setAutoSave(boolean autoSave) {
        this.autoSave = autoSave;
        logger.info("Auto-save {}", autoSave ? "ENABLED" : "DISABLED");
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Statistics data class
     */
    public static class CacheStatistics {
        private final int totalEntries;
        private final long totalHits;
        private final long expiredEntries;
        private final double averageAgeSeconds;
        
        public CacheStatistics(int totalEntries, long totalHits, 
                              long expiredEntries, double averageAgeSeconds) {
            this.totalEntries = totalEntries;
            this.totalHits = totalHits;
            this.expiredEntries = expiredEntries;
            this.averageAgeSeconds = averageAgeSeconds;
        }
        
        public int getTotalEntries() { return totalEntries; }
        public long getTotalHits() { return totalHits; }
        public long getExpiredEntries() { return expiredEntries; }
        public double getAverageAgeSeconds() { return averageAgeSeconds; }
        
        @Override
        public String toString() {
            return String.format(
                "CacheStats{entries=%d, hits=%d, expired=%d, avgAge=%.1fs}",
                totalEntries, totalHits, expiredEntries, averageAgeSeconds
            );
        }
    }
}
