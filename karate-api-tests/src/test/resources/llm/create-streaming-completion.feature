Feature: Create Streaming LLM Completion API

  Background:
    * url baseUrl

  Scenario: Create streaming completion
    Given path '/api/v1/llm/completions/stream'
    And request streamingRequest
    And header Authorization = 'Bearer ' + authToken
    And header Content-Type = 'application/json'
    And header Accept = 'text/event-stream'
    When method post
    Then status 200
    And match response.streamId == '#string'
    And match response.status == 'streaming'
    And match response.provider == '#string'
    And match response.model == '#string'