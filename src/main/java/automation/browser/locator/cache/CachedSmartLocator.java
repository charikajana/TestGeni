package automation.browser.locator.cache;

import automation.browser.SmartLocator;
import automation.browser.locator.cache.LocatorCacheManager.CacheStatistics;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.TimeoutError;

import java.util.HashMap;
import java.util.Map;

/**
 * Enhanced SmartLocator with Cache and Self-Healing capabilities.
 * 
 * Features:
 * - Tries cached locators first (FAST path)
 * - Falls back to SmartLocator if cache miss (SAFE path)
 * - Self-heals when cached locators fail
 * - Zero impact on existing functionality
 * - Can be disabled via configuration
 * 
 * Usage:
 *   Replace: SmartLocator locator = new SmartLocator(page);
 *   With:    SmartLocator locator = new CachedSmartLocator(page);
 * 
 * Or use directly:
 *   CachedSmartLocator locator = new CachedSmartLocator(page);
 * 
 * @author Chari
 * @version 2.0
 */
public class CachedSmartLocator extends SmartLocator {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(CachedSmartLocator.class);
    
    private final LocatorCacheManager cacheManager;
    private final Page page;
    
    // Configuration
    private boolean cacheEnabled = true;
    private boolean selfHealingEnabled = true;
    private long defaultTTLMillis = 3600000L; // Default 1 hour
    private int verificationAttempts = 2;
    
    /**
     * Constructor with default cache manager
     */
    public CachedSmartLocator(Page page) {
        super(page);
        this.page = page;
        this.cacheManager = LocatorCacheManager.getInstance();
        logger.debug("CachedSmartLocator initialized (cache: {}, self-healing: {})", 
            cacheEnabled, selfHealingEnabled);
    }
    
    /**
     * Enhanced findSmartElement with cache and self-healing
     * 
     * Algorithm:
     * 1. Generate cache key
     * 2. Try cached locator (FAST path)
     * 3. Verify cached locator works
     * 4. If fails, use SmartLocator (SAFE path) + heal cache
     * 5. Cache the working locator for next time
     */
    @Override
    public Locator findSmartElement(String name, String parsedType, Locator scope, 
                                    String frameAnchor, boolean includeHidden) {
        
        // Generate unique cache key
        String cacheKey = generateCacheKey(name, parsedType, frameAnchor, scope);
        
        // Step 1: Try cache (if enabled and no scope - scoped searches always use SmartLocator)
        if (cacheEnabled && scope == null && cacheManager.isEnabled()) {
            Locator cachedLocator = tryCachedLocator(cacheKey, name, parsedType);
            if (cachedLocator != null) {
                return cachedLocator; // FAST PATH SUCCESS
            }
        }
        
        // Step 2: Cache miss or disabled - use SmartLocator (existing robust logic)
        logger.debug("Using SmartLocator for: '{}'", name);
        Locator locator = super.findSmartElement(name, parsedType, scope, frameAnchor, includeHidden);
        
        // Step 3: Cache the successful locator (if found and caching enabled)
        if (locator != null && cacheEnabled && scope == null && cacheManager.isEnabled()) {
            cacheNewLocator(cacheKey, locator, name, parsedType);
        }
        
        return locator;
    }
    
    /**
     * Try to use cached locator with verification and fallback strategies
     * 
     * @return Working locator or null if cache miss/invalid
     */
    private Locator tryCachedLocator(String cacheKey, String name, String parsedType) {
        CachedLocator cached = cacheManager.getCachedLocator(cacheKey);
        
        if (cached == null) {
            return null; // Cache miss
        }
        
        // Try primary locator first
        Locator locator = page.locator(cached.getSelector()).first();
        
        if (verifyCachedLocator(locator, cached, name)) {
            logger.success("Using CACHED locator: {} (strategy: {}, hits: {})", 
                cached.getSelector(), cached.getLocatorStrategy(), cached.getHitCount());
            
            // Track ML effectiveness
            recordMLSuccess(cached.getLocatorStrategy(), page.url(), name, parsedType);
            
            return locator;
        }
        
        // Primary failed - try alternative strategies (self-healing level 1)
        if (selfHealingEnabled && cached.getAllLocators() != null && cached.getAllLocators().size() > 1) {
            logger.info("Primary locator failed, trying {} alternative strategies...", 
                cached.getAllLocators().size() - 1);
            
            for (Map.Entry<String, String> entry : cached.getOrderedLocators().entrySet()) {
                String strategy = entry.getKey();
                String selector = entry.getValue();
                
                // Skip the primary one we already tried
                if (selector.equals(cached.getSelector())) {
                    continue;
                }
                
                try {
                    Locator altLocator = page.locator(selector).first();
                    if (verifyCachedLocator(altLocator, cached, name)) {
                        logger.success("FALLBACK SUCCESS: Using alternative strategy '{}': {}", 
                            strategy, selector);
                        
                        // Update cache with the working strategy
                        cached.setSelector(selector);
                        cached.setLocatorStrategy(strategy);
                        cacheManager.healLocator(cacheKey, selector, strategy, page.url(), 
                            LocatorExtractor.extractElementAttributes(altLocator));
                        
                        return altLocator;
                    }
                } catch (Exception e) {
                    logger.debug("Alternative strategy '{}' failed: {}", strategy, e.getMessage());
                }
            }
            
            logger.warn("All cached strategies failed for: {}", cached.getElementKey());
        }
        
        // All cached strategies failed - trigger full self-healing (level 2)
        if (selfHealingEnabled) {
            logger.warn("Initiating full self-healing scan for: {}", name);
            return attemptSelfHealing(cacheKey, name, parsedType, cached);
        }
        
        // Self-healing disabled - just invalidate
        cacheManager.invalidateLocator(cacheKey);
        return null;
    }
    
