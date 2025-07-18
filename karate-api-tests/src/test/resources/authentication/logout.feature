Feature: User Logout API

  Background:
    * url baseUrl

  Scenario: User logout
    Given path '/api/v1/auth/logout'
    And header Authorization = 'Bearer ' + token
    And header Content-Type = 'application/json'
    When method post
    Then status 200
    And match response.message == 'Logout successful'