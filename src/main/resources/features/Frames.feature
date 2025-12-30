Feature: Iframe Handling
  Testing both automatic and explicit iframe handling

  Scenario: Automatic iframe traversal
    Given Navigate to "https://demoqa.com/frames"
    Then Verify "This is a sample page" is displayed

  Scenario: Explicit iframe scoping
    Given Navigate to "https://demoqa.com/frames"
    When In iframe "frame1", Verify "This is a sample page" is displayed

  Scenario: Frame switching persistence
    Given Navigate to "https://demoqa.com/frames"
    When Switch to frame "frame1"
    Then Verify "This is a sample page" is displayed
    And Switch back to main content
    Then Verify "Sample Iframe page" is displayed