    /**
     * Verify that a cached locator still works
     */
    private boolean verifyCachedLocator(Locator locator, CachedLocator cached, String elementName) {
        try {
            // Quick check: Is element visible and accessible?
            if (locator.count() == 0) {
                logger.debug("Cached locator validation: element not found");
                return false;
            }
            
            // Check if element is visible (with short timeout)
            try {
                locator.first().isVisible();
            } catch (TimeoutError e) {
                logger.debug("Cached locator validation: element not visible");
                return false;
            }
            
            // Strong Validation: Verify core attributes (Tag and Text)
            if (!cached.getElementAttributes().isEmpty()) {
                Map<String, String> actualAttributes = LocatorExtractor.extractElementAttributes(locator);
                
                // 1. Tag must match exactly (e.g., don't match a <p> if we cached a <button>)
                String expectedTag = cached.getElementAttributes().get("tag");
                String actualTag = actualAttributes.get("tag");
                if (expectedTag != null && actualTag != null && !expectedTag.equalsIgnoreCase(actualTag)) {
                    logger.debug("Verification FAILED: Tag mismatch (expected={}, actual={})", expectedTag, actualTag);
                    return false;
                }
                
                // 2. Text must be similar if expected (don't match a random element with empty text)
                String expectedText = cached.getElementAttributes().get("text");
                String actualText = actualAttributes.get("text");
                if (expectedText != null && !expectedText.isEmpty()) {
                    if (actualText == null || (!actualText.contains(expectedText) && !expectedText.contains(actualText))) {
                        logger.debug("Verification FAILED: Text mismatch (expected={}, actual={})", expectedText, actualText);
                        return false;
                    }
                }
                
                // 3. Optional: Map-based attribute verification (existing logic)
                if (!cached.matchesAttributes(actualAttributes)) {
                    logger.debug("Verification FAILED: Attribute mismatch");
                    return false;
                }
            }
            
            logger.debug("Cached locator VERIFIED: {}", cached.getSelector());
            return true;
            
        } catch (Exception e) {
            logger.debug("Cached locator validation failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Attempt self-healing when all cached locators fail
     * Uses SmartLocator to rediscover element and caches all new strategies
     */
    private Locator attemptSelfHealing(String cacheKey, String name, String parsedType, 
                                      CachedLocator oldCached) {
        logger.info("SELF-HEALING FULL SCAN: Rediscovering element '{}'", name);
        
        // Use SmartLocator to find element again
        Locator newLocator = super.findSmartElement(name, parsedType, null, null, false);
        
        if (newLocator != null) {
            // Extract ALL possible locator strategies
            Map<String, String> allNewLocators = LocatorExtractor.extractAllLocators(newLocator);
            
            if (!allNewLocators.isEmpty()) {
                String newSelector = LocatorExtractor.getBestLocator(allNewLocators);
                String newStrategy = LocatorExtractor.getStrategyForSelector(allNewLocators, newSelector);
                Map<String, String> newAttributes = LocatorExtractor.extractElementAttributes(newLocator);
                
                // Heal the cache with new locators
                cacheManager.healLocator(cacheKey, newSelector, newStrategy, page.url(), newAttributes);
                
                // Add all discovered strategies to cache
                CachedLocator healed = cacheManager.getCachedLocator(cacheKey);
                if (healed != null) {
                    allNewLocators.forEach(healed::addLocatorStrategy);
                }
                
                logger.success("SELF-HEALING SUCCESSFUL: '{}' | Old: {} ({}) | New: {} ({}) | Strategies: {}", 
                    name, oldCached.getSelector(), oldCached.getLocatorStrategy(),
                    newSelector, newStrategy, allNewLocators.size());
                
                logger.debug("  New strategies available: {}", String.join(", ", allNewLocators.keySet()));
                
                return newLocator;
            }
        }
        
        logger.error("SELF-HEALING FAILED: Could not rediscover element '{}'", name);
        cacheManager.invalidateLocator(cacheKey);
        return null;
    }
    
    /**
     * Cache a newly discovered locator with ALL possible strategies
     */
    private void cacheNewLocator(String cacheKey, Locator locator, String name, String parsedType) {
        try {
            // Extract ALL possible locator strategies
            Map<String, String> allLocators = LocatorExtractor.extractAllLocators(locator);
            
            if (allLocators.isEmpty()) {
                logger.warn("No locators extracted for: {}", cacheKey);
                return;
            }
            
            // Get the best (primary) locator
            String primarySelector = LocatorExtractor.getBestLocator(allLocators);
            String primaryStrategy = LocatorExtractor.getStrategyForSelector(allLocators, primarySelector);
            
            // Extract attributes for verification
            Map<String, String> attributes = LocatorExtractor.extractElementAttributes(locator);
            
            // Create cached locator with primary selector
            CachedLocator cached = new CachedLocator(
                cacheKey,
                primarySelector,
                primaryStrategy,
                page.url(),
                defaultTTLMillis,
                attributes
            );
            
            // Add ALL alternative locator strategies
            allLocators.forEach(cached::addLocatorStrategy);
            
            // Cache it
            cacheManager.cacheLocator(
                cacheKey,
                primarySelector,
                primaryStrategy,
                page.url(),
                attributes
            );
            
            logger.info("Cached {} with {} strategies: primary={} ({})", 
                cacheKey, allLocators.size(), primarySelector, primaryStrategy);
            logger.debug("  Available strategies: {}", String.join(", ", allLocators.keySet()));
            
        } catch (Exception e) {
            logger.warn("Failed to cache locator: {}", e.getMessage());
        }
    }
    
    /**
     * Generate unique cache key for an element
     */
    private String generateCacheKey(String name, String parsedType, String frameAnchor, Locator scope) {
        // Format: "PageContext::ElementName::Type"
        // Example: "https://example.com/login::Username::input"
        // Example with frame: "https://example.com/payment::frame:payment-iframe::Card Number::input"
        
        StringBuilder key = new StringBuilder();
        
        // Page context (domain + path, no query params)
        String pageContext = getPageContext();
        key.append(pageContext);
        key.append("::");
        
        // Frame context (if applicable)
        if (frameAnchor != null) {
            key.append("frame:").append(frameAnchor).append("::");
        }
        
        // Element identifier
        key.append(name);
        
        // Element type (if specified)
        if (parsedType != null && !parsedType.isEmpty()) {
            key.append("::").append(parsedType);
        }
        
        // Note: We don't include scope in key because scoped searches bypass cache
        
        return key.toString();
    }
    
    /**
     * Get page context for caching (URL without query params)
     */
    private String getPageContext() {
        try {
            String url = page.url();
            // Remove query parameters and fragments
            return url.split("\\?")[0].split("#")[0];
        } catch (Exception e) {
            return "unknown-page";
        }
    }
    
    
    /**
     * Get cache statistics
     */
    public CacheStatistics getCacheStatistics() {
        return cacheManager.getStatistics();
    }
    
    /**
     * Print cache statistics
     */
    public void printCacheStatistics() {
        CacheStatistics stats = cacheManager.getStatistics();
        logger.info("╔════════════════════════════════════════╗");
        logger.info("║     LOCATOR CACHE STATISTICS           ║");
        logger.info("╠════════════════════════════════════════╣");
        logger.info("║ Total Entries:    {}", stats.getTotalEntries());
        logger.info("║ Total Cache Hits: {}", stats.getTotalHits());
        logger.info("║ Expired Entries:  {}", stats.getExpiredEntries());
        logger.info("║ Avg Age:          {:.1f}s", stats.getAverageAgeSeconds());
        logger.info("╚════════════════════════════════════════╝");
    }
    
    // Configuration methods
    public void setCacheEnabled(boolean enabled) {
        this.cacheEnabled = enabled;
        logger.info("Cache {}", enabled ? "ENABLED" : "DISABLED");
    }
    
    public void setSelfHealingEnabled(boolean enabled) {
        this.selfHealingEnabled = enabled;
        logger.info("Self-healing {}", enabled ? "ENABLED" : "DISABLED");
    }
    
    public boolean isCacheEnabled() {
        return cacheEnabled;
    }
    
    public boolean isSelfHealingEnabled() {
        return selfHealingEnabled;
    }
    
    /**
     * Record ML success for effectiveness tracking
     */
    private void recordMLSuccess(String strategy, String pageUrl, String elementName, String parsedType) {
        try {
            logger.debug("ML SUCCESS: Strategy '{}' used for '{}' on {}", 
                strategy, elementName, extractDomain(pageUrl));
            // This data helps evaluate ML effectiveness
            // Can be extended to send metrics to analytics
        } catch (Exception e) {
            // Silently ignore - metrics tracking is optional
        }
    }
    
    /**
     * Extract domain from URL
     */
    private String extractDomain(String url) {
        try {
            if (url == null || url.isEmpty()) return "unknown";
            String domain = url.replaceAll("^https?://", "");
            return domain.split("/")[0];
        } catch (Exception e) {
            return "unknown";
        }
    }
}
