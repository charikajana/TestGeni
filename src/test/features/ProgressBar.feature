Feature: Progress Bar Automation
  This feature tests the management and synchronization of progress bars.

  Scenario: Wait for progress completion
    Given Navigate to "https://demoqa.com/progress-bar"
    When Click on Start
    And Wait for progress bar to reach "50%"
    And Take the Screenshot
    And Wait for progress bar to reach "100%"
    And Take the Screenshot
    Then Verify "Reset" button is displayed
    When Click on "Reset"

  Scenario: Targeted Progress Monitoring
    Given Navigate to "https://demoqa.com/progress-bar"
    When Click on Start button
    And Monitor the progress until reach "25%"
    Then Take the Screenshot
