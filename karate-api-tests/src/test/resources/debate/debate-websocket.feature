@smoke @regression @websocket
Feature: Debate WebSocket Real-time Communication Test Suite

  Background:
    * def config = callonce read('classpath:karate-config.js')
    * def authFixtures = callonce read('classpath:fixtures/auth-fixtures.js')
    * def debateFixtures = callonce read('classpath:fixtures/debate-fixtures.js')
    * def controllerUrl = config.serviceUrls.controller
    * url controllerUrl

  @smoke @websocket
  Scenario: Connect to debate WebSocket
    # Create debate and participant
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
    
    # Start the debate
    * def startedDebate = debateFixtures.startDebate(debate.id, auth.token)
    
    # Connect to WebSocket
    * def wsConnection = debateFixtures.connectToDebateWebSocket(debate.id, participant.token)
    
    # Verify connection
    * match wsConnection.connected == true
    * match wsConnection.debateId == debate.id
    * match wsConnection.url == '#string'
    
    # Test sending a message
    * def testMessage = {
        type: 'participant.ready',
        participantId: addedParticipant.id,
        data: { ready: true }
      }
    
    * wsConnection.sendMessage(testMessage)
    * match wsConnection.messages.length == 1
    * match wsConnection.messages[0].type == 'outgoing'
    * match wsConnection.messages[0].message.type == 'participant.ready'
    
    # Test receiving a message
    * def receivedMessage = wsConnection.receiveMessage()
    * match receivedMessage.type == 'debate.update'
    * match receivedMessage.debateId == debate.id
    * match receivedMessage.data == '#object'
    
    # Disconnect
    * wsConnection.disconnect()
    * match wsConnection.connected == false

  @regression @websocket
  Scenario: Test WebSocket message types
    # Create debate with participants
    * def auth = authFixtures.login()
    * def setup = debateFixtures.createDebateWithParticipants({}, 2, auth.token)
    * def debate = setup.debate
    * def participants = setup.participants
    
    # Start debate
    * def startedDebate = debateFixtures.startDebate(debate.id, auth.token)
    
    # Connect participants to WebSocket
    * def wsConnections = []
    * for (var i = 0; i < participants.length; i++) {
        var connection = debateFixtures.connectToDebateWebSocket(debate.id, participants[i].auth.token);
        wsConnections.push(connection);
      }
    
    # Test different message types
    * def messageTypes = [
        {
          type: 'participant.typing',
          data: { participantId: participants[0].participant.id, typing: true }
        },
        {
          type: 'response.submitted',
          data: { responseId: 'test-response-id', content: 'Test response' }
        },
        {
          type: 'round.updated',
          data: { currentRound: 2, timeRemaining: 300 }
        },
        {
          type: 'debate.paused',
          data: { reason: 'moderator_pause', pausedBy: 'moderator-id' }
        }
      ]
    
    # Send different message types
    * for (var i = 0; i < messageTypes.length; i++) {
        wsConnections[0].sendMessage(messageTypes[i]);
      }
    
    # Verify messages were sent
    * match wsConnections[0].messages.length == messageTypes.length
    * for (var i = 0; i < messageTypes.length; i++) {
        match wsConnections[0].messages[i].message.type == messageTypes[i].type;
      }
    
    # Cleanup connections
    * for (var i = 0; i < wsConnections.length; i++) {
        wsConnections[i].disconnect();
      }

  @regression @websocket
  Scenario: Test WebSocket authentication
    # Create debate
    * def auth = authFixtures.login()
    * def debate = debateFixtures.createDebate({}, auth.token)
    
    # Try to connect without authentication
    Given path '/api/v1/debates/' + debate.id + '/ws'
    When method get
    Then status 401
    And match response.error.code == 'UNAUTHORIZED'
    
    # Try to connect with invalid token
    Given path '/api/v1/debates/' + debate.id + '/ws'
    And header Authorization = 'Bearer invalid-token'
    When method get
    Then status 401
    And match response.error.code == 'INVALID_TOKEN'
    
    # Connect with valid token
    * def participant = authFixtures.register({
        email: config.utils.generateEmail('participant'),
        name: 'Test Participant'
      })
    
    * def addedParticipant = debateFixtures.addParticipant(debate.id, {
        userId: participant.user.id,
        position: 'PRO'
      }, auth.token)
    
    * def wsConnection = debateFixtures.connectToDebateWebSocket(debate.id, participant.token)
    * match wsConnection.connected == true
    * wsConnection.disconnect()

  @regression @websocket
  Scenario: Test WebSocket connection limits
    # Create debate
    * def auth = authFixtures.login()
    * def debate = debateFixtures.createDebate({}, auth.token)
    
    # Create multiple participants
    * def participants = []
    * for (var i = 0; i < 5; i++) {
        var participant = authFixtures.register({
          email: config.utils.generateEmail('participant' + i),
          name: 'Participant ' + i
        });
        
        var addedParticipant = debateFixtures.addParticipant(debate.id, {
          userId: participant.user.id,
          position: i < 3 ? 'PRO' : 'CON'
        }, auth.token);
        
        participants.push({ auth: participant, participant: addedParticipant });
      }
    
    # Connect all participants
    * def wsConnections = []
    * for (var i = 0; i < participants.length; i++) {
        var connection = debateFixtures.connectToDebateWebSocket(debate.id, participants[i].auth.token);
        wsConnections.push(connection);
      }
    
    # Verify all connections are established
    * match wsConnections.length == 5
    * for (var i = 0; i < wsConnections.length; i++) {
        match wsConnections[i].connected == true;
      }
    
    # Cleanup
    * for (var i = 0; i < wsConnections.length; i++) {
        wsConnections[i].disconnect();
      }

  @regression @websocket
  Scenario: Test WebSocket message broadcasting
    # Create debate with participants
    * def auth = authFixtures.login()
    * def setup = debateFixtures.createDebateWithParticipants({}, 3, auth.token)
    * def debate = setup.debate
    * def participants = setup.participants
    
    # Start debate
    * def startedDebate = debateFixtures.startDebate(debate.id, auth.token)
    
    # Connect all participants
    * def wsConnections = []
    * for (var i = 0; i < participants.length; i++) {
        var connection = debateFixtures.connectToDebateWebSocket(debate.id, participants[i].auth.token);
        wsConnections.push(connection);
      }
    
    # Submit a response (should broadcast to all connected participants)
    * def responseRequest = debateFixtures.generateResponseRequest("Test broadcast response")
    * def submittedResponse = debateFixtures.submitResponse(debate.id, responseRequest, participants[0].auth.token)
    
    # Verify all connections received the broadcast
    * for (var i = 0; i < wsConnections.length; i++) {
        var receivedMessage = wsConnections[i].receiveMessage();
        match receivedMessage.type == 'response.submitted';
        match receivedMessage.data.responseId == submittedResponse.id;
      }
    
    # Cleanup
    * for (var i = 0; i < wsConnections.length; i++) {
        wsConnections[i].disconnect();
      }

  @performance @websocket
  Scenario: Test WebSocket performance under load
    # Create debate with many participants
    * def auth = authFixtures.login()
    * def debate = debateFixtures.createDebate({
        config: { maxParticipants: 50 }
      }, auth.token)
    
    # Create multiple participants
    * def participants = []
    * for (var i = 0; i < 20; i++) {
        var participant = authFixtures.register({
          email: config.utils.generateEmail('participant' + i),
          name: 'Participant ' + i
        });
        
        var addedParticipant = debateFixtures.addParticipant(debate.id, {
          userId: participant.user.id,
          position: i < 10 ? 'PRO' : 'CON'
        }, auth.token);
        
        participants.push({ auth: participant, participant: addedParticipant });
      }
    
    # Connect all participants simultaneously
    * def startTime = Date.now()
    
    * def wsConnections = []
    * for (var i = 0; i < participants.length; i++) {
        var connection = debateFixtures.connectToDebateWebSocket(debate.id, participants[i].auth.token);
        wsConnections.push(connection);
      }
    
    * def connectionTime = Date.now() - startTime
    
    # Verify all connections are established quickly
    * match wsConnections.length == 20
    * assert connectionTime < 5000  # Should connect within 5 seconds
    
    # Test message broadcasting performance
    * def broadcastStartTime = Date.now()
    
    * def broadcastMessage = {
        type: 'test.broadcast',
        data: { message: 'Performance test broadcast' }
      }
    
    * wsConnections[0].sendMessage(broadcastMessage)
    
    # Verify all connections receive the message
    * for (var i = 1; i < wsConnections.length; i++) {
        var receivedMessage = wsConnections[i].receiveMessage();
        match receivedMessage.type == 'test.broadcast';
      }
    
    * def broadcastTime = Date.now() - broadcastStartTime
    * assert broadcastTime < 2000  # Should broadcast within 2 seconds
    
    # Cleanup
    * for (var i = 0; i < wsConnections.length; i++) {
        wsConnections[i].disconnect();
      }

  @regression @websocket
  Scenario: Test WebSocket error handling
    # Create debate
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
    
    # Connect to WebSocket
    * def wsConnection = debateFixtures.connectToDebateWebSocket(debate.id, participant.token)
    
    # Send invalid message format
    * def invalidMessage = {
        type: 'invalid.message.type',
        data: { invalid: 'data' }
      }
    
    * wsConnection.sendMessage(invalidMessage)
    
    # Should receive error response
    * def errorResponse = wsConnection.receiveMessage()
    * match errorResponse.type == 'error'
    * match errorResponse.data.code == 'INVALID_MESSAGE_TYPE'
    
    # Test connection recovery
    * wsConnection.disconnect()
    * match wsConnection.connected == false
    
    # Reconnect
    * wsConnection = debateFixtures.connectToDebateWebSocket(debate.id, participant.token)
    * match wsConnection.connected == true
    
    # Cleanup
    * wsConnection.disconnect()

  @regression @websocket
  Scenario: Test WebSocket presence indicators
    # Create debate with participants
    * def auth = authFixtures.login()
    * def setup = debateFixtures.createDebateWithParticipants({}, 2, auth.token)
    * def debate = setup.debate
    * def participants = setup.participants
    
    # Connect first participant
    * def wsConnection1 = debateFixtures.connectToDebateWebSocket(debate.id, participants[0].auth.token)
    
    # Connect second participant
    * def wsConnection2 = debateFixtures.connectToDebateWebSocket(debate.id, participants[1].auth.token)
    
    # First participant should receive presence notification
    * def presenceMessage = wsConnection1.receiveMessage()
    * match presenceMessage.type == 'participant.connected'
    * match presenceMessage.data.participantId == participants[1].participant.id
    
    # Test typing indicators
    * def typingMessage = {
        type: 'participant.typing',
        data: { participantId: participants[0].participant.id, typing: true }
      }
    
    * wsConnection1.sendMessage(typingMessage)
    
    # Second participant should receive typing indicator
    * def typingIndicator = wsConnection2.receiveMessage()
    * match typingIndicator.type == 'participant.typing'
    * match typingIndicator.data.participantId == participants[0].participant.id
    * match typingIndicator.data.typing == true
    
    # Disconnect first participant
    * wsConnection1.disconnect()
    
    # Second participant should receive disconnect notification
    * def disconnectMessage = wsConnection2.receiveMessage()
    * match disconnectMessage.type == 'participant.disconnected'
    * match disconnectMessage.data.participantId == participants[0].participant.id
    
    # Cleanup
    * wsConnection2.disconnect()

  @regression @websocket
  Scenario: Test WebSocket debate state synchronization
    # Create debate with participants
    * def auth = authFixtures.login()
    * def setup = debateFixtures.createDebateWithParticipants({}, 2, auth.token)
    * def debate = setup.debate
    * def participants = setup.participants
    
    # Connect participants
    * def wsConnection1 = debateFixtures.connectToDebateWebSocket(debate.id, participants[0].auth.token)
    * def wsConnection2 = debateFixtures.connectToDebateWebSocket(debate.id, participants[1].auth.token)
    
    # Start debate (should trigger state update)
    * def startedDebate = debateFixtures.startDebate(debate.id, auth.token)
    
    # Both participants should receive state update
    * def stateUpdate1 = wsConnection1.receiveMessage()
    * def stateUpdate2 = wsConnection2.receiveMessage()
    
    * match stateUpdate1.type == 'debate.state.updated'
    * match stateUpdate1.data.status == 'IN_PROGRESS'
    * match stateUpdate1.data.currentRound == 1
    
    * match stateUpdate2.type == 'debate.state.updated'
    * match stateUpdate2.data.status == 'IN_PROGRESS'
    * match stateUpdate2.data.currentRound == 1
    
    # Submit response (should update state)
    * def responseRequest = debateFixtures.generateResponseRequest("Test response")
    * def submittedResponse = debateFixtures.submitResponse(debate.id, responseRequest, participants[0].auth.token)
    
    # Both participants should receive response notification
    * def responseNotification1 = wsConnection1.receiveMessage()
    * def responseNotification2 = wsConnection2.receiveMessage()
    
    * match responseNotification1.type == 'response.submitted'
    * match responseNotification1.data.responseId == submittedResponse.id
    
    * match responseNotification2.type == 'response.submitted'
    * match responseNotification2.data.responseId == submittedResponse.id
    
    # Cleanup
    * wsConnection1.disconnect()
    * wsConnection2.disconnect()