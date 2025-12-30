Feature: Menu Interaction Testing
  Testing nested menu components

  Background:
    Given Navigate to "https://the-internet.herokuapp.com/jqueryui/menu"

  Scenario: Select nested sub-menu item with varied syntax
    When Select Enabled -> Downloads -> PDF from the menu
    And wait for 2 seconds
    And Navigate to "https://the-internet.herokuapp.com/jqueryui/menu"
    And Open Enabled / Downloads / PDF using the navigation menu
    And wait for 2 seconds
    And Navigate to "https://the-internet.herokuapp.com/jqueryui/menu"
    And Select Enabled -> Downloads -> PDF from navbar
    Then Verify "Enabled" is displayed

  Scenario: Select nested sub-menu item on VinothQA
    Given Navigate to "https://vinothqaacademy.com/multiple-windows/"
    When Select Free Complete QA Video Courses then Appium 2.0 Mobile Automation from menu
    And wait for 3 seconds
    And Take the Screenshot
    Then Verify "Appium 2.0 Full Course for Beginners" is displayed  
