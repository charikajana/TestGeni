Feature: Checkbox Automation
  Scenario: Interact with Checkboxes
    Given Open the browser and go to "https://demoqa.com"
    When Click on Elements
    And Click Check Box
    And check the Home Check box
    Then I verify Home check Box is selected
    Then Verify "You have selected :" is displayed
    Then take the screenshot
    And uncheck the Home Check box
    Then I verify Home check Box is not selected
    Then Verify "You have selected :" not displayed
    Then take the screenshot