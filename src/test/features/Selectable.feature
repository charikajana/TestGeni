Feature: Multiselect List Interactions
  Test scenarios for selecting multiple items from selectable lists (non-dropdown)
  Examples from: https://demoqa.com/selectable

  Scenario: Single item selection from list
    Given Navigate to "https://demoqa.com/selectable"
    When Select 'Cras justo odio' from list
    Then Verify 'Cras justo odio' is selected
    And Close the browser

  Scenario: Multiple items selection from list
    Given Navigate to "https://demoqa.com/selectable"
    When Select multiple items 'Cras justo odio;Morbi leo risus'
    Then Verify items 'Cras justo odio;Morbi leo risus' are selected
    And Close the browser

  Scenario: Grid multiselect with verification
    Given Navigate to "https://demoqa.com/selectable"
    When Click 'Grid'
    And Select multiple items 'One;Five;Nine'
    Then Verify items 'One;Five;Nine' are selected
    And Verify 'Two' is not selected
    And Close the browser

  Scenario: Sequential single selections (each item gets selected individually)
    Given Navigate to "https://demoqa.com/selectable"
    When Select 'Cras justo odio' from list
    Then Verify 'Cras justo odio' is selected
    When Select 'Morbi leo risus' from list
    Then Verify 'Morbi leo risus' is selected
    # Both items remain selected - this is the correct behavior for selectable lists
    And Verify 'Cras justo odio' is selected
    And Close the browser

  Scenario: Complete multiselect workflow
    Given Navigate to "https://demoqa.com/selectable"
    # Select from List tab
    When Select multiple items 'Cras justo odio;Porta ac consectetur ac'
    Then Verify items 'Cras justo odio;Porta ac consectetur ac' are selected
    
    # Switch to Grid tab
    When Click 'Grid'
    And Select multiple items 'One;Three;Five;Seven;Nine'
    Then Verify items 'One;Three;Five;Seven;Nine' are selected
    And Verify 'Two' is not selected
    And Verify 'Four' is not selected
    And Close the browser
