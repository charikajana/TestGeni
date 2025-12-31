package automation;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

@CucumberOptions(
    features = {
        "src/main/resources/features/AutoComplete.feature",
        "src/main/resources/features/HoverActions.feature",
        "src/main/resources/features/Select.feature"
    },
    plugin = {
        "pretty",
        "html:target/cucumber-reports/cucumber.html",
        "json:target/cucumber-reports/cucumber.json"
    },
    monochrome = true
)
public class QuickTestRunner extends AbstractTestNGCucumberTests {
}
