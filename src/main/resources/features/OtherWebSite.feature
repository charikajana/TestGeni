Feature: Navigation Feature

  Scenario: Open link in new tab
    When I navigate to "https://vinothqaacademy.com/multiple-windows/"
    
    # Use the new robust action: Click AND Switch in one atomic step using waitForPopup logic
    When I click on New Browser Tab button and switch to new window
    And wait for 10 seconds
    Then Verify page title is "Demo Site – WebTable – Vinoth Tech Solutions"
    And Enter "Vinoth" in Name field
    And Enter "QA" in Role field
    And Enter "vinoth@vinoth.com" in Email Address field
    And Enter "Chennai" in Location field
    And Enter "IT" in Department field
    And click on Add Row button
    And Verify new row is added with "Vinoth" in Name column
    And wait for 10 seconds
    And take the screenshot
    And select the checkbox in the row where Name column value is "Vinoth"
    And click on Delete Selected Row button
    Then validate "Vinoth" is deleted from the table
    And wait for 10 seconds
    And take the screenshot

    