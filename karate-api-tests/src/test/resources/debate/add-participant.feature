Feature: Add Participant to Debate API

  Background:
    * url baseUrl

  Scenario: Add participant to debate
    Given path '/api/debates/' + debateId + '/participants'
    And request participantRequest
    And header Authorization = 'Bearer ' + authToken
    And header Content-Type = 'application/json'
    When method post
    Then status 201
    And match response.id == '#string'
    And match response.userId == participantRequest.userId
    And match response.position == participantRequest.position
    And match response.role == participantRequest.role
    And match response.status == 'ACTIVE'
    And match response.joinedAt == '#string'
    And match response.debateId == debateId