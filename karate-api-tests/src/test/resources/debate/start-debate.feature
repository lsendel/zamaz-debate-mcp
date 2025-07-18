Feature: Start Debate API

  Background:
    * url baseUrl

  Scenario: Start debate
    Given path '/api/debates/' + debateId + '/start'
    And header Authorization = 'Bearer ' + authToken
    And header Content-Type = 'application/json'
    When method post
    Then status 200
    And match response.id == debateId
    And match response.status == 'IN_PROGRESS'
    And match response.startedAt == '#string'
    And match response.currentRound == 1
    And match response.config == '#object'
    And match response.participants == '#array'