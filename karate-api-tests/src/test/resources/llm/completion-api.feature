@smoke @regression
Feature: LLM Completion API Test Suite

  Background:
    * def config = callonce read('classpath:karate-config.js')
    * def authFixtures = callonce read('classpath:fixtures/auth-fixtures.js')
    * def llmFixtures = callonce read('classpath:fixtures/llm-fixtures.js')
    * def llmUrl = config.serviceUrls.llm
    * url llmUrl

  @smoke
  Scenario: Create basic completion
    # Login and get auth token
    * def auth = authFixtures.login()
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    
    # Create completion request
    * def completionRequest = llmFixtures.generateCompletionRequest({
        prompt: "What is the capital of France?",
        maxTokens: 50,
        temperature: 0.3,
        provider: "claude",
        model: "claude-3-sonnet"
      })
    
    Given path config.endpoints.llm.completions
    And request completionRequest
    And headers authHeaders
    When method post
    Then status 200
    And match response.id == '#string'
    And match response.content == '#string'
    And match response.provider == 'claude'
    And match response.model == 'claude-3-sonnet'
    And match response.usage.inputTokens == '#number'
    And match response.usage.outputTokens == '#number'
    And match response.usage.totalTokens == '#number'
    And match response.finishReason == '#string'
    And match response.createdAt == '#string'
    And match response.content contains 'Paris'

  @smoke
  Scenario: Create completion with different providers
    * def auth = authFixtures.login()
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    
    # Test Claude provider
    * def claudeRequest = llmFixtures.generateCompletionRequest({
        prompt: "Explain AI in one sentence.",
        provider: "claude",
        model: "claude-3-haiku"
      })
    
    Given path config.endpoints.llm.completions
    And request claudeRequest
    And headers authHeaders
    When method post
    Then status 200
    And match response.provider == 'claude'
    And match response.model == 'claude-3-haiku'
    And match response.content == '#string'
    
    # Test OpenAI provider
    * def openaiRequest = llmFixtures.generateCompletionRequest({
        prompt: "Explain AI in one sentence.",
        provider: "openai",
        model: "gpt-3.5-turbo"
      })
    
    Given path config.endpoints.llm.completions
    And request openaiRequest
    And headers authHeaders
    When method post
    Then status 200
    And match response.provider == 'openai'
    And match response.model == 'gpt-3.5-turbo'
    And match response.content == '#string'

  @regression
  Scenario: Create completion with various prompt types
    * def auth = authFixtures.login()
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    * def testPrompts = llmFixtures.generateTestPrompts()
    
    # Test simple prompt
    * def simpleRequest = llmFixtures.generateCompletionRequest({
        prompt: testPrompts.simple,
        maxTokens: 100
      })
    
    Given path config.endpoints.llm.completions
    And request simpleRequest
    And headers authHeaders
    When method post
    Then status 200
    And match response.content == '#string'
    And match response.content.length > 0
    
    # Test complex prompt
    * def complexRequest = llmFixtures.generateCompletionRequest({
        prompt: testPrompts.complex,
        maxTokens: 500
      })
    
    Given path config.endpoints.llm.completions
    And request complexRequest
    And headers authHeaders
    When method post
    Then status 200
    And match response.content == '#string'
    And match response.content.length > 100
    
    # Test technical prompt
    * def technicalRequest = llmFixtures.generateCompletionRequest({
        prompt: testPrompts.technical,
        maxTokens: 300
      })
    
    Given path config.endpoints.llm.completions
    And request technicalRequest
    And headers authHeaders
    When method post
    Then status 200
    And match response.content == '#string'
    And match response.usage.outputTokens > 50

  @regression
  Scenario: Test completion with different temperature values
    * def auth = authFixtures.login()
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    * def basePrompt = "Write a creative story about a robot."
    
    # Test with low temperature (more deterministic)
    * def lowTempRequest = llmFixtures.generateCompletionRequest({
        prompt: basePrompt,
        temperature: 0.1,
        maxTokens: 200
      })
    
    Given path config.endpoints.llm.completions
    And request lowTempRequest
    And headers authHeaders
    When method post
    Then status 200
    And match response.content == '#string'
    * def lowTempResponse = response.content
    
    # Test with high temperature (more creative)
    * def highTempRequest = llmFixtures.generateCompletionRequest({
        prompt: basePrompt,
        temperature: 1.5,
        maxTokens: 200
      })
    
    Given path config.endpoints.llm.completions
    And request highTempRequest
    And headers authHeaders
    When method post
    Then status 200
    And match response.content == '#string'
    * def highTempResponse = response.content
    
    # Responses should be different due to temperature difference
    * assert lowTempResponse != highTempResponse

  @regression
  Scenario: Test token limit enforcement
    * def auth = authFixtures.login()
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    
    # Request with very low token limit
    * def limitedRequest = llmFixtures.generateCompletionRequest({
        prompt: "Write a long essay about artificial intelligence and its impact on society.",
        maxTokens: 20
      })
    
    Given path config.endpoints.llm.completions
    And request limitedRequest
    And headers authHeaders
    When method post
    Then status 200
    And match response.usage.outputTokens <= 20
    And match response.finishReason == 'length'

  @security
  Scenario: Test completion without authentication
    * def completionRequest = llmFixtures.generateCompletionRequest()
    
    Given path config.endpoints.llm.completions
    And request completionRequest
    When method post
    Then status 401
    And match response.error.code == 'UNAUTHORIZED'

  @security
  Scenario: Test completion with invalid token
    * def completionRequest = llmFixtures.generateCompletionRequest()
    
    Given path config.endpoints.llm.completions
    And request completionRequest
    And header Authorization = 'Bearer invalid-token'
    When method post
    Then status 401
    And match response.error.code == 'INVALID_TOKEN'

  @security
  Scenario: Test completion with malicious prompts
    * def auth = authFixtures.login()
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    * def testPrompts = llmFixtures.generateTestPrompts()
    
    # Test HTML injection
    * def htmlRequest = llmFixtures.generateCompletionRequest({
        prompt: testPrompts.html,
        maxTokens: 100
      })
    
    Given path config.endpoints.llm.completions
    And request htmlRequest
    And headers authHeaders
    When method post
    Then status 200
    And match response.content == '#string'
    And match response.content !contains '<script>'
    
    # Test SQL injection
    * def sqlRequest = llmFixtures.generateCompletionRequest({
        prompt: testPrompts.sql,
        maxTokens: 100
      })
    
    Given path config.endpoints.llm.completions
    And request sqlRequest
    And headers authHeaders
    When method post
    Then status 200
    And match response.content == '#string'

  @regression
  Scenario: Test completion with invalid parameters
    * def auth = authFixtures.login()
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    
    # Test with empty prompt
    * def emptyPromptRequest = {
        prompt: "",
        maxTokens: 100,
        temperature: 0.7
      }
    
    Given path config.endpoints.llm.completions
    And request emptyPromptRequest
    And headers authHeaders
    When method post
    Then status 400
    And match response.error.code == 'INVALID_PROMPT'
    
    # Test with invalid temperature
    * def invalidTempRequest = llmFixtures.generateCompletionRequest({
        temperature: 5.0
      })
    
    Given path config.endpoints.llm.completions
    And request invalidTempRequest
    And headers authHeaders
    When method post
    Then status 400
    And match response.error.code == 'INVALID_TEMPERATURE'
    
    # Test with invalid token count
    * def invalidTokensRequest = llmFixtures.generateCompletionRequest({
        maxTokens: -1
      })
    
    Given path config.endpoints.llm.completions
    And request invalidTokensRequest
    And headers authHeaders
    When method post
    Then status 400
    And match response.error.code == 'INVALID_TOKEN_COUNT'

  @regression
  Scenario: Test completion with unsupported model
    * def auth = authFixtures.login()
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    
    * def unsupportedModelRequest = llmFixtures.generateCompletionRequest({
        provider: "claude",
        model: "non-existent-model"
      })
    
    Given path config.endpoints.llm.completions
    And request unsupportedModelRequest
    And headers authHeaders
    When method post
    Then status 400
    And match response.error.code == 'UNSUPPORTED_MODEL'
    And match response.error.message contains 'non-existent-model'

  @performance
  Scenario: Test completion response time
    * def auth = authFixtures.login()
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    
    * def quickRequest = llmFixtures.generateCompletionRequest({
        prompt: "Hello",
        maxTokens: 10
      })
    
    * def startTime = Date.now()
    
    Given path config.endpoints.llm.completions
    And request quickRequest
    And headers authHeaders
    When method post
    Then status 200
    
    * def endTime = Date.now()
    * def responseTime = endTime - startTime
    
    # Should respond within 5 seconds for simple requests
    * assert responseTime < 5000
    And match response.content == '#string'

  @performance
  Scenario: Test concurrent completion requests
    * def auth = authFixtures.login()
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    
    * def concurrentRequests = []
    * for (var i = 0; i < 5; i++) concurrentRequests.push(llmFixtures.generateCompletionRequest({prompt: "Test " + i}))
    
    * def startTime = Date.now()
    
    * def results = karate.parallel(concurrentRequests, function(request) {
        return karate.call('classpath:llm/create-completion.feature', {
          completionRequest: request,
          authToken: auth.token,
          baseUrl: llmUrl
        });
      })
    
    * def endTime = Date.now()
    * def totalTime = endTime - startTime
    
    # All requests should complete successfully
    * match results.length == 5
    * def successfulResults = results.filter(r => r.responseStatus == 200)
    * match successfulResults.length == 5
    
    # Should complete within reasonable time
    * assert totalTime < 15000

  @regression
  Scenario: Test completion caching
    * def auth = authFixtures.login()
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    
    * def cacheableRequest = llmFixtures.generateCompletionRequest({
        prompt: "What is 2+2?",
        enableCaching: true,
        maxTokens: 10
      })
    
    # First request
    * def startTime1 = Date.now()
    Given path config.endpoints.llm.completions
    And request cacheableRequest
    And headers authHeaders
    When method post
    Then status 200
    * def endTime1 = Date.now()
    * def firstResponseTime = endTime1 - startTime1
    * def firstResponse = response.content
    
    # Second identical request (should be cached)
    * def startTime2 = Date.now()
    Given path config.endpoints.llm.completions
    And request cacheableRequest
    And headers authHeaders
    When method post
    Then status 200
    * def endTime2 = Date.now()
    * def secondResponseTime = endTime2 - startTime2
    * def secondResponse = response.content
    
    # Cached response should be faster and identical
    * assert secondResponseTime < firstResponseTime
    * match firstResponse == secondResponse

  @regression
  Scenario: Test completion with metadata
    * def auth = authFixtures.login()
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    
    * def metadataRequest = llmFixtures.generateCompletionRequest({
        prompt: "Explain quantum computing",
        metadata: {
          userId: auth.user.id,
          sessionId: "test-session-123",
          context: "educational",
          priority: "high"
        }
      })
    
    Given path config.endpoints.llm.completions
    And request metadataRequest
    And headers authHeaders
    When method post
    Then status 200
    And match response.metadata == '#object'
    And match response.metadata.userId == auth.user.id
    And match response.metadata.sessionId == "test-session-123"
    And match response.metadata.context == "educational"