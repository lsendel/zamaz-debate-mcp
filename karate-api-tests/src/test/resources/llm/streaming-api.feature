@smoke @regression @streaming
Feature: LLM Streaming API Test Suite

  Background:
    * def config = callonce read('classpath:karate-config.js')
    * def authFixtures = callonce read('classpath:fixtures/auth-fixtures.js')
    * def llmFixtures = callonce read('classpath:fixtures/llm-fixtures.js')
    * def llmUrl = config.serviceUrls.llm
    * url llmUrl

  @smoke @streaming
  Scenario: Create basic streaming completion
    * def auth = authFixtures.login()
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    
    * def streamingRequest = llmFixtures.generateStreamingRequest({
        prompt: "Write a short story about a robot learning to paint.",
        maxTokens: 200,
        temperature: 0.8
      })
    
    Given path config.endpoints.llm.stream
    And request streamingRequest
    And headers authHeaders
    And header Accept = 'text/event-stream'
    When method post
    Then status 200
    And match response.streamId == '#string'
    And match response.status == 'streaming'
    And match response.provider == '#string'
    And match response.model == '#string'
    And match response.estimated_tokens == '#number'
    
    # Store stream ID for follow-up operations
    * def streamId = response.streamId

  @regression @streaming
  Scenario: Test streaming completion with different models
    * def auth = authFixtures.login()
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    
    # Test Claude streaming
    * def claudeStreamRequest = llmFixtures.generateStreamingRequest({
        prompt: "Explain the concept of machine learning step by step.",
        provider: "claude",
        model: "claude-3-sonnet",
        maxTokens: 300
      })
    
    Given path config.endpoints.llm.stream
    And request claudeStreamRequest
    And headers authHeaders
    And header Accept = 'text/event-stream'
    When method post
    Then status 200
    And match response.provider == 'claude'
    And match response.model == 'claude-3-sonnet'
    And match response.streamId == '#string'
    
    # Test OpenAI streaming
    * def openaiStreamRequest = llmFixtures.generateStreamingRequest({
        prompt: "Explain the concept of machine learning step by step.",
        provider: "openai",
        model: "gpt-4",
        maxTokens: 300
      })
    
    Given path config.endpoints.llm.stream
    And request openaiStreamRequest
    And headers authHeaders
    And header Accept = 'text/event-stream'
    When method post
    Then status 200
    And match response.provider == 'openai'
    And match response.model == 'gpt-4'
    And match response.streamId == '#string'

  @regression @streaming
  Scenario: Test streaming completion monitoring
    * def auth = authFixtures.login()
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    
    # Start streaming completion
    * def streamingRequest = llmFixtures.generateStreamingRequest({
        prompt: "Write a detailed analysis of renewable energy sources.",
        maxTokens: 500
      })
    
    Given path config.endpoints.llm.stream
    And request streamingRequest
    And headers authHeaders
    And header Accept = 'text/event-stream'
    When method post
    Then status 200
    * def streamId = response.streamId
    
    # Monitor stream status
    Given path '/api/v1/llm/streams/' + streamId
    And headers authHeaders
    When method get
    Then status 200
    And match response.streamId == streamId
    And match response.status == '#string'
    And match response.chunks_received == '#number'
    And match response.total_tokens == '#number'
    And match response.started_at == '#string'
    And match response.progress == '#object'

  @regression @streaming
  Scenario: Test streaming completion cancellation
    * def auth = authFixtures.login()
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    
    # Start streaming completion
    * def streamingRequest = llmFixtures.generateStreamingRequest({
        prompt: "Write a very long essay about the history of artificial intelligence.",
        maxTokens: 1000
      })
    
    Given path config.endpoints.llm.stream
    And request streamingRequest
    And headers authHeaders
    And header Accept = 'text/event-stream'
    When method post
    Then status 200
    * def streamId = response.streamId
    
    # Cancel the stream
    Given path '/api/v1/llm/streams/' + streamId + '/cancel'
    And headers authHeaders
    When method post
    Then status 200
    And match response.streamId == streamId
    And match response.status == 'cancelled'
    And match response.reason == 'user_cancelled'
    
    # Verify stream is cancelled
    Given path '/api/v1/llm/streams/' + streamId
    And headers authHeaders
    When method get
    Then status 200
    And match response.status == 'cancelled'

  @performance @streaming
  Scenario: Test streaming performance metrics
    * def auth = authFixtures.login()
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    
    * def performanceRequest = llmFixtures.generateStreamingRequest({
        prompt: "Generate a creative story with dialogue and descriptions.",
        maxTokens: 400,
        temperature: 0.9
      })
    
    * def startTime = Date.now()
    
    Given path config.endpoints.llm.stream
    And request performanceRequest
    And headers authHeaders
    And header Accept = 'text/event-stream'
    When method post
    Then status 200
    
    * def initTime = Date.now() - startTime
    * def streamId = response.streamId
    
    # Stream initialization should be fast
    * assert initTime < 2000
    
    # Monitor streaming performance
    * def streamingResult = llmFixtures.waitForStreamingCompletion(streamId, 30000)
    * match streamingResult.completed == true
    * match streamingResult.totalChunks > 0
    * assert streamingResult.duration < 30000

  @security @streaming
  Scenario: Test streaming security measures
    * def auth = authFixtures.login()
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    
    # Test streaming without authentication
    * def streamingRequest = llmFixtures.generateStreamingRequest()
    
    Given path config.endpoints.llm.stream
    And request streamingRequest
    And header Accept = 'text/event-stream'
    When method post
    Then status 401
    And match response.error.code == 'UNAUTHORIZED'
    
    # Test streaming with invalid token
    Given path config.endpoints.llm.stream
    And request streamingRequest
    And header Authorization = 'Bearer invalid-token'
    And header Accept = 'text/event-stream'
    When method post
    Then status 401
    And match response.error.code == 'INVALID_TOKEN'

  @regression @streaming
  Scenario: Test streaming error handling
    * def auth = authFixtures.login()
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    
    # Test streaming with invalid model
    * def invalidModelRequest = llmFixtures.generateStreamingRequest({
        provider: "claude",
        model: "invalid-model-name"
      })
    
    Given path config.endpoints.llm.stream
    And request invalidModelRequest
    And headers authHeaders
    And header Accept = 'text/event-stream'
    When method post
    Then status 400
    And match response.error.code == 'UNSUPPORTED_MODEL'
    
    # Test streaming with invalid parameters
    * def invalidParamsRequest = llmFixtures.generateStreamingRequest({
        prompt: "",
        maxTokens: -1
      })
    
    Given path config.endpoints.llm.stream
    And request invalidParamsRequest
    And headers authHeaders
    And header Accept = 'text/event-stream'
    When method post
    Then status 400
    And match response.error.code == 'INVALID_PARAMETERS'

  @regression @streaming
  Scenario: Test streaming rate limiting
    * def auth = authFixtures.login()
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    
    * def rateLimitRequests = []
    * for (var i = 0; i < 20; i++) {
        rateLimitRequests.push(llmFixtures.generateStreamingRequest({
          prompt: "Test rate limiting " + i,
          maxTokens: 10
        }));
      }
    
    * def results = karate.parallel(rateLimitRequests, function(request) {
        return karate.call('classpath:llm/create-streaming-completion.feature', {
          streamingRequest: request,
          authToken: auth.token,
          baseUrl: llmUrl
        });
      })
    
    # Some requests should be rate limited
    * def rateLimitedResults = results.filter(r => r.responseStatus == 429)
    * assert rateLimitedResults.length > 0
    
    # Rate limited responses should have proper error message
    * def rateLimitedResponse = rateLimitedResults[0]
    * match rateLimitedResponse.response.error.code == 'RATE_LIMIT_EXCEEDED'

  @regression @streaming
  Scenario: Test streaming with complex prompts
    * def auth = authFixtures.login()
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    * def testPrompts = llmFixtures.generateTestPrompts()
    
    # Test streaming with code generation
    * def codeRequest = llmFixtures.generateStreamingRequest({
        prompt: testPrompts.code,
        maxTokens: 300,
        temperature: 0.3
      })
    
    Given path config.endpoints.llm.stream
    And request codeRequest
    And headers authHeaders
    And header Accept = 'text/event-stream'
    When method post
    Then status 200
    And match response.streamId == '#string'
    
    # Test streaming with multilingual prompt
    * def multilingualRequest = llmFixtures.generateStreamingRequest({
        prompt: testPrompts.multilingual,
        maxTokens: 200,
        temperature: 0.5
      })
    
    Given path config.endpoints.llm.stream
    And request multilingualRequest
    And headers authHeaders
    And header Accept = 'text/event-stream'
    When method post
    Then status 200
    And match response.streamId == '#string'

  @performance @streaming
  Scenario: Test concurrent streaming requests
    * def auth = authFixtures.login()
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    
    * def concurrentStreams = []
    * for (var i = 0; i < 3; i++) {
        concurrentStreams.push(llmFixtures.generateStreamingRequest({
          prompt: "Generate a creative story about space exploration " + i,
          maxTokens: 150
        }));
      }
    
    * def startTime = Date.now()
    
    * def results = karate.parallel(concurrentStreams, function(request) {
        return karate.call('classpath:llm/create-streaming-completion.feature', {
          streamingRequest: request,
          authToken: auth.token,
          baseUrl: llmUrl
        });
      })
    
    * def endTime = Date.now()
    * def totalTime = endTime - startTime
    
    # All streams should start successfully
    * match results.length == 3
    * def successfulStreams = results.filter(r => r.responseStatus == 200)
    * match successfulStreams.length == 3
    
    # Concurrent streams should start quickly
    * assert totalTime < 5000

  @regression @streaming
  Scenario: Test streaming completion with metadata tracking
    * def auth = authFixtures.login()
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    
    * def metadataRequest = llmFixtures.generateStreamingRequest({
        prompt: "Explain quantum computing in simple terms.",
        maxTokens: 300,
        metadata: {
          userId: auth.user.id,
          sessionId: "streaming-session-123",
          context: "educational-streaming",
          trackingId: "stream-test-" + Date.now()
        }
      })
    
    Given path config.endpoints.llm.stream
    And request metadataRequest
    And headers authHeaders
    And header Accept = 'text/event-stream'
    When method post
    Then status 200
    And match response.streamId == '#string'
    And match response.metadata == '#object'
    And match response.metadata.userId == auth.user.id
    And match response.metadata.sessionId == "streaming-session-123"
    
    * def streamId = response.streamId
    
    # Check metadata is preserved in stream status
    Given path '/api/v1/llm/streams/' + streamId
    And headers authHeaders
    When method get
    Then status 200
    And match response.metadata.context == "educational-streaming"
    And match response.metadata.trackingId == '#string'