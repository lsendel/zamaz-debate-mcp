Feature: Get LLM Provider Models API

  Background:
    * url baseUrl

  Scenario: Get provider models
    Given path '/api/v1/llm/providers/' + providerId + '/models'
    And header Authorization = 'Bearer ' + authToken
    When method get
    Then status 200
    And match response.providerId == providerId
    And match response.models == '#array'
    And match response.models[0].id == '#string'
    And match response.models[0].name == '#string'
    And match response.models[0].description == '#string'
    And match response.models[0].maxTokens == '#number'
    And match response.models[0].supportedFeatures == '#array'