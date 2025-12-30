Feature: Hover and Mouse Interactions
  Testing hover menus, tooltips, and nested navigation

  Scenario: Real World - Flipkart Electronics Hover
    Given Navigate to "https://www.flipkart.com/"
    When user places cursor on "Electronics"
    And focus on "Powerbank"
    And I click on "Powerbank"
    Then Verify URL contains "power-banks"

