@smoke @regression
Feature: Debate Lifecycle Management Test Suite

  Background:
    * def config = callonce read('classpath:karate-config.js')
    * def authFixtures = callonce read('classpath:fixtures/auth-fixtures.js')
    * def debateFixtures = callonce read('classpath:fixtures/debate-fixtures.js')
    * def controllerUrl = config.serviceUrls.controller
    * url controllerUrl

  @smoke
  Scenario: Create basic debate
    # Login and get auth token
    * def auth = authFixtures.login()
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    
    # Create debate request
    * def debateRequest = debateFixtures.generateDebateRequest({
        topic: "Should AI be regulated?",
        description: "A debate about AI regulation",
        config: {
          format: "OXFORD",
          maxRounds: 3,
          maxParticipants: 4,
          isPublic: true
        }
      })
    
    Given path config.endpoints.debate.create
    And request debateRequest
    And headers authHeaders
    When method post
    Then status 201
    And match response.id == '#string'
    And match response.topic == debateRequest.topic
    And match response.description == debateRequest.description
    And match response.status == 'DRAFT'
    And match response.config.format == 'OXFORD'
    And match response.config.maxRounds == 3
    And match response.config.maxParticipants == 4
    And match response.positions == '#array'
    And match response.positions.length == 2
    And match response.createdAt == '#string'
    And match response.createdBy == '#string'
    And match response.organizationId == '#string'
    
    # Store debate ID for further tests
    * def debateId = response.id

  @smoke
  Scenario: Get debate by ID
    # Create debate first
    * def auth = authFixtures.login()
    * def debate = debateFixtures.createDebate({}, auth.token)
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    
    Given path config.endpoints.debate.base + '/' + debate.id
    And headers authHeaders
    When method get
    Then status 200
    And match response.id == debate.id
    And match response.topic == debate.topic
    And match response.description == debate.description
    And match response.status == debate.status
    And match response.config == '#object'
    And match response.positions == '#array'
    And match response.participants == '#array'
    And match response.rounds == '#array'
    And match response.createdAt == '#string'
    And match response.updatedAt == '#string'

  @smoke
  Scenario: Update debate
    # Create debate first
    * def auth = authFixtures.login()
    * def debate = debateFixtures.createDebate({}, auth.token)
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    
    # Update debate
    * def updateData = {
        topic: debate.topic + " (Updated)",
        description: "Updated description",
        config: {
          format: "STANDARD",
          maxRounds: 5,
          maxParticipants: 6
        }
      }
    
    Given path config.endpoints.debate.base + '/' + debate.id
    And request updateData
    And headers authHeaders
    When method put
    Then status 200
    And match response.id == debate.id
    And match response.topic == updateData.topic
    And match response.description == updateData.description
    And match response.config.format == 'STANDARD'
    And match response.config.maxRounds == 5
    And match response.config.maxParticipants == 6
    And match response.updatedAt != debate.updatedAt

  @regression
  Scenario: Complete debate lifecycle
    # Create debate
    * def auth = authFixtures.login()
    * def debate = debateFixtures.createDebate({
        topic: "Complete lifecycle test",
        config: {
          format: "OXFORD",
          maxRounds: 2,
          maxParticipants: 4
        }
      }, auth.token)
    
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    
    # Create participants
    * def participant1 = authFixtures.register({
        email: config.utils.generateEmail('participant1'),
        name: 'Participant 1'
      })
    * def participant2 = authFixtures.register({
        email: config.utils.generateEmail('participant2'),
        name: 'Participant 2'
      })
    
    # Add participants to debate
    * def participantRequest1 = debateFixtures.generateParticipantRequest(participant1.user.id, 'PRO')
    * def participantRequest2 = debateFixtures.generateParticipantRequest(participant2.user.id, 'CON')
    
    Given path config.endpoints.debate.base + '/' + debate.id + '/participants'
    And request participantRequest1
    And headers authHeaders
    When method post
    Then status 201
    And match response.userId == participant1.user.id
    And match response.position == 'PRO'
    
    Given path config.endpoints.debate.base + '/' + debate.id + '/participants'
    And request participantRequest2
    And headers authHeaders
    When method post
    Then status 201
    And match response.userId == participant2.user.id
    And match response.position == 'CON'
    
    # Start debate
    Given path config.endpoints.debate.base + '/' + debate.id + '/start'
    And headers authHeaders
    When method post
    Then status 200
    And match response.status == 'IN_PROGRESS'
    And match response.startedAt == '#string'
    And match response.currentRound == 1
    
    # Submit responses
    * def response1 = debateFixtures.generateResponseRequest("Pro argument for round 1", {type: "ARGUMENT"})
    * def response2 = debateFixtures.generateResponseRequest("Con argument for round 1", {type: "ARGUMENT"})
    
    # Participant 1 submits response
    Given path config.endpoints.debate.base + '/' + debate.id + '/responses'
    And request response1
    And header Authorization = 'Bearer ' + participant1.token
    And header Content-Type = 'application/json'
    When method post
    Then status 201
    And match response.content == response1.content
    And match response.type == 'ARGUMENT'
    And match response.round == 1
    
    # Participant 2 submits response
    Given path config.endpoints.debate.base + '/' + debate.id + '/responses'
    And request response2
    And header Authorization = 'Bearer ' + participant2.token
    And header Content-Type = 'application/json'
    When method post
    Then status 201
    And match response.content == response2.content
    And match response.type == 'ARGUMENT'
    And match response.round == 1
    
    # Complete debate
    Given path config.endpoints.debate.base + '/' + debate.id + '/complete'
    And headers authHeaders
    When method post
    Then status 200
    And match response.status == 'COMPLETED'
    And match response.completedAt == '#string'
    And match response.result == '#object'

  @regression
  Scenario: List debates with filtering
    # Create multiple debates with different properties
    * def auth = authFixtures.login()
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    
    * def publicDebate = debateFixtures.createDebate({
        topic: "Public debate test",
        config: { isPublic: true, format: "OXFORD" }
      }, auth.token)
    
    * def privateDebate = debateFixtures.createDebate({
        topic: "Private debate test",
        config: { isPublic: false, format: "STANDARD" }
      }, auth.token)
    
    # List all debates
    Given path config.endpoints.debate.base
    And headers authHeaders
    When method get
    Then status 200
    And match response.debates == '#array'
    And match response.debates.length >= 2
    And match response.pagination == '#object'
    
    # Filter by public debates
    Given path config.endpoints.debate.base
    And param isPublic = true
    And headers authHeaders
    When method get
    Then status 200
    And match response.debates == '#array'
    * def publicDebates = response.debates.filter(d => d.config.isPublic == true)
    * match publicDebates.length >= 1
    
    # Filter by format
    Given path config.endpoints.debate.base
    And param format = 'OXFORD'
    And headers authHeaders
    When method get
    Then status 200
    And match response.debates == '#array'
    * def oxfordDebates = response.debates.filter(d => d.config.format == 'OXFORD')
    * match oxfordDebates.length >= 1

  @regression
  Scenario: Delete debate
    # Create debate first
    * def auth = authFixtures.login()
    * def debate = debateFixtures.createDebate({}, auth.token)
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    
    # Delete debate
    Given path config.endpoints.debate.base + '/' + debate.id
    And headers authHeaders
    When method delete
    Then status 204
    
    # Verify debate is deleted
    Given path config.endpoints.debate.base + '/' + debate.id
    And headers authHeaders
    When method get
    Then status 404

  @security
  Scenario: Create debate without authentication
    * def debateRequest = debateFixtures.generateDebateRequest()
    
    Given path config.endpoints.debate.create
    And request debateRequest
    When method post
    Then status 401
    And match response.error.code == 'UNAUTHORIZED'

  @security
  Scenario: Access debate with invalid token
    * def auth = authFixtures.login()
    * def debate = debateFixtures.createDebate({}, auth.token)
    
    Given path config.endpoints.debate.base + '/' + debate.id
    And header Authorization = 'Bearer invalid-token'
    When method get
    Then status 401
    And match response.error.code == 'INVALID_TOKEN'

  @security
  Scenario: Access private debate from different organization
    # Create debate in organization 1
    * def org1Setup = orgFixtures.createOrganizationWithUsers({name: 'Org1'}, 1)
    * def org1AuthHeaders = authFixtures.getAuthHeaders(org1Setup.admin.token)
    * set org1AuthHeaders['X-Organization-Id'] = org1Setup.organization.id
    
    * def privateDebate = debateFixtures.createDebate({
        topic: "Private debate",
        config: { isPublic: false }
      }, org1Setup.admin.token)
    
    # Try to access from organization 2
    * def org2Setup = orgFixtures.createOrganizationWithUsers({name: 'Org2'}, 1)
    * def org2AuthHeaders = authFixtures.getAuthHeaders(org2Setup.admin.token)
    * set org2AuthHeaders['X-Organization-Id'] = org2Setup.organization.id
    
    Given path config.endpoints.debate.base + '/' + privateDebate.id
    And headers org2AuthHeaders
    When method get
    Then status 403
    And match response.error.code == 'FORBIDDEN'

  @regression
  Scenario: Create debate with invalid data
    * def auth = authFixtures.login()
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    
    # Test with missing topic
    * def invalidDebateRequest = {
        description: "Missing topic",
        config: { format: "OXFORD" }
      }
    
    Given path config.endpoints.debate.create
    And request invalidDebateRequest
    And headers authHeaders
    When method post
    Then status 400
    And match response.error.code == 'VALIDATION_ERROR'
    And match response.error.details == '#object'

  @regression
  Scenario: Create debate with invalid configuration
    * def auth = authFixtures.login()
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    
    # Test with invalid format
    * def invalidConfigRequest = debateFixtures.generateDebateRequest({
        config: {
          format: "INVALID_FORMAT",
          maxRounds: -1,
          maxParticipants: 0
        }
      })
    
    Given path config.endpoints.debate.create
    And request invalidConfigRequest
    And headers authHeaders
    When method post
    Then status 400
    And match response.error.code == 'INVALID_CONFIGURATION'

  @performance
  Scenario: Create multiple debates concurrently
    * def auth = authFixtures.login()
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    
    * def debateRequests = []
    * for (var i = 0; i < 10; i++) {
        debateRequests.push(debateFixtures.generateDebateRequest({
          topic: "Performance test debate " + i
        }));
      }
    
    * def startTime = Date.now()
    
    * def results = karate.parallel(debateRequests, function(request) {
        return karate.call('classpath:debate/create-debate.feature', {
          debateRequest: request,
          authToken: auth.token,
          baseUrl: controllerUrl
        });
      })
    
    * def endTime = Date.now()
    * def totalTime = endTime - startTime
    
    # All debates should be created successfully
    * match results.length == 10
    * def successfulResults = results.filter(r => r.responseStatus == 201)
    * match successfulResults.length == 10
    
    # Should complete within reasonable time
    * assert totalTime < 15000

  @performance
  Scenario: Test debate response time
    * def auth = authFixtures.login()
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    
    * def debateRequest = debateFixtures.generateDebateRequest({
        topic: "Performance test debate"
      })
    
    * def startTime = Date.now()
    
    Given path config.endpoints.debate.create
    And request debateRequest
    And headers authHeaders
    When method post
    Then status 201
    
    * def endTime = Date.now()
    * def responseTime = endTime - startTime
    
    # Should respond within 2 seconds
    * assert responseTime < 2000
    And match response.id == '#string'
    And match response.topic == debateRequest.topic