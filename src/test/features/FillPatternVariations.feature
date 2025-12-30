Feature: Test Fill Pattern Variations
  Verify all natural language variations for filling fields work correctly

  Scenario: Test "with" pattern variations
    Given Navigate to "https://demoqa.com/text-box"
    
    # Original pattern
    When Fill Full Name with 'John Doe'
    
    # With filler words
    When Fill Email with given value 'john@example.com'

    # Using "Set" verb
    When Set Current Address with the text '123 Main St'
    
    # Using "Update" verb
    When Update Permanent Address with the value '456 Oak Ave'
    
    And Close the browser

  Scenario: Test "in/into" pattern variations
    Given Navigate to "https://demoqa.com/text-box"
    
    # Original pattern
    When Enter 'Jane Smith' in Full Name
    
    # With filler words
    When Enter given value 'jane@test.com' in Email
    
    # Using "the text"
    When Enter the text '789 Park Lane' into Current Address
    
    # Using "the value"
    When Type the value '321 Elm St' in Permanent Address
    
    And Close the browser
