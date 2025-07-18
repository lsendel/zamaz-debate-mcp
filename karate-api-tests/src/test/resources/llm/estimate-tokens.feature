Feature: Estimate Tokens API

  Background:
    * url baseUrl

  Scenario: Estimate tokens for text
    Given path '/api/v1/llm/tokens/estimate'
    And request { text: '#(text)', model: '#(model)' }
    And header Authorization = 'Bearer ' + authToken
    And header Content-Type = 'application/json'
    When method post
    Then status 200
    And match response.text == text
    And match response.model == model
    And match response.tokenCount == '#number'
    And match response.estimatedCost == '#number'
    And match response.breakdown == '#object'