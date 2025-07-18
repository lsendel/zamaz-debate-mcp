Feature: User Registration API

  Background:
    * url baseUrl

  Scenario: User registration
    Given path '/api/v1/auth/register'
    And request registerRequest
    And header Content-Type = 'application/json'
    When method post
    Then status 201
    And match response.token == '#string'
    And match response.refreshToken == '#string'
    And match response.user.email == registerRequest.email
    And match response.user.name == registerRequest.name
    And match response.user.id == '#string'
    And match response.user.organizationId == '#string'