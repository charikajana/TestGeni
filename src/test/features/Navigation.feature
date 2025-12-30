Feature: Navigation Feature
  Background:
     Given Navigate to "https://demoqa.com"

  Scenario: Open link in new tab
    When I navigate to "https://demoqa.com/links"
    
    # Use the new robust action: Click AND Switch in one atomic step using waitForPopup logic
    When I click on Home link and switch to new window
    And wait for 10 seconds
    
    Then Verify URL is exactly "https://demoqa.com/"
    
    # Continue reliably in new window
    When I navigate to "https://demoqa.com/checkbox"
    Then Verify URL contains "checkbox"
    
    When I click Home checkbox
    Then Wait for 3 seconds
    Then Verify window count is 2
    When I close new window 
    And switch to parent window
    Then Verify URL is exactly "https://demoqa.com/links"
    And wait for 10 seconds
    And click on Text Box
    And wait for 10 seconds
    And click on Links
    When I click on Home link and switch to new window
    And wait for 10 seconds
    Then Verify URL is exactly "https://demoqa.com/"
    Then Wait for 3 seconds
    Then Verify window count is 2
    When I close new window
    And switch to parent window
    Then Verify URL is exactly "https://demoqa.com/links"
    And click on Text Box
    And wait for 10 seconds
    And click on Links
    When I click on Home link and switch to new window
    And wait for 10 seconds
    Then Verify URL is exactly "https://demoqa.com/"
    Then Wait for 3 seconds
    Then Verify window count is 2
    When I close new window
    Then Wait for 20 seconds
