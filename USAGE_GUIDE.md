# TestGeni - Host Framework Integration Guide

**Quick setup guide for integrating TestGeni into your Cucumber/Playwright framework**

---

## 📋 Prerequisites

- Java 11 or higher
- Maven or Gradle
- Existing Cucumber + Playwright framework

---

## ⚙️ Step 1: Add TestGeni Dependency

### **Maven (`pom.xml`)**

```xml
<dependencies>
    <!-- Your existing dependencies -->
    <dependency>
        <groupId>io.cucumber</groupId>
        <artifactId>cucumber-java</artifactId>
        <version>7.14.0</version>
    </dependency>
    
    <dependency>
        <groupId>com.microsoft.playwright</groupId>
        <artifactId>playwright</artifactId>
        <version>1.44.0</version>
    </dependency>
    
    <!-- ✅ Add TestGeni -->
    <dependency>
        <groupId>org.autohelp</groupId>
        <artifactId>TestGeni</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

### **Gradle (`build.gradle`)**

```gradle
dependencies {
    // Your existing dependencies
    implementation 'io.cucumber:cucumber-java:7.14.0'
    implementation 'com.microsoft.playwright:playwright:1.44.0'
    
    // ✅ Add TestGeni
    implementation 'org.autohelp:TestGeni:1.0.0'
}
```

---

## 🔧 Step 2: Configure Hooks

**File:** `src/test/java/hooks/Hooks.java`

```java
package hooks;

import automation.integration.TestGeniAgent;
import automation.integration.CucumberStepContext;
import com.microsoft.playwright.*;
import io.cucumber.java.*;

public class Hooks {
    
    // Playwright instances (managed by YOUR framework)
    private static Playwright playwright;
    private static Browser browser;
    public static Page page;
    
    // ✅ TestGeni instance
    public static TestGeniAgent testGeni;
    
    /**
     * Initialize browser and TestGeni before each scenario
     */
    @Before
    public void setup() {
        // Initialize Playwright (YOUR framework)
        playwright = Playwright.create();
        browser = playwright.chromium().launch(
            new BrowserType.LaunchOptions()
                .setHeadless(false)
                .setSlowMo(100)
        );
        page = browser.newPage();
        page.setDefaultTimeout(30000);
        
        // ✅ Initialize TestGeni with YOUR Page instance
        testGeni = new TestGeniAgent(page);
    }
    
    /**
     * ⭐ Capture current step text for automatic execution
     */
    @BeforeStep
    public void captureStepText(Scenario scenario) {
        try {
            // Extract step text from Cucumber
            String stepText = scenario.getPickle()
                .getSteps()
                .get(scenario.getStepIndex())
                .getText();
            
            // Pass to TestGeni's context
            CucumberStepContext.setCurrentStepText(stepText);
            
        } catch (Exception e) {
            System.err.println("Warning: Could not capture step text - " + e.getMessage());
        }
    }
    
    /**
     * Clear step context after each step
     */
    @AfterStep
    public void clearStepContext() {
        CucumberStepContext.clear();
    }
    
    /**
     * Cleanup after each scenario
     */
    @After
    public void teardown(Scenario scenario) {
        // Screenshot on failure
        if (scenario.isFailed() && page != null) {
            byte[] screenshot = page.screenshot();
            scenario.attach(screenshot, "image/png", "Failed Screenshot");
        }
        
        // Close browser
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }
}
```

---

## 📝 Step 3: Create Step Definitions

**File:** `src/test/java/stepdefs/CommonSteps.java`

```java
package stepdefs;

import hooks.Hooks;
import io.cucumber.java.en.*;
import automation.reporting.StepExecutionReport;

public class CommonSteps {
    
    /**
     * ✨ EASIEST WAY - Returns detailed report for fallback logic
     */
    @When("I enter {string} in {string} field")
    public void enterInField(String value, String field) {
        StepExecutionReport report = Hooks.testGeni.executeCurrentStep();
        
        // Logical Fallback: If TestGeni fails, use your own framework's logic
        if (!"PASSED".equals(report.getStatus())) {
            System.err.println("TestGeni failing, falling back to custom logic for: " + field);
            Hooks.page.locator("input[name='" + field + "']").fill(value);
        }
    }
    
