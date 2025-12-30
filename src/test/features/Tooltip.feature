Feature: Tooltip Verification
  Testing mouse hover and tooltip capture functionality

  Background:
    Given Navigate to "https://demoqa.com/tool-tips"

  Scenario: Verify input field tooltip
    When Mouse over Hover me to see input field
    And wait for 2 seconds
    Then Take the Screenshot
    And Verify tooltip of Hover me to see contains "You hovered over the text field"

  Scenario: Interactive Tooltip verification
    When Click on Hover me to see button
    Then Hover over Hover me to see button
    And Verify tooltip of Hover me to see contains "You hovered over the Button"  


