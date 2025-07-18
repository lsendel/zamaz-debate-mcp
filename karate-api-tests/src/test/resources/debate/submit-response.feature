Feature: Submit Response to Debate API

  Background:
    * url baseUrl

  Scenario: Submit response to debate
    Given path '/api/debates/' + debateId + '/responses'
    And request responseRequest
    And header Authorization = 'Bearer ' + authToken
    And header Content-Type = 'application/json'
    When method post
    Then status 201
    And match response.id == '#string'
    And match response.content == responseRequest.content
    And match response.type == responseRequest.type
    And match response.debateId == debateId
    And match response.participantId == '#string'
    And match response.round == '#number'
    And match response.submittedAt == '#string'
    And match response.status == 'SUBMITTED'