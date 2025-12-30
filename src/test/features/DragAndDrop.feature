Feature: Drag and Drop Interactions
  Testing drag and drop functionality for sorting, moving, and reordering elements

  Background:
    Given Navigate to "https://demoqa.com/dragabble"

  Scenario: Simple drag and drop
    When I drag "Draggable" element
    And I drop it on "Droppable" target
    Then Verify "Dropped!" message appears
    And Check target background color changes

  Scenario: Drag element to specific position
    When user drags "Box 1" to coordinates (300, 400)
    Then Verify element moved to new position
    And Check element remains at that location

  Scenario: Reorder list items by dragging
    When I drag "Item 3" above "Item 1"
    Then Verify new order is "Item 3, Item 1, Item 2"
    And Check list updated correctly

  Scenario: Drag to sort table columns
    When I drag "Name" column header
    And I drop it after "Email" column
    Then Verify columns reordered to "Email, Name, Phone"
    And Check data rows follow column order

  Scenario: Drag multiple items to container
    When I drag "File 1" to upload zone
    And I drag "File 2" to upload zone
    And I drag "File 3" to upload zone
    Then Verify 3 items in upload zone
    And Check all files are listed

  Scenario: Drag restricted to axis
    When I drag element with horizontal restriction
    And I try to move it vertically
    Then Verify element only moves horizontally
    And Check vertical position unchanged

  Scenario: Drag with snap to grid
    When I enable snap to grid
    And I drag element freely
    Then Verify element snaps to nearest grid point
    And Check position is grid-aligned

  Scenario: Cancel drag operation
    When I start dragging "Item A"
    And I press Escape key
    Then Verify drag is cancelled
    And Check element returns to original position

  Scenario: Drag between containers
    When I drag "Task 1" from "To Do" list
    And I drop it in "In Progress" list
    Then Verify "Task 1" removed from "To Do"
    And Check "Task 1" appears in "In Progress"
    And Verify task count updated in both lists

  Scenario: Drag to reorder with visual feedback
    When user starts dragging "Card 2"
    Then Verify drag placeholder appears
    And Check other items shift to make space
    When I drop card at new position
    Then Verify items rearranged smoothly
    And Check no placeholder remains
