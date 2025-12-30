Feature: Slider Interaction
  As a test automation user
  I want to interact with slider components
  So that I can test range input functionality across different applications

  Scenario: Interact with slider component
    Given Navigate to "https://demoqa.com/slider"
    And wait for 5 seconds
    When Set slider to "75"
    And wait for 2 seconds
    And Take the Screenshot
    When Move slider to "50"
    And wait for 2 seconds
    And Take the Screenshot
    When Adjust slider to "90"
    And wait for 2 seconds
    And Take the Screenshot
