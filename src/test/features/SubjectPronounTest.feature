Feature: Subject Pronoun Variations - Phase 1 Testing
  Verify framework supports flexible subject pronouns with case-insensitive matching

  Background:
    Given Navigate to "https://demoqa.com"


  # ========================================
  # CHECKBOX - Subject Variations  
  # ========================================
  Scenario: Checkbox actions with subject variations
    When I navigate to "https://demoqa.com/checkbox"
    # Lowercase 'i'
    And i check Home check Box
    And wait for 2 seconds
    # Verify checked
    Then I verify Home check Box is selected
    # Uppercase 'USER'
    And USER uncheck Home check Box
    Then I verify Home check Box is not selected
    # Title case 'We'
    Then We check Home check Box
    # No subject
    And uncheck Home check Box

  # ========================================
  # DOUBLE CLICK - Subject Variations
  # ========================================
  Scenario: Double click with subject variations
    When I navigate to "https://demoqa.com/buttons"
    # With 'I'
    Then I double click on Double Click Me
    # Verify the double click message
    And Verify "You have done a double click" is displayed
    # With 'user' (lowercase)
    When user double click on Double Click Me
    # Uppercase 'WE'
    Then WE double tap on Double Click Me

  # ========================================
  # RIGHT CLICK - Subject Variations
  # ========================================
  Scenario: Right click with subject variations
    When Navigate to "https://demoqa.com/buttons"
    # With 'I'
    And I right click on Right Click Me
    # Verify right click message
    Then Verify "You have done a right click" is displayed
    # With 'user'
    When user right click on Right Click Me
    # Uppercase variations
    Then USER right tap on Right Click Me
    # No subject
    And right click on Right Click Me

  # ========================================
  # COMBINED VARIATIONS - Real World Usage
  # ========================================
  Scenario: Real world form filling with mixed subjects
    Given I navigate to "https://demoqa.com/text-box"
    # Different case variations
    When i enter "John" in Full Name
    And USER enter "john@example.com" in Email
    Then We enter "Dallas" in Current Address
    # No subject
    And enter "Texas" in Permanent Address
    # Submit
    When I click Submit
    # Verify
    Then i verify "John" is displayed
    And user verify "john@example.com" is displayed

  # ========================================
  # GHERKIN KEYWORD + SUBJECT VARIATIONS
  # ========================================
  Scenario: Gherkin keywords with subjects
    # Given + Subject
    Given I navigate to "https://demoqa.com/buttons"
    # When + Subject (uppercase)
    When USER click on Click Me
    # Then + Subject (lowercase)
    Then i verify "You have done a dynamic click" is displayed
    # And + Subject (title case)
    And We wait for 1 seconds
    # But + No subject
    But wait 1 sec

  # ========================================
  # ALL PRONOUNS COVERAGE
  # ========================================
  Scenario: Testing all supported pronouns
    Given Navigate to "https://demoqa.com/text-box"
    # I
    When I click Full Name
    # user
    And user click Email
    # we
    Then we wait for 1 seconds
    # he
    And he wait 1 sec
    # she
    Then she wait 1s
    # they
    And they wait for 1 seconds
    
  # ========================================
  # CASE INSENSITIVITY - EXTREME TEST
  # ========================================
  Scenario: Extreme case variations
    Given Navigate to "https://demoqa.com"
    # All lowercase
    When i click elements
    # All uppercase
    Then USER CLICK TEXT BOX
    # Mixed case (natural)
    And We Click Full Name
    # Mixed case (unnatural)
    Then uSeR cLiCk Email
    # Verify case insensitivity
    And Verify "name@example.com" with Email place holder
    And Verify Full Name Place Holder value "Full Name"

  # ========================================
  # WAIT ACTIONS - Subject Variations
  # ========================================
  Scenario: Wait actions with different subjects
    When I navigate to "https://demoqa.com/text-box"
    # Lowercase 'i'
    And i wait for 1 seconds
    # Uppercase 'USER'
    Then USER wait 1 sec
    # Title case 'We'
    And We wait for 1s
    # No subject
    Then wait for 2 seconds

  # ========================================
  # SCREENSHOT - Subject Variations
  # ========================================
  Scenario: Screenshot with subject variations
    When I navigate to "https://demoqa.com"
    # With 'I'
    Then I take a screenshot
    # With 'user'
    And user capture a snapshot
    # Uppercase 'WE'
    Then WE take screenshot
    # No subject
    And take a screenshot

  # ========================================
  # SELECT/DESELECT - Subject Variations
  # ========================================
  Scenario: Select actions with subject variations
    When Navigate to "https://demoqa.com/select-menu"
    # Lowercase 'i'
    And i select "Group 1, option 1" from Select Option
    # Title case 'User'
    Then User select "Green" from Old Select Menu
    # Uppercase 'WE'
    And WE choose "Purple" from Old Select Menu
    # No subject
    Then select "Red" from Old Select Menu
