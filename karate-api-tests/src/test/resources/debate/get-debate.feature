Feature: Get Debate API

  Background:
    * url baseUrl

  Scenario: Get debate by ID
    Given path '/api/debates/' + debateId
    And header Authorization = 'Bearer ' + authToken
    When method get
    Then status 200
    And match response.id == debateId
    And match response.topic == '#string'
    And match response.description == '#string'
    And match response.status == '#string'
    And match response.config == '#object'
    And match response.positions == '#array'
    And match response.participants == '#array'
    And match response.rounds == '#array'
    And match response.createdAt == '#string'
    And match response.updatedAt == '#string'