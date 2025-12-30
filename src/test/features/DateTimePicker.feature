Feature: Date and Time Picker Testing
  Testing date/time selection, calendar navigation, and relative date selection

  Background:
    Given Navigate to "https://demoqa.com/date-picker"

  Scenario: Select specific date from calendar
    When I open date picker for birth date
    And I select "15" from calendar
    And I choose "March" from month dropdown
    And I choose "1990" from year dropdown
    Then Verify date field shows "03/15/1990"

  Scenario: Select relative date - Tomorrow
    When I click on start date field
    And I select tomorrow from calendar
    Then Verify tomorrow's date is displayed
    And Check date is formatted correctly

  Scenario: Select date range
    When I select date range for booking
    And I pick "25/12/2024" as start date
    And I pick "30/12/2024" as end date
    Then Verify "5 nights" duration is calculated
    And Check both dates are highlighted

  Scenario: Navigate calendar months
    When I open date picker
    And I click next month arrow 3 times
    And I select day "10"
    Then Verify selected date is 3 months ahead
    And Check month navigation worked correctly

  Scenario: Select time from time picker
    When I click on time field
    And I select "14" for hours
    And I select "30" for minutes
    And I choose "PM" from meridian
    Then Verify time shows "02:30 PM"

  Scenario: Select date and time together
    When I set appointment date to "01/15/2025"
    And I set appointment time to "10:00 AM"
    Then Verify complete datetime is "01/15/2025 10:00 AM"
    And Check both fields are populated

  Scenario: Clear selected date
    When I select date "12/25/2024"
    And I click clear button on date picker
    Then Verify date field is empty
    And Check calendar resets to current month

  Scenario: Disable past dates selection
    When I open date picker for future events
    And I try to select yesterday's date
    Then Verify past dates are disabled
    And Check I cannot click disabled dates

  Scenario: Quick date selection shortcuts
    When I click "Today" shortcut button
    Then Verify today's date is selected
    When I click "Next Week" shortcut
    Then Verify date is 7 days from today
