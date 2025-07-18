Feature: Create Organization API

  Background:
    * url baseUrl

  Scenario: Create organization
    Given path '/api/v1/organizations'
    And request organizationRequest
    And header Authorization = 'Bearer ' + authToken
    And header Content-Type = 'application/json'
    When method post
    Then status 201
    And match response.id == '#string'
    And match response.name == organizationRequest.name
    And match response.description == organizationRequest.description
    And match response.settings == '#object'
    And match response.tier == '#string'
    And match response.features == '#object'
    And match response.active == true
    And match response.createdAt == '#string'
    And match response.updatedAt == '#string'