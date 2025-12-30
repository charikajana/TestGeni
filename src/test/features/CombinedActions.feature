Feature: Combined Actions - Student Registration Form
  Testing realistic combined actions on the DemoQA Practice Form to ensure proper
  handling of composite steps with multiple actions chained together.

  Background:
    Given Navigate to "https://demoqa.com/automation-practice-form"

  Scenario: Quick registration with "X as Y" syntax (5 actions in one line)
    When Enter First Name as "John" and Enter Last Name as "Doe" and Enter Email as "john.doe@test.com" and Click on Male and Enter Mobile Number as "9876543210"
    And Click Submit
    And Wait for 2 seconds
    Then Verify "Thanks for submitting the form" is displayed
    And I press Escape to close modal

  Scenario: Registration with "in X" syntax (testing alternate phrasing)
    When Enter "Alice" in First Name and Enter "Smith" in Last Name and Enter "alice.smith@test.com" in Email
    And Click on Female and Enter "8765432109" in Mobile Number
    And Click Submit
    And Wait for 2 seconds
    Then Verify "Thanks for submitting the form" is displayed
    And I press Escape to close modal

  Scenario: Mixed syntax - combining "as" and "in" patterns
    When Enter First Name as "Bob" and Enter "Johnson" in Last Name and Enter Email as "bob.j@example.com"
    And Enter "7654321098" in Mobile Number and Click on Male
    And Click Submit
    And Wait for 2 seconds
    Then Verify "Thanks for submitting the form" is displayed
   And I press Escape to close modal

  Scenario: Sequential fills followed by single click
    When Enter First Name as "Emma" and Enter Last Name as "Wilson" and Enter Email as "emma.w@test.com" and Enter Mobile Number as "6543210987"
    And Click on Female
    And Click Submit
    And Wait for 2 seconds
    Then Verify "Thanks for submitting the form" is displayed
    And I press Escape to close modal

  Scenario: Two-action composite (minimal case)
    When Enter First Name as "Tom" and Enter Last Name as "Brown"
    And Enter Email as "tom.brown@test.com"
    And Enter Mobile Number as "5432109876"
    And Click on Male
    And Click Submit
    And Wait for 2 seconds
    Then Verify "Thanks for submitting the form" is displayed
    And I press Escape to close modal

  Scenario: Click and fill in same line
    When Click on Male and Enter First Name as "Sarah"
    And Enter Last Name as "Davis" and Enter Email as "sarah.d@test.com"
    And Enter Mobile Number as "4321098765"
    And Click Submit
    And Wait for 2 seconds
    Then Verify "Thanks for submitting the form" is displayed
    And I press Escape to close modal

  Scenario: Testing comma delimiter in composite actions
    When Enter First Name as "Mike", Enter Last Name as "Taylor", Enter Email as "mike.t@test.com"
    And Enter Mobile Number as "3210987654", Click on Male
    And Click Submit
    And Wait for 2 seconds
    Then Verify "Thanks for submitting the form" is displayed
    And I press Escape to close modal
