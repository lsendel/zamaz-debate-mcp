@smoke @regression
Feature: LLM Provider Health Test Suite

  Background:
    * def config = callonce read('classpath:karate-config.js')
    * def authFixtures = callonce read('classpath:fixtures/auth-fixtures.js')
    * def llmFixtures = callonce read('classpath:fixtures/llm-fixtures.js')
    * def llmUrl = config.serviceUrls.llm
    * url llmUrl

  @smoke
  Scenario: List available LLM providers
    * def auth = authFixtures.login()
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    
    Given path config.endpoints.llm.providers
    And headers authHeaders
    When method get
    Then status 200
    And match response.providers == '#array'
    And match response.providers.length > 0
    And match response.providers[0].id == '#string'
    And match response.providers[0].name == '#string'
    And match response.providers[0].status == '#string'
    And match response.providers[0].models == '#array'
    And match response.providers[0].capabilities == '#object'
    And match response.providers[0].rateLimit == '#object'
    And match response.providers[0].pricing == '#object'

  @smoke
  Scenario: Check health of specific providers
    * def auth = authFixtures.login()
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    
    # Get list of providers first
    Given path config.endpoints.llm.providers
    And headers authHeaders
    When method get
    Then status 200
    * def providers = response.providers
    * def activeProviders = providers.filter(p => p.status == 'active')
    * match activeProviders.length > 0
    
    # Check health of first active provider
    * def firstProvider = activeProviders[0]
    
    Given path '/api/v1/llm/providers/' + firstProvider.id + '/health'
    And headers authHeaders
    When method get
    Then status 200
    And match response.providerId == firstProvider.id
    And match response.status == '#string'
    And match response.healthy == '#boolean'
    And match response.lastChecked == '#string'
    And match response.responseTime == '#number'
    And match response.details == '#object'
    And match response.details.apiStatus == '#string'
    And match response.details.modelsAvailable == '#array'
    And match response.details.errorRate == '#number'

  @regression
  Scenario: Check health of all providers
    * def auth = authFixtures.login()
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    
    # Get all providers
    Given path config.endpoints.llm.providers
    And headers authHeaders
    When method get
    Then status 200
    * def providers = response.providers
    
    # Check health of each provider
    * def healthResults = []
    * for (var i = 0; i < providers.length; i++) {
        var provider = providers[i];
        var healthCheck = karate.call('classpath:llm/check-provider-health.feature', {
          providerId: provider.id,
          authToken: auth.token,
          baseUrl: llmUrl
        });
        healthResults.push({
          providerId: provider.id,
          healthy: healthCheck.response.healthy,
          responseTime: healthCheck.response.responseTime,
          status: healthCheck.response.status
        });
      }
    
    # At least one provider should be healthy
    * def healthyProviders = healthResults.filter(h => h.healthy == true)
    * match healthyProviders.length > 0
    
    # Response times should be reasonable
    * def fastProviders = healthResults.filter(h => h.responseTime < 5000)
    * match fastProviders.length > 0

  @regression
  Scenario: Get models for each provider
    * def auth = authFixtures.login()
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    
    # Get all providers
    Given path config.endpoints.llm.providers
    And headers authHeaders
    When method get
    Then status 200
    * def providers = response.providers
    
    # Get models for each provider
    * def modelResults = []
    * for (var i = 0; i < providers.length; i++) {
        var provider = providers[i];
        var modelsCheck = karate.call('classpath:llm/get-provider-models.feature', {
          providerId: provider.id,
          authToken: auth.token,
          baseUrl: llmUrl
        });
        if (modelsCheck.responseStatus == 200) {
          modelResults.push({
            providerId: provider.id,
            models: modelsCheck.response.models,
            modelCount: modelsCheck.response.models.length
          });
        }
      }
    
    # At least one provider should have models
    * match modelResults.length > 0
    * def providersWithModels = modelResults.filter(m => m.modelCount > 0)
    * match providersWithModels.length > 0

  @regression
  Scenario: Test provider model details
    * def auth = authFixtures.login()
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    
    # Get Claude provider models
    Given path '/api/v1/llm/providers/claude/models'
    And headers authHeaders
    When method get
    Then status 200
    And match response.providerId == 'claude'
    And match response.models == '#array'
    And match response.models.length > 0
    
    * def claudeModels = response.models
    * def firstModel = claudeModels[0]
    
    # Validate model structure
    And match firstModel.id == '#string'
    And match firstModel.name == '#string'
    And match firstModel.description == '#string'
    And match firstModel.maxTokens == '#number'
    And match firstModel.supportedFeatures == '#array'
    And match firstModel.pricing == '#object'
    And match firstModel.parameters == '#object'
    
    # Check specific Claude models
    * def claudeOpus = claudeModels.filter(m => m.id == 'claude-3-opus')
    * def claudeSonnet = claudeModels.filter(m => m.id == 'claude-3-sonnet')
    * def claudeHaiku = claudeModels.filter(m => m.id == 'claude-3-haiku')
    
    * match claudeOpus.length == 1
    * match claudeSonnet.length == 1
    * match claudeHaiku.length == 1

  @security
  Scenario: Test provider access security
    * def auth = authFixtures.login()
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    
    # Test without authentication
    Given path config.endpoints.llm.providers
    When method get
    Then status 401
    And match response.error.code == 'UNAUTHORIZED'
    
    # Test with invalid token
    Given path config.endpoints.llm.providers
    And header Authorization = 'Bearer invalid-token'
    When method get
    Then status 401
    And match response.error.code == 'INVALID_TOKEN'

  @regression
  Scenario: Test provider health monitoring
    * def auth = authFixtures.login()
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    
    # Get all providers
    Given path config.endpoints.llm.providers
    And headers authHeaders
    When method get
    Then status 200
    * def providers = response.providers
    * def firstProvider = providers[0]
    
    # Check health multiple times to test consistency
    * def healthChecks = []
    * for (var i = 0; i < 3; i++) {
        var healthCheck = karate.call('classpath:llm/check-provider-health.feature', {
          providerId: firstProvider.id,
          authToken: auth.token,
          baseUrl: llmUrl
        });
        healthChecks.push(healthCheck.response);
        java.lang.Thread.sleep(1000);
      }
    
    # All health checks should be consistent
    * match healthChecks.length == 3
    * def healthyChecks = healthChecks.filter(h => h.healthy == true)
    * def unhealthyChecks = healthChecks.filter(h => h.healthy == false)
    
    # Health status should be consistent (all healthy or all unhealthy)
    * assert healthyChecks.length == 3 || unhealthyChecks.length == 3

  @performance
  Scenario: Test provider performance metrics
    * def auth = authFixtures.login()
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    
    # Get provider performance metrics
    Given path '/api/v1/llm/providers/performance'
    And headers authHeaders
    When method get
    Then status 200
    And match response.providers == '#array'
    
    * def performanceMetrics = response.providers
    * match performanceMetrics.length > 0
    
    # Check performance metrics structure
    * def firstMetric = performanceMetrics[0]
    And match firstMetric.providerId == '#string'
    And match firstMetric.averageResponseTime == '#number'
    And match firstMetric.successRate == '#number'
    And match firstMetric.errorRate == '#number'
    And match firstMetric.throughput == '#number'
    And match firstMetric.availability == '#number'
    And match firstMetric.lastUpdated == '#string'
    
    # Performance metrics should be reasonable
    * assert firstMetric.successRate >= 0.0 && firstMetric.successRate <= 1.0
    * assert firstMetric.errorRate >= 0.0 && firstMetric.errorRate <= 1.0
    * assert firstMetric.availability >= 0.0 && firstMetric.availability <= 1.0

  @regression
  Scenario: Test provider configuration
    * def auth = authFixtures.login()
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    
    # Get provider configuration
    Given path '/api/v1/llm/providers/claude/config'
    And headers authHeaders
    When method get
    Then status 200
    And match response.providerId == 'claude'
    And match response.config == '#object'
    And match response.config.apiEndpoint == '#string'
    And match response.config.timeout == '#number'
    And match response.config.retryPolicy == '#object'
    And match response.config.rateLimits == '#object'
    And match response.config.authentication == '#object'
    
    # Rate limits should be properly configured
    * def rateLimits = response.config.rateLimits
    And match rateLimits.requestsPerMinute == '#number'
    And match rateLimits.tokensPerMinute == '#number'
    And match rateLimits.concurrentRequests == '#number'
    
    # Authentication should be configured
    * def authentication = response.config.authentication
    And match authentication.type == '#string'
    And match authentication.configured == true

  @regression
  Scenario: Test provider failover mechanism
    * def auth = authFixtures.login()
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    
    # Get provider failover configuration
    Given path '/api/v1/llm/providers/failover'
    And headers authHeaders
    When method get
    Then status 200
    And match response.enabled == '#boolean'
    And match response.strategy == '#string'
    And match response.providers == '#array'
    
    # Test failover order
    * def failoverProviders = response.providers
    * match failoverProviders.length > 1
    
    # Each provider should have priority and health threshold
    * def firstProvider = failoverProviders[0]
    And match firstProvider.providerId == '#string'
    And match firstProvider.priority == '#number'
    And match firstProvider.healthThreshold == '#number'
    And match firstProvider.enabled == '#boolean'

  @regression
  Scenario: Test provider cost estimation
    * def auth = authFixtures.login()
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    
    # Get cost estimation for different providers
    * def costRequest = {
        prompt: "Explain artificial intelligence in detail.",
        maxTokens: 500,
        providers: ["claude", "openai", "gemini"]
      }
    
    Given path '/api/v1/llm/providers/cost-estimate'
    And request costRequest
    And headers authHeaders
    When method post
    Then status 200
    And match response.estimates == '#array'
    And match response.estimates.length == 3
    
    # Check cost estimation structure
    * def firstEstimate = response.estimates[0]
    And match firstEstimate.providerId == '#string'
    And match firstEstimate.model == '#string'
    And match firstEstimate.inputTokens == '#number'
    And match firstEstimate.outputTokens == '#number'
    And match firstEstimate.totalTokens == '#number'
    And match firstEstimate.inputCost == '#number'
    And match firstEstimate.outputCost == '#number'
    And match firstEstimate.totalCost == '#number'
    And match firstEstimate.currency == '#string'
    
    # Find cheapest option
    * def cheapestOption = response.estimates.sort((a, b) => a.totalCost - b.totalCost)[0]
    * match cheapestOption.providerId == '#string'
    * assert cheapestOption.totalCost >= 0