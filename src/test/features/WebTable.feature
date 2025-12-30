Feature: Checkbox Automation
  Scenario: Interact with Checkboxes
    Given Open the browser and go to "https://demoqa.com"
    When I Click Elements
    And we Click Web Tables
    And User Click on Add button
    Then Verify "Registration Form" is displayed
    And User fill the first name field with "John"
    And User fill the last name field with "Doe"
    And fill the email field with "john.doe@example.com"
    And fill the age field with "30"
    And fill the salary field with "5000"
    And fill the department field with "IT"
    And click on Submit button
    Then Verify New Row is added with "John" in First Name column
    And take the ScreenShot
    And wait for 20 seconds
    # Extract all column values from the row where First Name is "John"
    And I get all column values where "First Name" is "John"
    And click on Edit Icon in the row where "First Name" is "John"
    And fill the first name field with "Chari"
    And fill the last name field with "Palapadu"
    And fill the email field with "johnChari.doe@example.com"
    And fill the age field with "30"
    And fill the salary field with "5000"
    And fill the department field with "ITNON"
    And click on Submit button
    Then Verify New Row is added with "Chari" in First Name column
    And take the ScreenShot
    And wait for 20 seconds
    And click on Delete Icon in the row where "First Name" is "Chari"
    Then Validate row should not be present where "First Name" is "Chari"




    