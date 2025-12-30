package automation.ml;

import automation.browser.locator.cache.LocatorCacheManager;
import automation.utils.LoggerUtil;

import java.io.File;

/**
 * Automated ML training data collection pipeline.
 * 
 * Runs multiple test scenarios to collect diverse training examples.
 * Goal: Collect 100+ examples for ML model training.
 * 
 * Strategy:
 * 1. Run existing feature files
 * 2. Collect locator cache data
 * 3. Export to training_data.csv
 * 4. When threshold reached (100+ examples) → train ML model
 * 
 * @author Chari
 * @version 1.0
 */
public class MLDataPipeline {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(MLDataPipeline.class);
    private static final int MIN_EXAMPLES_FOR_ML = 100;
    
    public static void main(String[] args) {
        logger.info("🤖 ML Training Data Pipeline Started\n");
        
        // Check current data status
        TrainingDataCollector collector = new TrainingDataCollector();
        var examples = collector.loadFromCache("config/locator_cache.json");
        
        logger.info("Current training examples: {}", examples.size());
        
        if (examples.size() < MIN_EXAMPLES_FOR_ML) {
            logger.warn("⚠️  Need {} more examples for ML training", 
                MIN_EXAMPLES_FOR_ML - examples.size());
            logger.info("\n📝 To collect more data:");
            logger.info("  1. Run: mvn test (runs all feature files)");
            logger.info("  2. Or run: AllFeaturesTestRunner");
            logger.info("  3. Each test adds 5-15 examples to cache");
            logger.info("  4. After ~10 tests, you'll have 100+ examples");
            logger.info("\n💡 Then re-run this pipeline to train the ML model");
            
        } else {
            logger.info("✅ Sufficient data collected! ({} examples)", examples.size());
            logger.info("📊 Proceeding with ML model training...\n");
            
            // Export latest data
            collector.printStatistics(examples);
            collector.exportToCSV(examples, "ml_data/training_data.csv");
            
            // Train ML model (placeholder for now - will implement full RandomForest)
            logger.info("\n🎓 Training RandomForest model...");
            logger.info("  (Full implementation coming next)");
            
            logger.info("\n✅ ML Pipeline Complete!");
        }
        
        // Always export current data
        if (!examples.isEmpty()) {
            collector.exportToCSV(examples, "ml_data/training_data.csv");
            logger.info("\n📁 Data exported to: ml_data/training_data.csv");
        }
        
        // Clean up old cache entries
        logger.info("\n🧹 Cleaning expired cache entries...");
        int removed = LocatorCacheManager.getInstance().cleanExpiredEntries();
        logger.info("  Removed {} expired entries", removed);
    }
}
