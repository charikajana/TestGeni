Feature: Test Individual Fill Actions
  Testing individual fill actions to verify field matching
  
  Background:
    Given Navigate to "https://demoqa.com/automation-practice-form"

  Scenario: Fill fields individually
    When Enter "John" in First Name
    Then Wait for 1 seconds
    When Enter "Doe" in Last Name  
    Then Wait for 1 seconds
    When Enter "john.doe@test.com" in Email
    Then Wait for 1 seconds
    When Enter "9876543210" in Mobile Number
    Then Wait for 5 seconds
    Then close browser
