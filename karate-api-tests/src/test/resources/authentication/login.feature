Feature: User Login API

  Background:
    * url baseUrl

  Scenario: User login
    Given path '/api/v1/auth/login'
    And request loginRequest
    And header Content-Type = 'application/json'
    When method post
    Then status 200
    And match response.token == '#string'
    And match response.refreshToken == '#string'
    And match response.user.email == loginRequest.email
    And match response.user.id == '#string'
    And match response.user.organizationId == '#string'