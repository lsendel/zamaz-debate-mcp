Feature: Check LLM Provider Health API

  Background:
    * url baseUrl

  Scenario: Check provider health
    Given path '/api/v1/llm/providers/' + providerId + '/health'
    And header Authorization = 'Bearer ' + authToken
    When method get
    Then status 200
    And match response.providerId == providerId
    And match response.status == '#string'
    And match response.healthy == '#boolean'
    And match response.lastChecked == '#string'
    And match response.responseTime == '#number'
    And match response.details == '#object'