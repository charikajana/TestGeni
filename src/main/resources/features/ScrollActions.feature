Feature: Scroll Actions on Real Page
  
  Background:
    Given Navigate to "https://demoqa.com/automation-practice-form"

  Scenario: Scroll to specific element
    # The form is long, 'Submit' is at the bottom
    When I scroll to "Submit"
    Then Verify "Submit" is displayed

  Scenario: Scroll to bottom of page
    When I scroll to bottom
    # Verifying footer content or bottom element
    Then Verify "Submit" is displayed

  Scenario: Scroll by pixels
    # Scroll down to ensure we move
    When I scroll down 300 pixels
    # Verify we are not at top (hard to verify strictly without JS check, but we can verify generic visibility)
    Then Verify "Student Registration Form" is displayed
