Feature: User Registration and Account Deletion
  Scenario: Successful Signup with all details and Delete Account
    # Step 1: Navigate to page
    Given Navigate to "https://automationexercise.com"
    Then Verify the page title contains "Automation Exercise"
    
    # Step 4: Click on 'Signup / Login'
    When click on Signup / Login
    
    # Step 5: Verify 'New User Signup!'
    Then Verify "New User Signup!" is displayed
    
    # Step 6: Enter name and email
    And enter "Test User" in Signup Name
    And enter "testgeni_user_20260103_1120@example.com" in Signup Email Address
    
    # Step 7: Click 'Signup'
    And click on Signup
    
    # Step 8: Verify 'ENTER ACCOUNT INFORMATION'
    Then Verify "ENTER ACCOUNT INFORMATION" is displayed
    
    # Step 9: Fill Account Details
    And click on Mr.
    And enter "Password123!" in Password
    And select "1" from days
    And select "January" from months
    And select "1990" from years
    
    # Step 10 & 11: Select Checkboxes
    And check the checkbox Sign up for our newsletter!
    And check the checkbox Receive special offers from our partners!
    
    # Step 12: Fill Address details
    And enter "John" in First name
    And enter "Doe" in Last name
    And enter "Test Company" in Company
    And enter "123 Test Street" in Address
    And enter "Suite 400" in Address2
    And select "Canada" from country
    And enter "New York" in State
    And enter "New York City" in City
    And enter "M1M 1M1" in Zipcode
    And enter "1234567890" in Mobile Number
    
    # Step 20: Click 'Create Account' button
    And click on Create Account
    
    # Step 21: Verify 'ACCOUNT CREATED!' is displayed
    Then Verify "ACCOUNT CREATED!" is displayed
    
    # Step 22: Click 'Continue' button
    And click on Continue
    
    # Step 23: Verify that 'Logged in as username' is visible
    Then Verify "Logged in as Test User" is displayed
    
    # Step 24: Click 'Delete Account' button
    When click on Delete Account
    
    # Step 25: Verify that 'ACCOUNT DELETED!' is visible and click 'Continue' button
    Then Verify "ACCOUNT DELETED!" is displayed
    And click on Continue
