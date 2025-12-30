Feature: Checkbox Automation
  Scenario: Interact with Checkboxes
    Given Open the browser and go to "https://demoqa.com"
    When Click "Elements"
    And Click on Buttons tab
    And double click on "Double Click Me" button
    Then Verify "You have done a double click" is displayed
    And take the ScreenShot
    And wait for 20 seconds
    And right click on "Right Click Me" button
    Then Verify "You have done a right click" is displayed
    And take the ScreenShot
    And wait for 20 seconds
    And Click on "Click Me" button
    Then Verify "You have done a dynamic click" is displayed
    And take the ScreenShot
    And wait for 20 seconds



    