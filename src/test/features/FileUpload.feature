Feature: File Upload Testing
  Comprehensive testing of file upload functionality including single, multiple, drag-drop, and validation

  Background:
    Given Navigate to "https://demoqa.com/upload-download"

  Scenario: Upload single file
    When I upload "test-document.pdf" to file input
    Then Verify "test-document.pdf" is displayed
    And Check upload success message appears

  Scenario: Upload multiple files
    When I attach multiple files to upload field
      | document1.pdf |
      | image.jpg     |
      | data.xlsx     |
    Then Verify 3 files are uploaded
    And Check all file names are displayed

  Scenario: Drag and drop file upload
    When user drags "report.pdf" to upload area
    Then Verify file is uploaded successfully
    And Check "report.pdf" appears in file list

  Scenario: Upload with file type validation
    When I try to upload "script.exe" file
    Then Verify error message "Invalid file type" is shown
    And Check file is not uploaded

  Scenario: Upload with size validation
    When I upload large file exceeding limit
    Then Verify "File size exceeds 5MB limit" error appears
    And Check upload is rejected

  Scenario: Remove uploaded file before submission
    When I upload "temp-file.pdf"
    And I click remove button next to file
    Then Verify file is removed from list
    And Check upload field is empty

  Scenario: Replace uploaded file
    When I upload "old-document.pdf"
    And I upload "new-document.pdf" to replace it
    Then Verify only "new-document.pdf" is shown
    And Check "old-document.pdf" is not displayed
