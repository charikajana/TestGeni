Feature: RadioButton Automation
  Scenario: Interact with RadioButton
    Given Open the browser and go to "https://demoqa.com"
    When Click "Elements"
    And Click Radio Button
    Then Validate Yes Radio Button is enabled
    Then Validate Impressive Radio Button is enabled
    Then Validate No Radio Button is disabled
    And Click Yes Radio Button
    Then Validate "You have selected Yes" message/text should be visible
    And take the screenshot
    And Click Impressive Radio Button
    Then Validate "You have selected Impressive" message/text should be visible
    And take the screenshot
