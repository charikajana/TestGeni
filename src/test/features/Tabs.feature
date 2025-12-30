Feature: Modern UI Components Testing
  Testing accordions, tabs, carousels, sliders, progress bars, and other modern UI elements

  Background:
    Given Navigate to "https://demoqa.com/tabs"

  Scenario: Accordion expand and collapse
    When click on What tab
    And wait for 10 seconds
    And Take the Screenshot
    Then Verify "Lorem Ipsum is simply dummy text" is displayed
    And Click on Origin tab
    And wait for 10 seconds
    And Take the Screenshot
    Then Verify "Contrary to popular belief, " is displayed
    And Click on Use tab
    And wait for 10 seconds
    And Take the Screenshot
    Then Verify "It is a long established fact that a reader will" is displayed

    

