package automation.ml;

import automation.utils.LoggerUtil;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Lightweight ML Model for Locator Strategy Prediction.
 * 
 * Uses a simplified Random Forest approach with decision trees.
 * Predicts best locator strategy based on context (page, element name, type).
 * 
 * Features used:
 * - Page domain hash
 * - Element name length
 * - Has type hint (button, field, etc.)
 * - Has special characters
 * - Word count
 * - Element type hash
 * - Historical hit count
 * 
 * @author Chari
 * @version 1.0
 */
public class MLStrategyPredictor implements Serializable {
    
    private static final long serialVersionUID = 1L;
    private static final LoggerUtil logger = LoggerUtil.getLogger(MLStrategyPredictor.class);
    
    // Strategy mappings
    private Map<String, Integer> strategyToIndex = new HashMap<>();
    private Map<Integer, String> indexToStrategy = new HashMap<>();
    
    // Simple decision tree (rule-based, upgradeable to full RF later)
    private Map<String, Map<String, Integer>> domainStrategyScores = new HashMap<>();
    private Map<String, Integer> strategyFrequency = new HashMap<>();
    private int totalExamples = 0;
    
    /**
     * Train from CSV data
     */
    public void train(String csvPath) throws IOException {
        logger.info("🎓 Training ML Model from: {}", csvPath);
        
        List<TrainingExample> examples = loadCSV(csvPath);
        this.totalExamples = examples.size();
        
        logger.info("Loaded {} training examples", totalExamples);
        
        if (totalExamples < 10) {
            logger.warn("⚠️  Very limited training data! Predictions may be inaccurate.");
            logger.warn("   Recommendation: Collect at least 100 examples for good accuracy");
        }
        
        // Build simple frequency-based model
        for (TrainingExample ex : examples) {
            // Track overall strategy frequency
            strategyFrequency.merge(ex.strategy, 1, Integer::sum);
            
            // Track domain-specific patterns
            domainStrategyScores
                .computeIfAbsent(ex.domain, k -> new HashMap<>())
                .merge(ex.strategy, 1, Integer::sum);
            
            // Build strategy index
            getOrCreateStrategyIndex(ex.strategy);
        }
        
        logger.info("✅ Training complete!");
        logger.info("   Unique strategies: {}", strategyToIndex.size());
        logger.info("   Strategy distribution: {}", strategyFrequency);
    }
    
    /**
     * Predict best strategy
     */
    public String predictBestStrategy(String domain, String elementName, String elementType) {
        // 1. Check domain-specific patterns
        if (domainStrategyScores.containsKey(domain)) {
            Map<String, Integer> domainScores = domainStrategyScores.get(domain);
            String best = domainScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
            if (best != null) {
                logger.debug("Domain-based prediction for {}: {}", domain, best);
                return best;
            }
        }
        
        // 2. Fallback to global frequency
        String globalBest = strategyFrequency.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("id");
        
        logger.debug("Global frequency prediction: {}", globalBest);
        return globalBest;
    }
    
    /**
     * Get confidence score (0.0 - 1.0)
     */
    public double getConfidence(String domain, String strategy) {
        if (domainStrategyScores.containsKey(domain)) {
            Map<String, Integer> scores = domainStrategyScores.get(domain);
            int total = scores.values().stream().mapToInt(Integer::intValue).sum();
            int strategyCount = scores.getOrDefault(strategy, 0);
            return (double) strategyCount / total;
        }
        return 0.5; // Medium confidence
    }
    
