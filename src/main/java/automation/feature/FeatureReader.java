package automation.feature;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FeatureReader {
    public List<String> readSteps(String featureFilePath) throws Exception {
        List<String> steps = new ArrayList<>();
        List<String> lines = Files.readAllLines(Path.of(featureFilePath));
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("Given") || line.startsWith("When") || line.startsWith("And") || line.startsWith("Then")) {
                steps.add(line);
            }
        }
        return steps;
    }
}
