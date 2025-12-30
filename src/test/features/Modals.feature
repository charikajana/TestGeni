Feature: Modal Dialogs

  Scenario: Interacting with Small Modal
    Given Navigate to "https://demoqa.com/modal-dialogs"
    When Click "Small modal"
    Then Verify the message "Small Modal" is displayed
    And Verify "This is a small modal. It has very less content" is present
    When Close the popup
    Then Verify modal with title "Small Modal" is not displayed

  Scenario: Interacting with Large Modal
    Given Navigate to "https://demoqa.com/modal-dialogs"
    When Click "Large modal"
    Then Verify the header "Large Modal" is visible
    And Verify that "Lorem Ipsum is simply dummy text" is displayed
    When Close the dialog box
    Then Verify modal with title "Large Modal" is not displayed
