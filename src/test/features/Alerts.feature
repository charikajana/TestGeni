Feature: JavaScript Alert Handling
  Testing alert, confirm, and prompt dialog handling with message verification

  Background:
    Given Navigate to "https://demoqa.com/alerts"

  Scenario: Handle simple JavaScript alert and verify message
    When user Click on alertButton
    Then Verify alert says "You clicked a button"
    
  Scenario: Accept alert with specific message verification
    When user Click on alertButton
    Then Accept alert with message "You clicked a button"
    
  Scenario: Handle alert that appears after delay and verify message
    When user Click on timerAlertButton
    Then Wait 6 seconds
    And Verify alert says "This alert appeared after 5 seconds"
    
  Scenario: Handle confirm dialog - Accept with verification
    When user Click on confirmButton
    Then Verify and accept alert with "Do you confirm action?"
    
  Scenario: Handle confirm dialog - Accept without verification
    When we Click on confirmButton
    Then Accept confirm
    
  Scenario: Handle confirm dialog - Dismiss
    When we Click on confirmButton
    Then Dismiss confirm
    
  Scenario: Handle prompt dialog with text input
    When Click on promtButton
    Then Enter "Automation Test User" in prompt

  Scenario: Handle prompt dialog - Dismiss
    When Click on promtButton
    Then Dismiss prompt
