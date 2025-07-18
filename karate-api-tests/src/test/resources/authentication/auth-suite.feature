@smoke @regression
Feature: Authentication Test Suite

  Background:
    * def config = callonce read('classpath:karate-config.js')
    * def authFixtures = callonce read('classpath:fixtures/auth-fixtures.js')
    * def gatewayUrl = config.serviceUrls.gateway
    * url gatewayUrl

  @smoke
  Scenario: Successful user login
    Given path config.endpoints.auth.login
    And request
      """
      {
        "email": "#(config.testData.defaultUser.email)",
        "password": "#(config.testData.defaultUser.password)",
        "organizationId": "#(config.testData.defaultOrganization)"
      }
      """
    When method post
    Then status 200
    And match response.token == '#string'
    And match response.refreshToken == '#string'
    And match response.user.email == config.testData.defaultUser.email
    And match response.user.id == '#string'
    And match response.user.organizationId == '#string'
    And match response.user.name == '#string'
    And match response.expiresIn == '#number'
    And match response.tokenType == 'Bearer'
    
    # Validate token structure
    * def tokenValid = authFixtures.validateTokenStructure(response.token)
    * match tokenValid == true

  @smoke
  Scenario: User registration with valid data
    * def uniqueEmail = config.utils.generateEmail('test')
    * def uniqueName = 'Test User ' + config.utils.randomString(5)
    
    Given path config.endpoints.auth.register
    And request
      """
      {
        "email": "#(uniqueEmail)",
        "password": "TestPassword123!",
        "name": "#(uniqueName)",
        "organizationId": "#(config.testData.defaultOrganization)"
      }
      """
    When method post
    Then status 201
    And match response.token == '#string'
    And match response.refreshToken == '#string'
    And match response.user.email == uniqueEmail
    And match response.user.name == uniqueName
    And match response.user.id == '#string'
    And match response.user.organizationId == config.testData.defaultOrganization
    And match response.user.role == 'USER'
    And match response.user.active == true

  @smoke
  Scenario: Get current user information
    # Login first
    * def loginResponse = authFixtures.login()
    * def authHeaders = authFixtures.getAuthHeaders(loginResponse.token)
    
    Given path config.endpoints.auth.me
    And headers authHeaders
    When method get
    Then status 200
    And match response.id == '#string'
    And match response.email == config.testData.defaultUser.email
    And match response.name == '#string'
    And match response.organizationId == '#string'
    And match response.role == '#string'
    And match response.active == true
    And match response.createdAt == '#string'
    And match response.lastLoginAt == '#string'

  @regression
  Scenario: Token refresh functionality
    # Login first
    * def loginResponse = authFixtures.login()
    * def originalToken = loginResponse.token
    * def refreshToken = loginResponse.refreshToken
    
    Given path config.endpoints.auth.refresh
    And request
      """
      {
        "refreshToken": "#(refreshToken)"
      }
      """
    When method post
    Then status 200
    And match response.token == '#string'
    And match response.refreshToken == '#string'
    And match response.expiresIn == '#number'
    
    # Verify new token is different from original
    * match response.token != originalToken
    
    # Verify new token is valid
    * def tokenValid = authFixtures.validateTokenStructure(response.token)
    * match tokenValid == true

  @regression
  Scenario: User logout
    # Login first
    * def loginResponse = authFixtures.login()
    * def authHeaders = authFixtures.getAuthHeaders(loginResponse.token)
    
    Given path config.endpoints.auth.logout
    And headers authHeaders
    When method post
    Then status 200
    And match response.message == 'Logout successful'
    
    # Verify token is invalidated
    Given path config.endpoints.auth.me
    And headers authHeaders
    When method get
    Then status 401

  @security
  Scenario: Login with invalid credentials
    Given path config.endpoints.auth.login
    And request
      """
      {
        "email": "invalid@zamaz.com",
        "password": "wrongpassword",
        "organizationId": "#(config.testData.defaultOrganization)"
      }
      """
    When method post
    Then status 401
    And match response.error.code == 'INVALID_CREDENTIALS'
    And match response.error.message == '#string'

  @security
  Scenario: Login with malformed request
    Given path config.endpoints.auth.login
    And request
      """
      {
        "email": "not-an-email",
        "password": ""
      }
      """
    When method post
    Then status 400
    And match response.error.code == 'VALIDATION_ERROR'
    And match response.error.details == '#object'

  @security
  Scenario: Registration with duplicate email
    * def existingEmail = config.testData.defaultUser.email
    
    Given path config.endpoints.auth.register
    And request
      """
      {
        "email": "#(existingEmail)",
        "password": "TestPassword123!",
        "name": "Duplicate User",
        "organizationId": "#(config.testData.defaultOrganization)"
      }
      """
    When method post
    Then status 409
    And match response.error.code == 'EMAIL_ALREADY_EXISTS'
    And match response.error.message == '#string'

  @security
  Scenario: Registration with weak password
    * def uniqueEmail = config.utils.generateEmail('weak')
    
    Given path config.endpoints.auth.register
    And request
      """
      {
        "email": "#(uniqueEmail)",
        "password": "weak",
        "name": "Test User",
        "organizationId": "#(config.testData.defaultOrganization)"
      }
      """
    When method post
    Then status 400
    And match response.error.code == 'WEAK_PASSWORD'
    And match response.error.message == '#string'

  @security
  Scenario: Access protected endpoint without token
    Given path config.endpoints.auth.me
    When method get
    Then status 401
    And match response.error.code == 'UNAUTHORIZED'

  @security
  Scenario: Access protected endpoint with invalid token
    Given path config.endpoints.auth.me
    And header Authorization = 'Bearer invalid-token'
    When method get
    Then status 401
    And match response.error.code == 'INVALID_TOKEN'

  @security
  Scenario: Access protected endpoint with expired token
    * def expiredToken = authFixtures.generateTestToken({exp: 1000000})
    
    Given path config.endpoints.auth.me
    And header Authorization = 'Bearer ' + expiredToken
    When method get
    Then status 401
    And match response.error.code == 'TOKEN_EXPIRED'

  @security
  Scenario: Token refresh with invalid refresh token
    Given path config.endpoints.auth.refresh
    And request
      """
      {
        "refreshToken": "invalid-refresh-token"
      }
      """
    When method post
    Then status 401
    And match response.error.code == 'INVALID_REFRESH_TOKEN'

  @security
  Scenario: Rate limiting on login attempts
    * def invalidCredentials = 
      """
      {
        "email": "test@example.com",
        "password": "wrongpassword",
        "organizationId": "#(config.testData.defaultOrganization)"
      }
      """
    
    # Make multiple failed login attempts
    * def attempts = []
    * for (var i = 0; i < config.security.rateLimitThreshold + 1; i++) attempts.push(i)
    
    # First attempts should return 401
    Given path config.endpoints.auth.login
    And request invalidCredentials
    When method post
    Then status 401
    
    # After rate limit threshold, should return 429
    * def lastAttempt = karate.repeat(config.security.rateLimitThreshold, function() { return karate.call('classpath:authentication/login.feature', {loginRequest: invalidCredentials, baseUrl: gatewayUrl}) })
    
    Given path config.endpoints.auth.login
    And request invalidCredentials
    When method post
    Then status 429
    And match response.error.code == 'RATE_LIMIT_EXCEEDED'

  @integration
  Scenario: Organization switching
    # Create two organizations with users
    * def org1 = authFixtures.createOrganizationWithAdmin('org1')
    * def org2 = authFixtures.createOrganizationWithAdmin('org2')
    
    # Login to first organization
    * def authHeaders1 = authFixtures.getAuthHeaders(org1.admin.token)
    * set authHeaders1['X-Organization-Id'] = org1.organization.id
    
    # Verify access to first organization
    Given path config.endpoints.auth.me
    And headers authHeaders1
    When method get
    Then status 200
    And match response.organizationId == org1.organization.id
    
    # Switch to second organization
    Given path config.endpoints.auth.switchOrganization
    And headers authHeaders1
    And request
      """
      {
        "organizationId": "#(org2.organization.id)"
      }
      """
    When method post
    Then status 200
    And match response.token == '#string'
    And match response.user.organizationId == org2.organization.id