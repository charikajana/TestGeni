Feature: Element Scoping
  
  Scenario: Scoped Clicking and Filling
    Given I navigate to "https://demoqa.com/automation-practice-form"
    When I fill "John" inside First Name
    And I fill "Doe" within Last Name
    And I click "Male" inside Gender
    Then Verify "Male" is selected
    
  Scenario: Scoped Verification
    Given I navigate to "https://demoqa.com/login"
    Then Verify "Login" inside Main Header is displayed
    And Verify "UserName" within User Form is displayed