    @When("I click {string}")
    public void clickElement(String elementName) {
        StepExecutionReport report = Hooks.testGeni.executeCurrentStep();
        
        if (!"PASSED".equals(report.getStatus())) {
            // Your fallback logic here
            Hooks.page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(elementName)).click();
        }
    }
    
    @Then("I should see {string}")
    public void shouldSee(String text) {
        StepExecutionReport report = Hooks.testGeni.executeCurrentStep();
        
        if (!"PASSED".equals(report.getStatus())) {
            // Fallback verification
            assertThat(Hooks.page.getByText(text)).isVisible();
        }
    }

    /**
     * 🌟 UNIVERSAL STEP - Catches everything!
     */
    @Given("^(.+)$")
    @When("^(.+)$")
    @Then("^(.+)$")
    public void executeAnyStep(String stepText) {
        StepExecutionReport report = Hooks.testGeni.executeCurrentStep();
        
        if (!"PASSED".equals(report.getStatus())) {
            throw new AssertionError("TestGeni failed and no fallback provided: " + report.getErrorMessage());
        }
    }
}
```

---

## 📊 Parsing the Report (JSON)

The `executeCurrentStep()` method returns a `StepExecutionReport` object that can be converted to JSON using `report.toJson()`.

### **Data you can extract:**
- `report.getStepName()`: Original natural language step.
- `report.getStatus()`: `PASSED`, `FAILED`, or `SKIPPED`.
- `report.getExecutionTime()`: Timestamp of the action.
- `report.getLocatorIdentified().getSelector()`: The actual selector discovered by TestGeni.
- `report.getLocatorIdentified().getElementType()`: Type of element acted upon.
- `report.getValidation().getExpected()`: Expected text (for Verify actions).
- `report.getValidation().getActual()`: Actual text found (for Verify actions).
- `report.getDuration()`: Time taken in milliseconds.

### **Example: Logging JSON for Analytics**
```java
@AfterStep
public void logStepDetails(Scenario scenario) {
    // If you saved the last report in a ThreadLocal
    System.out.println(lastReport.toJson());
}
```

---

## 🎯 Step 4: Write Feature Files

**File:** `src/test/resources/features/Login.feature`

```gherkin
Feature: User Login

  Scenario: Successful login
    Given I navigate to "https://example.com/login"
    When I enter "john@example.com" in "Email" field
    And I enter "password123" in "Password" field
    And I click "Login" button
    Then I should see "Welcome John"
    
  Scenario: Invalid credentials
    Given I navigate to "https://example.com/login"
    When I enter "invalid@example.com" in "Email" field
    And I enter "wrongpass" in "Password" field
    And I click "Login" button
    Then the "Error Message" should be "Invalid credentials"
```

---

## 🚀 Step 5: Run Tests

### **Maven:**
```bash
mvn clean test
```

### **Gradle:**
```bash
gradle clean test
```

### **With TestNG Runner:**

**File:** `src/test/java/runners/TestRunner.java`

```java
package runners;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

