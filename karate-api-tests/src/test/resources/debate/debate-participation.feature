@smoke @regression
Feature: Debate Participation Test Suite

  Background:
    * def config = callonce read('classpath:karate-config.js')
    * def authFixtures = callonce read('classpath:fixtures/auth-fixtures.js')
    * def debateFixtures = callonce read('classpath:fixtures/debate-fixtures.js')
    * def controllerUrl = config.serviceUrls.controller
    * url controllerUrl

  @smoke
  Scenario: Add participant to debate
    # Create debate and user
    * def auth = authFixtures.login()
    * def debate = debateFixtures.createDebate({}, auth.token)
    * def participant = authFixtures.register({
        email: config.utils.generateEmail('participant'),
        name: 'Test Participant'
      })
    
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    * def participantRequest = debateFixtures.generateParticipantRequest(participant.user.id, 'PRO')
    
    Given path config.endpoints.debate.base + '/' + debate.id + '/participants'
    And request participantRequest
    And headers authHeaders
    When method post
    Then status 201
    And match response.id == '#string'
    And match response.userId == participant.user.id
    And match response.position == 'PRO'
    And match response.role == 'DEBATER'
    And match response.status == 'ACTIVE'
    And match response.joinedAt == '#string'
    And match response.debateId == debate.id

  @smoke
  Scenario: Remove participant from debate
    # Create debate with participant
    * def auth = authFixtures.login()
    * def debate = debateFixtures.createDebate({}, auth.token)
    * def participant = authFixtures.register({
        email: config.utils.generateEmail('participant'),
        name: 'Test Participant'
      })
    
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    * def addedParticipant = debateFixtures.addParticipant(debate.id, {
        userId: participant.user.id,
        position: 'PRO'
      }, auth.token)
    
    # Remove participant
    Given path config.endpoints.debate.base + '/' + debate.id + '/participants/' + addedParticipant.id
    And headers authHeaders
    When method delete
    Then status 200
    And match response.id == addedParticipant.id
    And match response.status == 'REMOVED'
    And match response.removedAt == '#string'

  @regression
  Scenario: Add multiple participants to debate
    # Create debate
    * def auth = authFixtures.login()
    * def debate = debateFixtures.createDebate({
        config: { maxParticipants: 6 }
      }, auth.token)
    
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    
    # Create and add multiple participants
    * def participants = []
    * for (var i = 0; i < 4; i++) {
        var participant = authFixtures.register({
          email: config.utils.generateEmail('participant' + i),
          name: 'Participant ' + i
        });
        participants.push(participant);
      }
    
    # Add participants with different positions
    * def addedParticipants = []
    * for (var i = 0; i < participants.length; i++) {
        var position = i < 2 ? 'PRO' : 'CON';
        var participantRequest = debateFixtures.generateParticipantRequest(participants[i].user.id, position);
        
        var addResponse = karate.call('classpath:debate/add-participant.feature', {
          debateId: debate.id,
          participantRequest: participantRequest,
          authToken: auth.token,
          baseUrl: controllerUrl
        });
        
        addedParticipants.push(addResponse.response);
      }
    
    # Verify all participants were added
    * match addedParticipants.length == 4
    * def proParticipants = addedParticipants.filter(p => p.position == 'PRO')
    * def conParticipants = addedParticipants.filter(p => p.position == 'CON')
    * match proParticipants.length == 2
    * match conParticipants.length == 2

  @regression
  Scenario: Test participant role management
    # Create debate
    * def auth = authFixtures.login()
    * def debate = debateFixtures.createDebate({}, auth.token)
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    
    # Add participant as debater
    * def participant = authFixtures.register({
        email: config.utils.generateEmail('participant'),
        name: 'Test Participant'
      })
    
    * def debaterRequest = debateFixtures.generateParticipantRequest(participant.user.id, 'PRO', {
        role: 'DEBATER'
      })
    
    Given path config.endpoints.debate.base + '/' + debate.id + '/participants'
    And request debaterRequest
    And headers authHeaders
    When method post
    Then status 201
    And match response.role == 'DEBATER'
    * def participantId = response.id
    
    # Update participant role to observer
    * def roleUpdateRequest = { role: 'OBSERVER' }
    
    Given path config.endpoints.debate.base + '/' + debate.id + '/participants/' + participantId + '/role'
    And request roleUpdateRequest
    And headers authHeaders
    When method put
    Then status 200
    And match response.role == 'OBSERVER'
    And match response.updatedAt == '#string'

  @regression
  Scenario: Test participant position switching
    # Create debate
    * def auth = authFixtures.login()
    * def debate = debateFixtures.createDebate({}, auth.token)
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    
    # Add participant with PRO position
    * def participant = authFixtures.register({
        email: config.utils.generateEmail('participant'),
        name: 'Test Participant'
      })
    
    * def participantRequest = debateFixtures.generateParticipantRequest(participant.user.id, 'PRO')
    
    Given path config.endpoints.debate.base + '/' + debate.id + '/participants'
    And request participantRequest
    And headers authHeaders
    When method post
    Then status 201
    And match response.position == 'PRO'
    * def participantId = response.id
    
    # Switch to CON position
    * def positionUpdateRequest = { position: 'CON' }
    
    Given path config.endpoints.debate.base + '/' + debate.id + '/participants/' + participantId + '/position'
    And request positionUpdateRequest
    And headers authHeaders
    When method put
    Then status 200
    And match response.position == 'CON'
    And match response.updatedAt == '#string'

  @regression
  Scenario: Test participant capacity limits
    # Create debate with low participant limit
    * def auth = authFixtures.login()
    * def debate = debateFixtures.createDebate({
        config: { maxParticipants: 2 }
      }, auth.token)
    
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    
    # Add participants up to the limit
    * def participant1 = authFixtures.register({
        email: config.utils.generateEmail('participant1'),
        name: 'Participant 1'
      })
    * def participant2 = authFixtures.register({
        email: config.utils.generateEmail('participant2'),
        name: 'Participant 2'
      })
    * def participant3 = authFixtures.register({
        email: config.utils.generateEmail('participant3'),
        name: 'Participant 3'
      })
    
    # Add first participant
    * def participantRequest1 = debateFixtures.generateParticipantRequest(participant1.user.id, 'PRO')
    Given path config.endpoints.debate.base + '/' + debate.id + '/participants'
    And request participantRequest1
    And headers authHeaders
    When method post
    Then status 201
    
    # Add second participant
    * def participantRequest2 = debateFixtures.generateParticipantRequest(participant2.user.id, 'CON')
    Given path config.endpoints.debate.base + '/' + debate.id + '/participants'
    And request participantRequest2
    And headers authHeaders
    When method post
    Then status 201
    
    # Try to add third participant (should fail)
    * def participantRequest3 = debateFixtures.generateParticipantRequest(participant3.user.id, 'PRO')
    Given path config.endpoints.debate.base + '/' + debate.id + '/participants'
    And request participantRequest3
    And headers authHeaders
    When method post
    Then status 400
    And match response.error.code == 'PARTICIPANT_LIMIT_EXCEEDED'

  @regression
  Scenario: Test duplicate participant prevention
    # Create debate
    * def auth = authFixtures.login()
    * def debate = debateFixtures.createDebate({}, auth.token)
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    
    # Add participant
    * def participant = authFixtures.register({
        email: config.utils.generateEmail('participant'),
        name: 'Test Participant'
      })
    
    * def participantRequest = debateFixtures.generateParticipantRequest(participant.user.id, 'PRO')
    
    Given path config.endpoints.debate.base + '/' + debate.id + '/participants'
    And request participantRequest
    And headers authHeaders
    When method post
    Then status 201
    
    # Try to add same participant again
    Given path config.endpoints.debate.base + '/' + debate.id + '/participants'
    And request participantRequest
    And headers authHeaders
    When method post
    Then status 409
    And match response.error.code == 'PARTICIPANT_ALREADY_EXISTS'

  @regression
  Scenario: List debate participants
    # Create debate with multiple participants
    * def auth = authFixtures.login()
    * def setup = debateFixtures.createDebateWithParticipants({}, 4, auth.token)
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    
    # List all participants
    Given path config.endpoints.debate.base + '/' + setup.debate.id + '/participants'
    And headers authHeaders
    When method get
    Then status 200
    And match response.participants == '#array'
    And match response.participants.length == 4
    And match response.participants[0].id == '#string'
    And match response.participants[0].userId == '#string'
    And match response.participants[0].position == '#string'
    And match response.participants[0].role == '#string'
    And match response.participants[0].status == '#string'
    
    # Filter by position
    Given path config.endpoints.debate.base + '/' + setup.debate.id + '/participants'
    And param position = 'PRO'
    And headers authHeaders
    When method get
    Then status 200
    And match response.participants == '#array'
    * def proParticipants = response.participants.filter(p => p.position == 'PRO')
    * match proParticipants.length == 2

  @security
  Scenario: Test participant access control
    # Create debate
    * def auth = authFixtures.login()
    * def debate = debateFixtures.createDebate({}, auth.token)
    
    # Try to add participant without authentication
    * def participant = authFixtures.register({
        email: config.utils.generateEmail('participant'),
        name: 'Test Participant'
      })
    
    * def participantRequest = debateFixtures.generateParticipantRequest(participant.user.id, 'PRO')
    
    Given path config.endpoints.debate.base + '/' + debate.id + '/participants'
    And request participantRequest
    When method post
    Then status 401
    And match response.error.code == 'UNAUTHORIZED'

  @security
  Scenario: Test participant modification security
    # Create debate with participant
    * def auth = authFixtures.login()
    * def debate = debateFixtures.createDebate({}, auth.token)
    * def participant = authFixtures.register({
        email: config.utils.generateEmail('participant'),
        name: 'Test Participant'
      })
    
    * def addedParticipant = debateFixtures.addParticipant(debate.id, {
        userId: participant.user.id,
        position: 'PRO'
      }, auth.token)
    
    # Try to modify participant with different user's token
    * def otherUser = authFixtures.register({
        email: config.utils.generateEmail('other'),
        name: 'Other User'
      })
    * def otherAuthHeaders = authFixtures.getAuthHeaders(otherUser.token)
    
    * def roleUpdateRequest = { role: 'OBSERVER' }
    
    Given path config.endpoints.debate.base + '/' + debate.id + '/participants/' + addedParticipant.id + '/role'
    And request roleUpdateRequest
    And headers otherAuthHeaders
    When method put
    Then status 403
    And match response.error.code == 'FORBIDDEN'

  @regression
  Scenario: Test participant notification preferences
    # Create debate
    * def auth = authFixtures.login()
    * def debate = debateFixtures.createDebate({}, auth.token)
    * def authHeaders = authFixtures.getAuthHeaders(auth.token)
    
    # Add participant with notification preferences
    * def participant = authFixtures.register({
        email: config.utils.generateEmail('participant'),
        name: 'Test Participant'
      })
    
    * def participantRequest = debateFixtures.generateParticipantRequest(participant.user.id, 'PRO', {
        notificationPreferences: {
          emailNotifications: true,
          pushNotifications: false,
          newResponses: true,
          roundUpdates: true,
          debateCompletion: true
        }
      })
    
    Given path config.endpoints.debate.base + '/' + debate.id + '/participants'
    And request participantRequest
    And headers authHeaders
    When method post
    Then status 201
    And match response.notificationPreferences == '#object'
    And match response.notificationPreferences.emailNotifications == true
    And match response.notificationPreferences.pushNotifications == false
    * def participantId = response.id
    
    # Update notification preferences
    * def notificationUpdateRequest = {
        notificationPreferences: {
          emailNotifications: false,
          pushNotifications: true,
          newResponses: false,
          roundUpdates: true,
          debateCompletion: true
        }
      }
    
    Given path config.endpoints.debate.base + '/' + debate.id + '/participants/' + participantId + '/notifications'
    And request notificationUpdateRequest
    And headers authHeaders
    When method put
    Then status 200
    And match response.notificationPreferences.emailNotifications == false
    And match response.notificationPreferences.pushNotifications == true

  @performance
  Scenario: Test concurrent participant addition
    # Create debate
    * def auth = authFixtures.login()
    * def debate = debateFixtures.createDebate({
        config: { maxParticipants: 20 }
      }, auth.token)
    
    # Create multiple participants
    * def participants = []
    * for (var i = 0; i < 10; i++) {
        var participant = authFixtures.register({
          email: config.utils.generateEmail('participant' + i),
          name: 'Participant ' + i
        });
        participants.push(participant);
      }
    
    # Add participants concurrently
    * def participantRequests = []
    * for (var i = 0; i < participants.length; i++) {
        var position = i < 5 ? 'PRO' : 'CON';
        var request = debateFixtures.generateParticipantRequest(participants[i].user.id, position);
        participantRequests.push(request);
      }
    
    * def startTime = Date.now()
    
    * def results = karate.parallel(participantRequests, function(request) {
        return karate.call('classpath:debate/add-participant.feature', {
          debateId: debate.id,
          participantRequest: request,
          authToken: auth.token,
          baseUrl: controllerUrl
        });
      })
    
    * def endTime = Date.now()
    * def totalTime = endTime - startTime
    
    # All participants should be added successfully
    * match results.length == 10
    * def successfulResults = results.filter(r => r.responseStatus == 201)
    * match successfulResults.length == 10
    
    # Should complete within reasonable time
    * assert totalTime < 10000