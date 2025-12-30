Feature: Keyboard Actions and Shortcuts
  Testing keyboard navigation, shortcuts, key presses, and accessibility features

  Background:
    Given Navigate to "https://demoqa.com/keyboard-test"

  Scenario: Press Enter to submit form
    When I fill "test@example.com" in email field
    And I press Enter key
    Then Verify form is submitted
    And Check success message appears

  Scenario: Press Escape to close modal
    When I open modal dialog
    And I press Escape key
    Then Verify modal closes
    And Check modal is no longer visible

  Scenario: Tab navigation through form fields
    When I press Tab key
    Then Verify focus moves to next field
    When I press Tab 5 times
    Then Verify all fields are navigated
    And Check focus returned to first field

  Scenario: Shift+Tab for reverse navigation
    When focus is on last field
    And I press Shift+Tab
    Then Verify focus moves to previous field
    And Check backward navigation works

  Scenario: Arrow key navigation in dropdown
    When I open dropdown menu
    And I press Down arrow key 3 times
    Then Verify third option is highlighted
    When I press Enter
    Then Verify selected option is chosen

  Scenario: Keyboard shortcuts - Save
    When I type content in editor
    And I press Ctrl+S
    Then Verify "Document saved" message appears
    And Check save action triggered

  Scenario: Keyboard shortcuts - Copy/Paste
    When I select text "Hello World"
    And I press Ctrl+C
    Then Verify text is copied to clipboard
    When I click in different field
    And I press Ctrl+V
    Then Verify "Hello World" is pasted

  Scenario: Keyboard shortcuts - Undo/Redo
    When I type "Original text"
    And I delete all text
    And I press Ctrl+Z
    Then Verify "Original text" is restored
    When I press Ctrl+Y
    Then Verify text is deleted again

  Scenario: Space bar to toggle checkbox
    When I focus on checkbox using Tab
    And I press Space key
    Then Verify checkbox is checked
    When I press Space again
    Then Verify checkbox is unchecked

  Scenario: Enter to activate button
    When I navigate to submit button using Tab
    And I press Enter key
    Then Verify button click action triggered
    And Check form is submitted

  Scenario: Home/End keys in text field
    When I type long text in input field
    And I press Home key
    Then Verify cursor moves to start
    When I press End key
    Then Verify cursor moves to end

  Scenario: Page Up/Page Down scrolling
    When I press Page Down key
    Then Verify page scrolls down one viewport
    When I press Page Up key
    Then Verify page scrolls back up

  Scenario: Alt+Number for quick actions
    When I press Alt+1
    Then Verify first tab is activated
    When I press Alt+2
    Then Verify second tab is activated

  Scenario: Multiple key combination
    When I press Ctrl+Shift+A
    Then Verify "Select All" action triggered
    And Check all content is selected
