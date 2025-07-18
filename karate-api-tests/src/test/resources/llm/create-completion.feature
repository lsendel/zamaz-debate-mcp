Feature: Create LLM Completion API

  Background:
    * url baseUrl

  Scenario: Create completion
    Given path '/api/v1/llm/completions'
    And request completionRequest
    And header Authorization = 'Bearer ' + authToken
    And header Content-Type = 'application/json'
    When method post
    Then status 200
    And match response.id == '#string'
    And match response.content == '#string'
    And match response.provider == '#string'
    And match response.model == '#string'
    And match response.usage == '#object'
    And match response.usage.inputTokens == '#number'
    And match response.usage.outputTokens == '#number'
    And match response.usage.totalTokens == '#number'
    And match response.finishReason == '#string'
    And match response.createdAt == '#string'