Feature: Links Automation
  Scenario: Interact with Links
    Given Open the browser and go to "https://demoqa.com"
    When Click "Elements"
    And Click on Links tab
    And wait for page load
    And click on Created link
    And wait for 2 seconds
    Then validate "Link has responded with staus 201 and status text Created" message is displayed
    And click on No Content link
    And wait for 2 seconds
    Then validate "Link has responded with staus 204 and status text No Content" message is displayed
    And click on Moved
    And wait for 2 seconds
    Then validate "Link has responded with staus 301 and status text Moved Permanently" message is displayed
    And click on Bad Request
    And wait for 2 seconds
    Then verify "Link has responded with staus 400 and status text Bad Request" message should be display
    And click on Unauthorized
    And wait for 4 seconds
    Then verify "Link has responded with staus 401 and status text Unauthorized" this text present
    And click on Forbidden
    And wait for 4 seconds
    Then validate "Link has responded with staus 403 and status text Forbidden" text present
    And click on Not Found
    And wait for 4 seconds
    Then validate "Link has responded with staus 404 and status text Not Found" message should be display






    