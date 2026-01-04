Feature: Date Picker Interaction
  As a user
  I want to interact with date pickers
  To select specific dates for forms

  Background:
    Given Navigate to "https://demoqa.com/date-picker"

  Scenario: Native and library date pickers (Generic)
    Given Navigate to "https://demoqa.com/date-picker"
    When Set "05/20/2026" in Select Date
    Then Verify "05/20/2026" appears in Select Date
    
    When Set "30" in Select Date
    And Take the Screenshot
    
    # Testing on a different site with jQuery UI
    Given Navigate to "https://demo.automationtesting.in/Datepicker.html"
    And wait for 2 seconds
    When Set "January 1, 2026" in datepicker1
    Then Verify "01/01/2026" appears in datepicker1
    
    When Set "tomorrow" in datepicker2
    And wait for 2 seconds
    And Take the Screenshot

    When Set "February 1, 2026" in DatePicker Disabled
    Then Verify "01/02/2026" appears in DatePicker Disabled
    And wait for 2 seconds
    When Set "01/03/2026" in DatePicker Enabled
    And Take the Screenshot
    And wait for 2 seconds
    When Set "90" in DatePicker Enabled
    And Take the Screenshot
