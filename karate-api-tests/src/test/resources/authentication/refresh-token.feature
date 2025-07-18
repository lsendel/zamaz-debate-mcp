Feature: Token Refresh API

  Background:
    * url baseUrl

  Scenario: Refresh authentication token
    Given path '/api/v1/auth/refresh'
    And request refreshRequest
    And header Content-Type = 'application/json'
    When method post
    Then status 200
    And match response.token == '#string'
    And match response.refreshToken == '#string'
    And match response.expiresIn == '#number'
    And match response.tokenType == 'Bearer'