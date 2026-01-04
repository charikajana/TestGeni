Feature: Expandable Navigation Menu Test
  As a user
  I want to expand menu sections and click items
  To navigate through hierarchical menus

  Scenario: Exapand Collapse Example
    Given Navigate to "https://dequeuniversity.com/library/aria/expand-collapse"

    # Expand the menu hierarchy step by step
    And wait for 5 seconds
    When expand HTML Source Code
    And wait for 5 seconds
    Then Verify "This component has been adapted from an example provided by the W3C," in displayed
    And collapse HTML Source Code


    When expand JavaScript Source Code
    And wait for 5 seconds
    Then Verify "typeof TabExpander ===" in displayed
    And collapse JavaScript Source Code


    When expand CSS Source Code
    And wait for 5 seconds
    Then Verify "This component has been adapted from an example provided by the W3C" in displayed
    And collapse CSS Source Code


  Scenario: Expand nested menu and navigate to Instructions page
    Given Navigate to "https://testpages.eviltester.com/"

    # Expand the menu hierarchy step by step
    When expand Pages
    And wait for 5 seconds
    And collapse Pages
    And wait for 5 seconds
    When expand Pages
    And expand Basics
    And wait for 5 seconds
    And collapse Basics
    And wait for 5 seconds
    When expand Basics
    And expand Alerts - JavaScript

    # Click on the target item and handle new window
    Then click on JavaScript Alerts - Instructions
    And wait for 2 seconds
    # Verify the page loaded correctly
    Then Verify "JavaScript Alerts - Instructions" is displayed

