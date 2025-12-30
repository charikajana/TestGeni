Feature: Complete Form Submission Flow
  End-to-end testing of form filling and submission with page refresh isolation and validation checks

  Background:
    Given Navigate to "https://demoqa.com/automation-practice-form"

  Scenario: Complete registration form submission
    # Personal Information
    When I enter "John" in first name
    And I enter "Doe" in last name
    And I type "john.doe@example.com" in email
    And I click Male radio button
    And I enter "1234567890" in Mobile Number
    
    # Address
    And I enter "123 Main Street, Apt 4B" in current address
    
    # Submit
    And I click submit button
    
    # Verification
    Then Verify "Thanks for submitting the form" is displayed
    And I press Escape to close modal

  Scenario: Negative - Submit without filling required fields
    When I refresh the page
    And I click submit button
    Then Verify "first name" field is invalid
    And Verify "last name" field has red border
    And Verify "Mobile Number" shows error
    # These steps now use the new VerifyValidationAction to check CSS/HTML validation

  Scenario: Fill form fields and verify
    When I refresh the page
    And I enter "Alice" in first name
    And I enter "Smith" in last name
    Then Verify "Alice" is filled in first name field
    And Verify "Smith" is filled in last name field

  Scenario: Another complete form submission
    When I refresh the page
    And I enter "Bob" in first name
    And I enter "Johnson" in last name  
    And I enter "bob@example.com" in email
    And I click Female radio button
    And I enter "9876543210" in Mobile Number
    And I enter "456 Oak Avenue" in current address
    And I click submit button
    Then Verify "Thanks for submitting the form" is displayed
    And I press Escape to close modal
