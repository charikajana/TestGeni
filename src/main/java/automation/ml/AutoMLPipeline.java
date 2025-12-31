package automation.ml;

import automation.utils.LoggerUtil;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Automated ML training pipeline with smart retraining logic.
 * 
 * Features:
 * - Checks if retraining is needed
 * - Only retrains when significant new data is available
 * - Can be scheduled or run manually
 * - Tracks last training timestamp
 * 
 * Usage:
 * 1. Run after test suite: mvn exec:java -Dexec.mainClass="automation.ml.AutoMLPipeline"
 * 2. Or schedule weekly via cron/task scheduler
 * 3. Or trigger on CI/CD after test runs
 * 
 * @author Chari
 * @version 1.0
 */
public class AutoMLPipeline {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(AutoMLPipeline.class);
    
    // Configuration
    private static final int MIN_NEW_EXAMPLES_FOR_RETRAIN = 20; // Retrain when 20+ new examples
    private static final String LAST_TRAIN_INFO = "models/last_training.txt";
    
    public static void main(String[] args) {
        logger.info("Automated ML Pipeline Started\n");
        
        try {
            // Step 1: Check current cache size
            int currentCacheSize = getCurrentCacheSize();
            logger.info("Current cache size: {} entries", currentCacheSize);
            
            // Step 2: Check last training info
            int lastTrainedSize = getLastTrainedSize();
            logger.info("Last trained on: {} entries", lastTrainedSize);
            
            int newExamples = currentCacheSize - lastTrainedSize;
            logger.info("New examples since last training: {}", newExamples);
            
            // Step 3: Decide if retraining is needed
            if (newExamples >= MIN_NEW_EXAMPLES_FOR_RETRAIN) {
                logger.info("\nRetraining recommended ({} new examples)", newExamples);
                logger.info("Starting automatic retraining...\n");
                
                // Step 4: Collect training data
                logger.info("Step 1/2: Collecting training data...");
                TrainingDataCollector collector = new TrainingDataCollector();
                var examples = collector.loadFromCache("CacheLocatorRepository/locator_cache.json");
                collector.printStatistics(examples);
                collector.exportToCSV(examples, "ml_data/training_data.csv");
                
                // Step 5: Train ML model
                logger.info("\nStep 2/2: Training ML model...");
                MLStrategyPredictor model = new MLStrategyPredictor();
                model.train("ml_data/training_data.csv");
                model.printStats();
                model.saveModel("models/ml_strategy_predictor.json");
                
                // Step 6: Save training info
                saveTrainingInfo(currentCacheSize);
                
                logger.info("\nAutomated ML Training Complete!");
                logger.info("   Trained on: {} examples", currentCacheSize);
                logger.info("   Model saved: models/ml_strategy_predictor.json");
                
            } else {
                logger.info("\nRetraining skipped (only {} new examples)", newExamples);
                logger.info("   Minimum required: {} new examples", MIN_NEW_EXAMPLES_FOR_RETRAIN);
                logger.info("   Current model is still good!");
                logger.info("\nRun {} more tests to trigger retraining", 
                    MIN_NEW_EXAMPLES_FOR_RETRAIN - newExamples);
            }
            
        } catch (Exception e) {
            logger.error("Auto ML Pipeline failed: {}", e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get current cache size from locator_cache.json
     */
    private static int getCurrentCacheSize() {
        try {
            String cacheContent = Files.readString(Paths.get("CacheLocatorRepository/locator_cache.json"));
            // Simple parsing - look for "totalEntries"
            int startIndex = cacheContent.indexOf("\"totalEntries\"");
            if (startIndex > 0) {
                int colonIndex = cacheContent.indexOf(":", startIndex);
                int commaIndex = cacheContent.indexOf(",", colonIndex);
                String sizeStr = cacheContent.substring(colonIndex + 1, commaIndex).trim();
                return Integer.parseInt(sizeStr);
            }
        } catch (Exception e) {
            logger.warn("Could not read cache size: {}", e.getMessage());
        }
        return 0;
    }
    
    /**
     * Get size from last training
     */
    private static int getLastTrainedSize() {
        try {
            File infoFile = new File(LAST_TRAIN_INFO);
            if (infoFile.exists()) {
                String content = Files.readString(infoFile.toPath());
                return Integer.parseInt(content.trim());
            }
        } catch (Exception e) {
            logger.debug("No previous training info found");
        }
        return 0; // First time training
    }
    
    /**
     * Save training info for next run
     */
    private static void saveTrainingInfo(int size) {
        try {
            new File("models").mkdirs();
            Files.writeString(Paths.get(LAST_TRAIN_INFO), String.valueOf(size));
            logger.debug("Saved training info: {} examples", size);
        } catch (Exception e) {
            logger.warn("Could not save training info: {}", e.getMessage());
        }
    }
}