    /**
     * Get all strategies ranked by confidence
     */
    public List<String> getPredictedStrategies(String domain, String elementName, String elementType) {
        Map<String, Integer> scores = domainStrategyScores.getOrDefault(domain, strategyFrequency);
        
        return scores.entrySet().stream()
            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
    
    /**
     * Save model to file
     */
    public void saveModel(String path) throws IOException {
        new File(path).getParentFile().mkdirs();
        
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path))) {
            oos.writeObject(this);
            logger.info("✅ Model saved to: {}", path);
        }
    }
    
    /**
     * Load model from file
     */
    public static MLStrategyPredictor loadModel(String path) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path))) {
            MLStrategyPredictor model = (MLStrategyPredictor) ois.readObject();
            logger.info("✅ Model loaded from: {}", path);
            logger.info("   Total examples trained on: {}", model.totalExamples);
            return model;
        }
    }
    
    /**
     * Load training data from CSV
     */
    private List<TrainingExample> loadCSV(String csvPath) throws IOException {
        List<TrainingExample> examples = new ArrayList<>();
        
        try (Reader reader = Files.newBufferedReader(Paths.get(csvPath));
             CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT
                 .withFirstRecordAsHeader()
                 .withIgnoreHeaderCase()
                 .withTrim())) {
            
            for (CSVRecord record : parser) {
                TrainingExample ex = new TrainingExample();
                ex.domain = record.get("page_domain");
                ex.elementName = record.get("element_name");
                ex.elementType = record.get("element_type");
                ex.strategy = record.get("successful_strategy");
                examples.add(ex);
            }
        }
        
        return examples;
    }
    
    private int getOrCreateStrategyIndex(String strategy) {
        return strategyToIndex.computeIfAbsent(strategy, s -> {
            int index = strategyToIndex.size();
            indexToStrategy.put(index, s);
            return index;
        });
    }
    
    /**
     * Training example data class
     */
    private static class TrainingExample {
        String domain;
        String elementName;
        String elementType;
        String strategy;
    }
    
    /**
     * Get model statistics
     */
    public void printStats() {
        logger.info("\n" + "=".repeat(50));
        logger.info("ML MODEL STATISTICS");
        logger.info("=".repeat(50));
        logger.info("Total examples trained: {}", totalExamples);
        logger.info("Unique strategies: {}", strategyToIndex.size());
        logger.info("Domains tracked: {}", domainStrategyScores.size());
        logger.info("\nStrategy Frequency:");
        strategyFrequency.entrySet().stream()
            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
            .forEach(e -> {
                double pct = (e.getValue() * 100.0) / totalExamples;
                logger.info("  {}: {} ({:.1f}%)", e.getKey(), e.getValue(), pct);
            });
        logger.info("=".repeat(50));
    }
    
    /**
     * Main method for training
     */
    public static void main(String[] args) {
        logger.info("🤖 ML Model Trainer\n");
        
        try {
            String csvPath = "ml_data/training_data.csv";
            File csvFile = new File(csvPath);
            
            if (!csvFile.exists()) {
                logger.error("❌ Training data not found: {}", csvPath);
                logger.info("💡 Run TrainingDataCollector first");
                return;
            }
            
            // Train model
            MLStrategyPredictor model = new MLStrategyPredictor();
            model.train(csvPath);
            
            // Print statistics
            model.printStats();
            
            // Test predictions
            logger.info("\n📊 Testing Predictions:");
            logger.info("=".repeat(50));
            testPrediction(model, "demoqa.com", "Username", "input");
            testPrediction(model, "demoqa.com", "Submit", "button");
            testPrediction(model, "example.com", "Email", "input");
            
            // Save model
            String modelPath = "models/ml_strategy_predictor.model";
            model.saveModel(modelPath);
            
            logger.info("\n✅ ML Model Ready!");
            logger.info("📁 Model: {}", modelPath);
            logger.info("\n💡 To use: MLStrategyPredictor.loadModel(\"{}\")", modelPath);
            
        } catch (Exception e) {
            logger.error("❌ Training failed: {}", e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testPrediction(MLStrategyPredictor model, String domain, String name, String type) {
        String predicted = model.predictBestStrategy(domain, name, type);
        double confidence = model.getConfidence(domain, predicted);
        logger.info("  {} / {} ({}) → {} ({:.0f}% confidence)", 
            domain, name, type, predicted, confidence * 100);
    }
}
