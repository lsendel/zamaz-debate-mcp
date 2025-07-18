Feature: Create Debate API

  Background:
    * url baseUrl

  Scenario: Create debate
    Given path '/api/debates'
    And request debateRequest
    And header Authorization = 'Bearer ' + authToken
    And header Content-Type = 'application/json'
    When method post
    Then status 201
    And match response.id == '#string'
    And match response.topic == debateRequest.topic
    And match response.description == debateRequest.description
    And match response.status == 'DRAFT'
    And match response.config == '#object'
    And match response.positions == '#array'
    And match response.createdAt == '#string'
    And match response.updatedAt == '#string'
    And match response.createdBy == '#string'
    And match response.organizationId == '#string'