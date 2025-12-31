package automation.examples;

import automation.browser.locator.cache.CachedSmartLocator;
import automation.browser.locator.cache.LocatorCacheManager;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

/**
 * Example demonstrating Cache-Based Locator Strategy with Self-Healing
 * 
 * How it works:
 * 1. First run: Captures 17+ locator strategies for each element and stores in JSON
 * 2. Subsequent runs: Uses cached locators (FAST - milliseconds instead of seconds)
 * 3. If cached locator fails: Tries alternative strategies automatically (Self-Healing Level 1)
 * 4. If all strategies fail: Re-scans with SmartLocator and updates cache (Self-Healing Level 2)
 * 
 * @author Chari
 */
public class CacheAndSelfHealingExample {
    
    public static void main(String[] args) {
        
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
            Page page = browser.newPage();
            
            // Use CachedSmartLocator instead of SmartLocator
            CachedSmartLocator locator = new CachedSmartLocator(page);
            
            // Navigate to test page
            page.navigate("https://demoqa.com/text-box");
            
            System.out.println("\n========================================");
            System.out.println("FIRST RUN - Building Cache");
            System.out.println("========================================\n");
            
            // First interaction - will cache ALL possible locators
            var nameField = locator.findSmartElement("Full Name", "input", null, null, false);
            if (nameField != null) {
                nameField.fill("John Doe");
                System.out.println("✓ Filled Full Name field");
            }
            
            var emailField = locator.findSmartElement("Email", "input", null, null, false);
            if (emailField != null) {
                emailField.fill("john@example.com");
                System.out.println("✓ Filled Email field");
            }
            
            var submitButton = locator.findSmartElement("Submit", "button", null, null, false);
            if (submitButton != null) {
                submitButton.click();
                System.out.println("✓ Clicked Submit button");
            }
            
            // Print cache statistics
            System.out.println("\n========================================");
            System.out.println("CACHE STATISTICS");
            System.out.println("========================================");
            locator.printCacheStatistics();
            
            // Simulate page reload to test cache
            System.out.println("\n========================================");
            System.out.println("SECOND RUN - Using Cached Locators");
            System.out.println("========================================\n");
            
            page.reload();
            
            // This time, locators will be retrieved from cache (FAST!)
            var cachedNameField = locator.findSmartElement("Full Name", "input", null, null, false);
            if (cachedNameField != null) {
                cachedNameField.fill("Jane Smith");
                System.out.println("✓ Filled Full Name (from cache)");
            }
            
            var cachedEmailField = locator.findSmartElement("Email", "input", null, null, false);
            if (cachedEmailField != null) {
                cachedEmailField.fill("jane@example.com");
                System.out.println("✓ Filled Email (from cache)");
            }
            
            // Print updated statistics
            System.out.println("\n========================================");
            System.out.println("UPDATED CACHE STATISTICS");
            System.out.println("========================================");
            locator.printCacheStatistics();
            
            // View the JSON cache file
            System.out.println("\n========================================");
            System.out.println("Cache File Location: CacheLocatorRepository/locator_cache.json");
            System.out.println("========================================");
            
            // Manual save
            LocatorCacheManager.getInstance().saveCache();
            System.out.println("\n✓ Cache saved successfully!");
            
            Thread.sleep(3000);
            browser.close();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
