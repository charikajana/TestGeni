package automation.ml;

import automation.browser.locator.cache.CachedLocator;
import automation.browser.locator.cache.LocatorCacheData;
import automation.browser.locator.cache.LocatorCacheManager;
import automation.utils.LoggerUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Collects training data from locator cache for ML model training.
 * 
 * Extracts features from successful element matches and exports to CSV format.
 * 
 * Features extracted:
 * - Page URL patterns (domain, path)
 * - Element name characteristics (length, has type hint, keywords)
 * - Element type
 * - Successful locator strategy
 * - Execution metadata (hit count, age)
 * 
 * @author Chari
 * @version 1.0
 */
public class TrainingDataCollector {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(TrainingDataCollector.class);
    
    /**
     * Training example for ML model
     */
    public static class TrainingExample {
        // Features
        public String pageUrl;
        public String pageDomain;
        public String elementName;
        public String elementType;
        public int nameLength;
        public boolean hasTypeHint;
        public boolean hasSpecialChars;
        public int wordCount;
        
        // Label (what we want to predict)
        public String successfulStrategy;
        public double confidenceScore;
        
        // Metadata
        public int hitCount;
        public long ageSeconds;
        
        public TrainingExample() {}
    }
    
    /**
     * Load training data from locator cache
     */
    public List<TrainingExample> loadFromCache(String cacheFilePath) {
        List<TrainingExample> examples = new ArrayList<>();
        
        try {
            File cacheFile = new File(cacheFilePath);
            if (!cacheFile.exists()) {
                logger.warn("Cache file not found: {}", cacheFilePath);
                return examples;
            }
            
            ObjectMapper mapper = new ObjectMapper();
            LocatorCacheData cacheData = mapper.readValue(cacheFile, LocatorCacheData.class);
            
            logger.info("Loading training data from {} entries", cacheData.getTotalEntries());
            
            for (Map.Entry<String, CachedLocator> entry : cacheData.getLocators().entrySet()) {
                String cacheKey = entry.getKey();
                CachedLocator cached = entry.getValue();
                
                TrainingExample example = extractFeatures(cacheKey, cached);
                examples.add(example);
            }
            
            logger.info("Collected {} training examples", examples.size());
            
        } catch (IOException e) {
            logger.error("Failed to load cache: {}", e.getMessage());
        }
        
        return examples;
    }
    
    /**
     * Extract features from a cached locator entry
     */
    private TrainingExample extractFeatures(String cacheKey, CachedLocator cached) {
        TrainingExample example = new TrainingExample();
        
        // Parse cache key: "pageUrl::elementName::elementType"
        String[] parts = cacheKey.split("::");
        
        example.pageUrl = parts[0];
        example.pageDomain = extractDomain(parts[0]);
        example.elementName = parts.length > 1 ? parts[1] : "";
        example.elementType = parts.length > 2 ? parts[2] : "";
        
        // Name-based features
        example.nameLength = example.elementName.length();
        example.hasTypeHint = hasTypeHint(example.elementName);
        example.hasSpecialChars = example.elementName.matches(".*[^a-zA-Z0-9\\s].*");
        example.wordCount = example.elementName.split("\\s+").length;
        
        // Label
        example.successfulStrategy = cached.getLocatorStrategy();
        example.confidenceScore = calculateConfidence(cached);
        
        // Metadata
        example.hitCount = cached.getHitCount();
        example.ageSeconds = cached.getAgeSeconds();
        
        return example;
    }
    
    /**
     * Extract domain from URL
     */
    private String extractDomain(String url) {
        try {
            // Remove protocol
            String domain = url.replaceAll("^https?://", "");
            // Get domain part only
            domain = domain.split("/")[0];
            return domain;
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    /**
     * Check if element name has type hint (e.g., "Submit button", "Username field")
     */
    private boolean hasTypeHint(String name) {
        String lower = name.toLowerCase();
        return lower.matches(".*(button|field|input|link|checkbox|dropdown|select|textarea|slider|icon|label|menu|tab)$");
    }
    
    /**
     * Calculate confidence score based on hit count and freshness
     */
    private double calculateConfidence(CachedLocator cached) {
        // More hits = higher confidence
        double hitScore = Math.min(cached.getHitCount() / 10.0, 1.0);
        
        // Fresher = higher confidence
        double freshnessScore = cached.getFreshnessScore();
        
        return (hitScore * 0.6) + (freshnessScore * 0.4);
    }
    
    /**
     * Export training data to CSV file
     */
    public void exportToCSV(List<TrainingExample> examples, String outputPath) {
        // Ensure directory exists
        File outputFile = new File(outputPath);
        if (outputFile.getParentFile() != null) {
            outputFile.getParentFile().mkdirs();
        }
        
        try (FileWriter writer = new FileWriter(outputPath);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT
                 .withHeader("page_domain", "element_name", "element_type", 
                            "name_length", "has_type_hint", "has_special_chars", 
                            "word_count", "successful_strategy", "confidence_score",
                            "hit_count", "age_seconds"))) {
            
            for (TrainingExample ex : examples) {
                printer.printRecord(
                    ex.pageDomain,
                    ex.elementName,
                    ex.elementType,
                    ex.nameLength,
                    ex.hasTypeHint ? 1 : 0,
                    ex.hasSpecialChars ? 1 : 0,
                    ex.wordCount,
                    ex.successfulStrategy,
                    String.format("%.2f", ex.confidenceScore),
                    ex.hitCount,
                    ex.ageSeconds
                );
            }
            
            printer.flush();
            logger.info("Exported {} examples to {}", examples.size(), outputPath);
            
        } catch (IOException e) {
            logger.error("Failed to export CSV: {}", e.getMessage());
        }
    }
    
    /**
     * Get strategy distribution statistics
     */
    public void printStatistics(List<TrainingExample> examples) {
        Map<String, Long> strategyCount = new java.util.HashMap<>();
        
        for (TrainingExample ex : examples) {
            strategyCount.merge(ex.successfulStrategy, 1L, Long::sum);
        }
        
        logger.info("\n" + "=".repeat(50));
        logger.info("TRAINING DATA STATISTICS");
        logger.info("=".repeat(50));
        logger.info("Total Examples: {}", examples.size());
        logger.info("\nStrategy Distribution:");
        
        strategyCount.entrySet().stream()
            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
            .forEach(entry -> {
                double percentage = (entry.getValue() * 100.0) / examples.size();
                logger.info("  {}: {} ({:.1f}%)", 
                    entry.getKey(), entry.getValue(), percentage);
            });
        
        logger.info("=".repeat(50) + "\n");
    }
    
    /**
     * Main method to run data collection
     */
    public static void main(String[] args) {
        logger.info("🤖 Starting ML Training Data Collection...\n");
        
        TrainingDataCollector collector = new TrainingDataCollector();
        
        // Load from cache
        String cacheFile = "CacheLocatorRepository/locator_cache.json";
        List<TrainingExample> examples = collector.loadFromCache(cacheFile);
        
        if (examples.isEmpty()) {
            logger.warn("No training data found. Run some tests first to populate the cache!");
            return;
        }
        
        // Print statistics
        collector.printStatistics(examples);
        
        // Export to CSV
        String outputFile = "ml_data/training_data.csv";
        new File("ml_data").mkdirs(); // Create directory if needed
        collector.exportToCSV(examples, outputFile);
        
        logger.info("✅ Training data collected successfully!");
        logger.info("📁 Output: {}", outputFile);
        logger.info("\n💡 Next step: Run TrainStrategyModel to train the ML model");
    }
}
