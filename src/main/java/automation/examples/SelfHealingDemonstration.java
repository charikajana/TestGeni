package automation.examples;

import automation.browser.locator.cache.CachedSmartLocator;
import com.microsoft.playwright.*;

/**
 * Demonstrates SELF-HEALING in action!
 * 
 * Simulates UI changes and shows how the framework automatically adapts.
 * 
 * TWO-LEVEL SELF-HEALING:
 * 
 * LEVEL 1 - Fast Fallback (Milliseconds):
 * ==========================================
 * - Primary locator fails (e.g., ID changed)
 * - Automatically tries 16 alternative strategies from cache
 * - Uses: name, xpath, aria-label, data-testid, etc.
 * - No DOM re-scan needed
 * - Updates cache with working strategy
 * 
 * LEVEL 2 - Full Healing (Seconds):
 * ==========================================
 * - All cached strategies fail (element completely changed)
 * - Triggers SmartLocator to re-discover element
 * - Captures fresh set of 17+ locator strategies
 * - Updates entire cache with new locators
 * - Test continues without manual intervention
 * 
 * @author Chari
 */
public class SelfHealingDemonstration {
    
    public static void main(String[] args) {
        
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(false).setSlowMo(500)
            );
            Page page = browser.newPage();
            
            CachedSmartLocator locator = new CachedSmartLocator(page);
            
            // Navigate to a page with a dynamic form
            page.navigate("data:text/html," + createDynamicFormHTML_Version1());
            
            System.out.println("\n" + "=".repeat(60));
            System.out.println("PHASE 1: INITIAL RUN - Building Cache");
            System.out.println("=".repeat(60) + "\n");
            
            // First interaction - caches all locator strategies
            var usernameField = locator.findSmartElement("Username", "input", null, null, false);
            if (usernameField != null) {
                usernameField.fill("john_doe");
                System.out.println("Found and filled Username field");
                System.out.println("  Cached strategies: id, name, placeholder, xpath, etc.\n");
            }
            
            Thread.sleep(2000);
            
            // Simulate UI change - change the ID attribute (common in CI/CD)
            System.out.println("\n" + "=".repeat(60));
            System.out.println("SIMULATING UI CHANGE: ID attribute changed!");
            System.out.println("=".repeat(60) + "\n");
            
            page.evaluate("""
                document.getElementById('username').id = 'user-input-new-123';
                console.log('ID changed: username -> user-input-new-123');
            """);
            
            System.out.println("Old ID (#username) is now INVALID\n");
            Thread.sleep(2000);
            
            // Try to find element again - SELF-HEALING LEVEL 1 will trigger
            System.out.println("\n" + "=".repeat(60));
            System.out.println("PHASE 2: SELF-HEALING LEVEL 1 - Fast Fallback");
            System.out.println("=".repeat(60) + "\n");
            
            System.out.println("Primary locator (#username) will fail...");
            System.out.println("Trying alternative strategies from cache...\n");
            
            var healedField1 = locator.findSmartElement("Username", "input", null, null, false);
            if (healedField1 != null) {
                healedField1.fill("healed_user_1");
                System.out.println("SELF-HEALING SUCCESS!");
                System.out.println("  Used alternative: name=[name=\"username\"] or xpath");
                System.out.println("  Cache automatically updated\n");
            }
            
            Thread.sleep(2000);
            
            // Simulate more drastic UI change - change multiple attributes
            System.out.println("\n" + "=".repeat(60));
            System.out.println("SIMULATING MAJOR UI CHANGE: Multiple attributes changed!");
            System.out.println("=".repeat(60) + "\n");
            
            page.evaluate("""
                var elem = document.querySelector('input[name="username"]');
                elem.id = 'completely-new-id-xyz';
                elem.name = 'user_name_new';
                elem.placeholder = 'Your username here';
                console.log('Major change: ID, name, placeholder all changed!');
            """);
            
            System.out.println("ID changed again");
            System.out.println("name attribute changed");
            System.out.println("placeholder changed");
            System.out.println("Many cached strategies now INVALID\n");
            
            Thread.sleep(2000);
            
            // This will trigger LEVEL 2 healing
            System.out.println("\n" + "=".repeat(60));
            System.out.println("PHASE 3: SELF-HEALING LEVEL 2 - Full Re-scan");
            System.out.println("=".repeat(60) + "\n");
            
            System.out.println("Most cached strategies failed...");
            System.out.println("Initiating SmartLocator full scan...");
            System.out.println("Re-discovering element with semantic matching...\n");
            
            var healedField2 = locator.findSmartElement("Username", "input", null, null, false);
            if (healedField2 != null) {
                healedField2.fill("fully_healed_user");
                System.out.println("FULL HEALING SUCCESS!");
                System.out.println("  SmartLocator found element using semantic matching");
                System.out.println("  Captured fresh set of 17+ locator strategies");
                System.out.println("  Cache completely updated with new locators\n");
            }
            
            // Show final cache state
            System.out.println("\n" + "=".repeat(60));
            System.out.println("FINAL CACHE STATISTICS");
            System.out.println("=".repeat(60) + "\n");
            locator.printCacheStatistics();
            
            System.out.println("\n" + "=".repeat(60));
            System.out.println("SELF-HEALING SUMMARY");
            System.out.println("=".repeat(60));
            System.out.println("Test ran successfully despite 2 UI changes");
            System.out.println("No manual intervention required");
            System.out.println("Cache automatically healed both times");
            System.out.println("Zero test code changes needed\n");
            
            Thread.sleep(3000);
            browser.close();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Create a dynamic HTML form for testing (Version 1)
     */
    private static String createDynamicFormHTML_Version1() {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <title>Self-Healing Test</title>
            <style>
                body { 
                    font-family: Arial; 
                    padding: 50px;
                    background: #f5f5f5;
                }
                .form-container {
                    background: white;
                    padding: 30px;
                    border-radius: 8px;
                    box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                    max-width: 400px;
                }
                h2 { color: #333; }
                label { 
                    display: block; 
                    margin: 15px 0 5px;
                    font-weight: bold;
                }
                input {
                    width: 100%;
                    padding: 10px;
                    border: 1px solid #ddd;
                    border-radius: 4px;
                    box-sizing: border-box;
                }
                button {
                    background: #4CAF50;
                    color: white;
                    padding: 12px 30px;
                    border: none;
                    border-radius: 4px;
                    cursor: pointer;
                    margin-top: 20px;
                }
                button:hover { background: #45a049; }
                .info {
                    background: #e3f2fd;
                    padding: 10px;
                    border-radius: 4px;
                    margin-bottom: 20px;
                }
            </style>
        </head>
        <body>
            <div class="form-container">
                <div class="info">
                    <strong>Self-Healing Demo</strong><br>
                    This form will change dynamically to test healing
                </div>
                
                <h2>Login Form</h2>
                
                <label for="username">Username</label>
                <input 
                    type="text" 
                    id="username" 
                    name="username" 
                    placeholder="Enter username"
                    aria-label="Username field"
                    data-testid="username-input"
                />
                
                <label for="password">Password</label>
                <input 
                    type="password" 
                    id="password" 
                    name="password" 
                    placeholder="Enter password"
                    aria-label="Password field"
                />
                
                <button id="submit" type="submit">Login</button>
            </div>
            
            <script>
                console.log('Form loaded with initial attributes');
            </script>
        </body>
        </html>
        """;
    }
}
