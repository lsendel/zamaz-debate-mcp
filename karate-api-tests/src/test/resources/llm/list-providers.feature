Feature: List LLM Providers API

  Background:
    * url baseUrl

  Scenario: List available providers
    Given path '/api/v1/llm/providers'
    And header Authorization = 'Bearer ' + authToken
    When method get
    Then status 200
    And match response.providers == '#array'
    And match response.providers[0].id == '#string'
    And match response.providers[0].name == '#string'
    And match response.providers[0].status == '#string'
    And match response.providers[0].models == '#array'
    And match response.providers[0].capabilities == '#object'