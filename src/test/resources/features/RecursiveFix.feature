Feature: Recursive Fix Verification

  Scenario: Verify no infinite loop in self-healing
    When I go to "https://demoqa.com/auto-complete"
    And I fill "Blue" into "Type multiple color names"
    # Wait for the option to appear and select it
    And I wait for 1 seconds
    And I click "Blue"
    And I fill "Green" into "Type multiple color names"
    And I wait for 1 seconds
    And I click "Green"
    Then Verify "Blue" is displayed
    And Verify "Green" is displayed
    # Now for the removal test
    When I remove "Green" from "Type multiple color names"
    Then Verify "Green" is not displayed
    And Verify "Blue" is displayed
