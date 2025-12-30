Feature: Comprehensive State Verification
  
  Background:
    Given Open the browser and go to "https://demoqa.com/radio-button"
    And wait for page load

  Scenario: Comprehensive Enable Variations
    # 1. Standard Enabled phrasings
    Then Verify Yes radio is enabled
    And Verify Yes isEnabled
    And Verify Yes is active
    And Verify Yes is clickable
    And Verify Yes is interactive
    And Verify Yes should be enabled
    
    # 2. Negative phrasing for Enabled
    And Verify Yes is not disabled

  Scenario: Comprehensive Disable Variations
    # No radio button is permanently disabled
    Then Verify No radio is disabled
    And Verify No isDisabled
    And Verify No is greyed out
    And Verify No is grayed out
    And Verify No is inactive
    And Verify No is read-only
    And Verify No is readonly
    And Verify No is restricted
    And Verify No button should be disabled
    
    # Negative phrasing for Disabled
    And Verify No button is not enabled

  Scenario: Comprehensive Selection and Checked Variations
    # 1. Initial unchecked state
    Then Verify Yes radio is not selected
    And Verify Yes is not checked
    And Verify Yes is unchecked
    And Verify Yes is off
    And Verify Yes is not chosen
    And Verify Yes should not be selected
    
    # 2. Checked state after click
    When click "Yes"
    Then Verify Yes radio is selected
    And Verify Yes is checked
    And Verify Yes is on
    And Verify Yes is chosen
    And Verify Yes should be selected

  Scenario: Row-Scoped State Verifications (Enabled/Disabled)
    When navigate to "https://demoqa.com/webtables"
    And wait for page load
    
    # 1. Enabled in row
    Then Verify Edit Icon is enabled in row where "First Name" is "Cierra"
    And Verify Delete Icon is active in row where "Last Name" is "Vega"
    And Verify Edit Icon is clickable in row where "Email" is "cierra@example.com"
    And Verify Delete Icon should be enabled in row where "Age" is "45"
    
    # 2. Negative phrasing in row
    And Verify Edit Icon is not disabled in row where "First Name" is "Alden"

  Scenario: Row-Scoped Selection Verifications (Negative checks)
    When navigate to "https://demoqa.com/webtables"
    And wait for page load
    
    # These rows/buttons are NOT selected
    Then Verify Edit Icon is not selected in row where "First Name" is "Cierra"
    And Verify Delete Icon is not checked in row where "First Name" is "Alden"
    And Verify Edit Icon is unchecked in row where "Email" is "kierra@example.com"
