Feature: Robot Demo
  Scenario: Navigate to DemoQA and Click Elements
    Given Open the browser and go to "https://demoqa.com/"
    When Click Elements tab
    And click on Text box
    And Enter Full Name "chari"
    And Enter Email "abce@gmail.com"
    And Enter Current Address "NRT"
    And Enter Permanent Address "NRT"
    And click on Submit Button
    Then validate the name "chari" is displayed
    Then Validate the email "abce@gmail.com" is displayed
    Then Validate the current address "NRT" is displayed
    Then Validate the permanent address "NRT" is displayed
    