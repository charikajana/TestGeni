Feature: Modern UI Components Testing
  Testing accordions, tabs, carousels, sliders, progress bars, and other modern UI elements

  Background:
    Given Navigate to "https://demoqa.com/auto-complete"

  Scenario: Accordion expand and collapse
    When User Enter "Re" in Type multiple color names
    And Click on "Green"
    And wait for 10 seconds
    Then Verify "Green" is selected
    When User Enter "Bl" in Type multiple color names
    And Click on "Blue"
    Then Verify "Blue" is selected
    And wait for 10 seconds
    And Take the Screenshot
    And remove "Green" from Type multiple color names
    Then verify "Green" is not selected
    And wait for 10 seconds
    And Take the Screenshot
    And remove "Blue" from Type multiple color names
    Then verify "Blue" is not selected
    And wait for 10 seconds
    And Take the Screenshot

    

