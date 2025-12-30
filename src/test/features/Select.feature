Feature: Comprehensive Dropdown/Select Testing - All Frameworks

  Background:
    Given Open the browser and go to "https://demoqa.com/select-menu"
    And wait for page load

  Scenario: Test React-Select Multi-Select Dropdown
    # React-Select with multiple selection capability
    When Select "Green" from Multiselect drop down
    When deselect "Green" from Multiselect drop down
    When Select "Green" and "Blue" and "Black" from Multiselect drop down
    Then Verify "Green" is displayed
    

    
    # Note: Multi-select allows multiple values simultaneously

  Scenario: Test Standard Multi-Select Element
    # Standard HTML <select multiple>
    When Select "Volvo" from Standard multi select
    Then Verify "Volvo" is displayed
    
    When Select "Saab" from Standard multi select
    Then Verify "Saab" is displayed
    
    When Select "Audi" from Standard multi select
    Then Verify "Audi" is displayed

  Scenario: Test All Dropdown Variations in Different Syntax
    # Using "choose" instead of "select"
    When choose "Yellow" in Old Style Select Menu
    Then Verify "Yellow" is displayed
    
    # Using "set...to" syntax
    When set Old Style Select Menu to "Red"
    Then Verify "Red" is displayed
    
    # Select from React dropdown using "choose"
    When choose "Ms." in Select One
    Then Verify "Ms." is displayed
    
    # Take screenshot at the end
    And take screenshot

  Scenario: Sequential Dropdown Interactions
    # Test changing values multiple times
    When Select "Blue" from Old Style Select Menu
    And Select "Mr." from Select One
    And Select "Group 1, option 2" from Select Value
    Then Verify "Blue" is displayed
    And Verify "Mr." is displayed
    And Verify "Group 1, option 2" is displayed
    
    # Change all values
    When Select "Green" from Old Style Select Menu
    And Select "Dr." from Select One
    And Select "Another root option" from Select Value
    Then Verify "Green" is displayed
    And Verify "Dr." is displayed
    And Verify "Another root option" is displayed

    Scenario: Test Standard HTML Select Dropdown
    # Standard <select> element - Uses native selectOption()
    When Select "Blue" from Old Style Select Menu
    Then Verify "Blue" is displayed
    
    When Select "Green" from Old Style Select Menu
    Then Verify "Green" is displayed
    
    When Select "Purple" from Old Style Select Menu
    Then Verify "Purple" is displayed

  Scenario: Test React-Select Dropdown with Option Groups
    # React-Select with grouped options
    When Select "Group 1, option 1" from Select Value
    Then Verify "Group 1, option 1" is displayed
    
    When Select "Group 2, option 1" from Select Value
    Then Verify "Group 2, option 1" is displayed
    
    When Select "A root option" from Select Value
    Then Verify "A root option" is displayed

  Scenario: Test React-Select Single Selection Dropdown
    # React-Select for title selection
    When Select "Dr." from "Select One"
    Then Verify "Dr." is displayed
    
    When Select "Mr." from "Select One"
    Then Verify "Mr." is displayed
    
    When Select "Mrs." from "Select One"
    Then Verify "Mrs." is displayed
    
    When Select "Prof." from "Select One"
    Then Verify "Prof." is displayed   