@CucumberOptions(
    features = "src/test/resources/features",
    glue = {"hooks", "stepdefs"},
    plugin = {
        "pretty",
        "html:target/cucumber-reports/cucumber.html",
        "json:target/cucumber-reports/cucumber.json"
    },
    tags = "@smoke"
)
public class TestRunner extends AbstractTestNGCucumberTests {
}
```

---

## 📚 Usage Options

### **Option 1: Auto-Execute Current Step** ⭐ **RECOMMENDED**

```java
@When("I enter {string} in {string} field")
public void enterField(String value, String field) {
    StepExecutionReport report = Hooks.testGeni.executeCurrentStep();  // ✅ Returns detailed JSON report
}
```

### **Option 2: Execute with Template**

```java
@When("I enter {string} in {string} field")
public void enterField(String value, String field) {
    Hooks.testGeni.executeTemplate(
        "I enter \"%s\" in \"%s\" field",
        value,
        field
    );
}
```

### **Option 3: Execute Full Step**

```java
@When("^(.+)$")
public void anyStep(String stepText) {
    Hooks.testGeni.execute(stepText);  // stepText is complete
}
```

---

## 🔍 Advanced Features

### **Check if Step is Supported**

```java
@When("^(.+)$")
public void executeStep(String stepText) {
    if (Hooks.testGeni.isSupported(stepText)) {
        Hooks.testGeni.execute(stepText);
    } else {
        // Get suggestions
        System.out.println("Suggestions:");
        Hooks.testGeni.getSuggestions(stepText)
            .forEach(System.out::println);
        
        throw new io.cucumber.java.PendingException(
            "Step not supported: " + stepText
        );
    }
}
```

### **Window/Tab Management**

```java
@When("I switch to new tab")
public void switchToNewTab() {
    // Get new page from Playwright
    Page newPage = Hooks.page.context().pages().get(1);
    
    // Update TestGeni to use new page
    Hooks.testGeni.updatePage(newPage);
    
    // Update static reference
    Hooks.page = newPage;
}
```

### **Get Detailed Execution Report**

```java
@When("I perform complex action")
public void performAction() {
    StepExecutionReport report = 
        Hooks.testGeni.executeWithReport("I perform complex action");
    
    System.out.println("Status: " + report.getStatus());
    System.out.println("Duration: " + report.getDuration() + "ms");
}
```

---

## 📁 Project Structure

```
YourProject/
├── pom.xml                          # Maven config with TestGeni
├── src/
│   ├── test/
│   │   ├── java/
│   │   │   ├── hooks/
│   │   │   │   └── Hooks.java       # ✅ Setup TestGeni here
│   │   │   ├── stepdefs/
│   │   │   │   └── CommonSteps.java # ✅ Use TestGeni here
│   │   │   └── runners/
│   │   │       └── TestRunner.java  # TestNG/JUnit runner
│   │   └── resources/
│   │       └── features/
│   │           └── Login.feature    # Cucumber scenarios
└── config/
    └── locator_cache.json           # ✅ Auto-generated cache
```

---

## ⚙️ Configuration Options

### **Enable/Disable Intelligence Layer**

```java
@Before
public void setup() {
    page = browser.newPage();
    testGeni = new TestGeniAgent(page);
    
    // Optional: Access internal parser to configure
    // (Advanced usage - usually not needed)
}
```

### **Cache Location**

TestGeni auto-creates cache at: `config/locator_cache.json`

To customize, set system property:
```java
System.setProperty("testgeni.cache.dir", "path/to/cache");
```

---

## ✅ Verification

Run this simple test to verify integration:

**File:** `src/test/resources/features/TestGeniVerification.feature`

```gherkin
Feature: Verify TestGeni Integration

  Scenario: TestGeni is working
    Given I navigate to "https://demoqa.com/text-box"
    When I enter "TestGeni" in "Full Name" field
    And I enter "testgeni@example.com" in "Email" field
    And I click "Submit" button
    Then I should see "TestGeni"
```

Run and check console for:
```
✓ TestGeniAgent initialized with external Page instance
✓ Smart automation succeeded: I enter "TestGeni" in "Full Name" field
```

---

## 🎉 Benefits

✅ **No hardcoded selectors** - Smart locators find elements  
✅ **Self-healing tests** - Auto-recovery from UI changes  
✅ **100x faster** - 5ms (cached) vs 500ms (standard)  
✅ **Less maintenance** - Automatic locator updates  
✅ **Natural language** - Plain English in feature files  

---

## 🆘 Troubleshooting

### **Issue: "No step text available"**

**Cause:** `@BeforeStep` hook not configured

**Fix:** Add `@BeforeStep` hook in `Hooks.java` (see Step 2)

---

### **Issue: TestGeni not found**

**Cause:** Dependency not installed

**Fix:**
```bash
# Install TestGeni locally first
cd /path/to/TestGeni
mvn clean install

# Then build your project
cd /path/to/YourProject
mvn clean test
```

---

### **Issue: Page instance is null**

**Cause:** TestGeni initialized before Page

**Fix:** Initialize in `@Before` hook AFTER creating Page:
```java
@Before
public void setup() {
    page = browser.newPage();  // ✅ Create page first
    testGeni = new TestGeniAgent(page);  // ✅ Then TestGeni
}
```

---

## 📞 Support

- GitHub: [ChariKajana/TestGeni](https://github.com/ChariKajana/TestGeni)
- Issues: [Report a bug](https://github.com/ChariKajana/TestGeni/issues)

---

**Happy Testing with TestGeni!** 🚀
