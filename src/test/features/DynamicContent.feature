Feature: Dynamic Content and AJAX Testing
  Testing dynamically loaded content, AJAX calls, real-time updates, and async operations

  Background:
    Given Navigate to "https://demoqa.com/dynamic-properties"

  Scenario: Wait for element to appear
    When page loads
    Then Wait for "Welcome Message" to appear
    And Verify message is displayed within 5 seconds
    And Check message has correct text

  Scenario: Wait for element to disappear
    When I click "Show Loading"
    Then Verify loading spinner appears
    And Wait for spinner to disappear
    And Check content is loaded

  Scenario: Wait for text to change
    When I click "Refresh Status"
    Then Wait for status to change from "Pending" to "Complete"
    And Verify status updated successfully
    And Check timestamp is recent

  Scenario: Wait for button to become enabled
    When form loads
    Then Verify submit button is disabled
    When I fill all required fields
    Then Wait for submit button to become enabled
    And Check button is clickable

  Scenario: Real-time counter update
    When I start counter
    Then Verify counter increments every second
    And Wait for counter to reach 10
    And Check final value is 10

  Scenario: Live search with debounce
    When I type "Java" in search box
    Then Wait 500 milliseconds
    And Verify search results appear
    When I type "Script"
    Then Verify results update to "JavaScript"
    And Check only relevant results shown

  Scenario: Progressive form validation
    When I type invalid email "test@"
    Then Wait for error message to appear
    And Verify "Invalid email format" is shown
    When I complete email to "test@example.com"
    Then Wait for error to disappear
    And Check validation passed

  Scenario: Auto-save with delay
    When I type content in editor
    And I pause typing
    Then Wait for "Saving..." indicator
    And Wait for "Saved" confirmation
    And Verify changes are persisted

  Scenario: Polling for status updates
    When I submit long-running task
    Then Verify "Processing..." status shows
    And Wait up to 30 seconds for completion
    And Check status changes to "Completed"
    And Verify results are displayed

  Scenario: Notification toast auto-dismiss
    When I perform action that triggers notification
    Then Verify toast notification appears
    And Wait for 3 seconds
    And Verify notification auto-dismisses
    And Check notification is no longer visible

  Scenario: Lazy loading images in feed
    When I scroll down news feed
    Then Verify "Loading..." placeholder shows
    And Wait for images to load
    And Check images replace placeholders
    And Verify images are fully rendered

  Scenario: WebSocket real-time updates
    When I open live dashboard
    Then Verify connection status is "Connected"
    And Wait for new data to arrive
    And Check dashboard updates without refresh
    And Verify timestamp is current

  Scenario: API response with loading state
    When I click "Fetch Data" button
    Then Verify button shows "Loading..." text
    And Verify button is disabled
    And Wait for API response
    And Check button text returns to "Fetch Data"
    And Verify data is displayed in table

  Scenario: Conditional element visibility
    When I check "Show Advanced Options"
    Then Wait for advanced panel to slide in
    And Verify panel is visible
    When I uncheck "Show Advanced Options"
    Then Wait for panel to slide out
    And Verify panel is hidden

  Scenario: Network idle before proceeding
    When complex page loads
    Then Wait for all network requests to complete
    And Verify page is fully interactive
    And Check no pending AJAX calls
