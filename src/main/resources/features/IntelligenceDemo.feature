Feature: Intelligence Layer Demo
  Testing the intelligent NLP-based step parsing

  Scenario: Demonstrate Intelligent Parsing
    Given I navigate to "https://demoqa.com/text-box"
    
    # These steps will be parsed by the Intelligence Layer:
    When I enter "John Doe" in full name
    And I type "john@example.com" in email
    And I fill "123 Main St" in current address
    And I press submit button
    
    Then Verify "John Doe" is displayed
    And Check "john@example.com" message is visible

  Scenario: Navigate through Electronics Hover Menu
    Given Navigate to "https://www.flipkart.com/"
    When I move mouse to "Electronics"
    And I point to "MobileAccessory"
    And I click on "MobileFlash"
    Then Verify URL contains "mobile-flashes"
    And Verify "Mobile Flashes" is visible
