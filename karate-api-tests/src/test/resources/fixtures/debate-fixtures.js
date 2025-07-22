// TODO: Refactor to reduce cognitive complexity (SonarCloud S3776)
// Consider breaking down complex functions into smaller, more focused functions

/**
 * Debate Service Fixtures and Utilities
 * This file contains reusable debate service functions and test data
 */

function fn() {
    const config = karate.callSingle('classpath:karate-config.js');
    const authFixtures = karate.callSingle('classpath:fixtures/auth-fixtures.js');
    const debateFixtures = {
        // Debate cache
        debateCache: {},

        // Generate debate request
        generatedebaterequest: function(overrides) {
            let defaultRequest = {
                topic: "Should artificial intelligence be regulated by government?",
                description: "A debate about the role of government regulation in AI development",
                config: {
                    format: "OXFORD",
                    maxRounds: 3,
                    responseTimeout: 300,
                    maxParticipants: 6,
                    allowAnonymous: false,
                    requireModeration: false,
                    isPublic: true,
                    tags: ["AI", "regulation", "government"],
                    difficulty: "INTERMEDIATE"
                },
                positions: [
                    {
                        side: "PRO",
                        description: "AI should be regulated by government",
                        maxParticipants: 3
                    },
                    {
                        side: "CON",
                        description: "AI regulation would stifle innovation",
                        maxParticipants: 3
                    }
                ]
            }

            return Object.assign({}, defaultRequest, overrides || {});
        },

        // Generate participant request
        generateparticipantrequest: function(userId, position, overrides) {
            let defaultRequest = {
                userId: userId,
                position: position || "PRO",
                role: "DEBATER",
                anonymous: false,
                bio: "Participant in the debate"
            }

            return Object.assign({}, defaultRequest, overrides || {});
        },

        // Generate response request
        generateresponserequest: function(content, overrides) {
            const defaultRequest = {
                content: content || "This is a test response in the debate.",
                type: "ARGUMENT",
                evidenceUrls: [],
                tags: [],
                metadata: {}
            }

            return Object.assign({}, defaultRequest, overrides || {});
        },

        // Create debate
        createdebate: function(debateData, authToken) {
            if (!authToken) {
                let auth = authFixtures.login();
                authToken = auth.token;
            }

            const debateRequest = debateFixtures.generateDebateRequest(debateData);

            let response = karate.call('classpath:debate/create-debate.feature', {
                debateRequest: debateRequest,
                authToken: authToken,
                baseUrl: config.serviceUrls.controller
            });

            if (response.response && response.response.id) {
                let debate = response.response;
                debateFixtures.debateCache[debate.id] = debate;
                return debate;
            }

            throw new Error('Failed to create debate');
        },

        // Get debate
        getdebate: function(debateId, authToken) {
            if (!authToken) {
                let auth = authFixtures.login();
                authToken = auth.token;
            }

            let response = karate.call('classpath:debate/get-debate.feature', {
                debateId: debateId,
                authToken: authToken,
                baseUrl: config.serviceUrls.controller
            });

            if (response.response && response.response.id) {
                return response.response;
            }

            throw new Error('Failed to get debate: ' + debateId);
        },

        // Update debate
        updatedebate: function(debateId, updateData, authToken) {
            if (!authToken) {
                let auth = authFixtures.login();
                authToken = auth.token;
            }

            let response = karate.call('classpath:debate/update-debate.feature', {
                debateId: debateId,
                updateRequest: updateData,
                authToken: authToken,
                baseUrl: config.serviceUrls.controller
            });

            if (response.response && response.response.id) {
                let debate = response.response;
                debateFixtures.debateCache[debate.id] = debate;
                return debate;
            }

            throw new Error('Failed to update debate: ' + debateId);
        },

        // Delete debate
        deletedebate: function(debateId, authToken) {
            if (!authToken) {
                let auth = authFixtures.login();
                authToken = auth.token;
            }

            let response = karate.call('classpath:debate/delete-debate.feature', {
                debateId: debateId,
                authToken: authToken,
                baseUrl: config.serviceUrls.controller
            });

            // Remove from cache
            if (debateFixtures.debateCache[debateId]) {
                delete debateFixtures.debateCache[debateId]
            }

            return response.response;
        },

        // List debates
        listdebates: function(filters, authToken) {
            if (!authToken) {
                let auth = authFixtures.login();
                authToken = auth.token;
            }

            let response = karate.call('classpath:debate/list-debates.feature', {
                filters: filters || {},
                authToken: authToken,
                baseUrl: config.serviceUrls.controller
            });

            if (response.response && response.response.debates) {
                return response.response.debates;
            }

            return []
        },

        // Add participant to debate
        addparticipant: function(debateId, participantData, authToken) {
            if (!authToken) {
                let auth = authFixtures.login();
                authToken = auth.token;
            }

            const participantRequest = debateFixtures.generateParticipantRequest(
                participantData.userId,
                participantData.position,
                participantData
            );

            let response = karate.call('classpath:debate/add-participant.feature', {
                debateId: debateId,
                participantRequest: participantRequest,
                authToken: authToken,
                baseUrl: config.serviceUrls.controller
            });

            if (response.response && response.response.id) {
                return response.response;
            }

            throw new Error('Failed to add participant to debate: ' + debateId);
        },

        // Remove participant from debate
        removeparticipant: function(debateId, participantId, authToken) {
            if (!authToken) {
                let auth = authFixtures.login();
                authToken = auth.token;
            }

            let response = karate.call('classpath:debate/remove-participant.feature', {
                debateId: debateId,
                participantId: participantId,
                authToken: authToken,
                baseUrl: config.serviceUrls.controller
            });

            return response.response;
        },

        // Start debate
        startdebate: function(debateId, authToken) {
            if (!authToken) {
                let auth = authFixtures.login();
                authToken = auth.token;
            }

            let response = karate.call('classpath:debate/start-debate.feature', {
                debateId: debateId,
                authToken: authToken,
                baseUrl: config.serviceUrls.controller
            });

            if (response.response && response.response.id) {
                let debate = response.response;
                debateFixtures.debateCache[debate.id] = debate;
                return debate;
            }

            throw new Error('Failed to start debate: ' + debateId);
        },

        // Submit response
        submitresponse: function(debateId, responseData, authToken) {
            if (!authToken) {
                let auth = authFixtures.login();
                authToken = auth.token;
            }

            const responseRequest = debateFixtures.generateResponseRequest(
                responseData.content,
                responseData
            );

            let response = karate.call('classpath:debate/submit-response.feature', {
                debateId: debateId,
                responseRequest: responseRequest,
                authToken: authToken,
                baseUrl: config.serviceUrls.controller
            });

            if (response.response && response.response.id) {
                return response.response;
            }

            throw new Error('Failed to submit response to debate: ' + debateId);
        },

        // Complete debate
        completedebate: function(debateId, authToken) {
            if (!authToken) {
                let auth = authFixtures.login();
                authToken = auth.token;
            }

            let response = karate.call('classpath:debate/complete-debate.feature', {
                debateId: debateId,
                authToken: authToken,
                baseUrl: config.serviceUrls.controller
            });

            if (response.response && response.response.id) {
                let debate = response.response;
                debateFixtures.debateCache[debate.id] = debate;
                return debate;
            }

            throw new Error('Failed to complete debate: ' + debateId);
        },

        // Get debate analysis
        getdebateanalysis: function(debateId, authToken) {
            if (!authToken) {
                let auth = authFixtures.login();
                authToken = auth.token;
            }

            let response = karate.call('classpath:debate/get-debate-analysis.feature', {
                debateId: debateId,
                authToken: authToken,
                baseUrl: config.serviceUrls.controller
            });

            if (response.response) {
                return response.response;
            }

            throw new Error('Failed to get debate analysis: ' + debateId);
        },

        // Create debate with participants
        createdebatewithparticipants: function(debateData, participantCount, authToken) {
            participantCount = participantCount || 4;

            if (!authToken) {
                let auth = authFixtures.login();
                authToken = auth.token;
            }

            // Create the debate
            const debate = debateFixtures.createDebate(debateData, authToken);

            // Create participants
            const participants = []
            for (var i = 0; i < participantCount; i++) {
                const userAuth = authFixtures.register({
                    email: config.utils.generateEmail('participant' + i),
                    name: 'Participant ' + i
                });

                const position = i < participantCount / 2 ? 'PRO' : 'CON';
                let participant = debateFixtures.addParticipant(debate.id, {
                    userId: userAuth.user.id,
                    position: position,
                    role: 'DEBATER'
                }, authToken);

                participants.push({
                    participant: participant,
                    auth: userAuth
                });
            }

            return {
                debate: debate,
                participants: participants
            }
        },

        // Run complete debate scenario
        runcompletedebatescenario: function(debateData, participantCount, roundCount) {
            participantCount = participantCount || 4;
            roundCount = roundCount || 3;

            let auth = authFixtures.login();
            const setup = debateFixtures.createDebateWithParticipants(debateData, participantCount, auth.token);

            // Start the debate
//              // Removed: useless assignment
            for (var round = 1; round <= roundCount; round++) {
                for (var i = 0; i < setup.participants.length; i++) {
                    const participant = setup.participants[i]
                    let response = debateFixtures.submitResponse(setup.debate.id, {
                        content: 'Round ' + round + ' response from ' + participant.participant.userId,
                        type: 'ARGUMENT',
                        round: round
                    }, participant.auth.token);

                    responses.push(response);
                }
            }

            // Complete the debate
            const completedDebate = debateFixtures.completeDebate(setup.debate.id, auth.token);

            return {
                debate: completedDebate,
                participants: setup.participants,
                responses: responses
            }
        },

        // Get debate statistics
        getdebatestats: function(debateId, authToken) {
            if (!authToken) {
                let auth = authFixtures.login();
                authToken = auth.token;
            }

            const response = karate.call('classpath:debate/get-debate-stats.feature', {
                debateId: debateId,
                authToken: authToken,
                baseUrl: config.serviceUrls.controller
            });

            if (response.response) {
                return response.response;
            }

            throw new Error('Failed to get debate statistics: ' + debateId);
        },

        // Connect to debate WebSocket
        connecttodebatewebsocket: function(debateId, authToken) {
            if (!authToken) {
                const auth = authFixtures.login();
                authToken = auth.token;
            }

            // This would be implemented with actual WebSocket connection
            // For now, we return a mock connection object
            return {
                debateId: debateId,
                connected: true,
                url: config.serviceUrls.controller + '/api/v1/debates/' + debateId + '/ws',
                authToken: authToken,
                messages: [],

                sendmessage: function(message) {
                    this.messages.push({
                        type: 'outgoing',
                        message: message,
                        timestamp: new Date().toISOString()
                    });
                },

                receivemessage: function() {
                    // Mock receiving a message
                    const mockMessage = {
                        type: 'debate.update',
                        debateId: this.debateId,
                        data: {
                            status: 'IN_PROGRESS',
                            currentRound: 1,
                            lastActivity: new Date().toISOString()
                        }
                    }

                    this.messages.push({
                        type: 'incoming',
                        message: mockMessage,
                        timestamp: new Date().toISOString()
                    });

                    return mockMessage;
                },

                disconnect: function() {
                    this.connected = false;
                }
            }
        },

        // Validate debate response
        validatedebateresponse: function(response) {
            const validationErrors = []

            if (!response.id || typeof response.id !== 'string') {
                validationErrors.push('Missing or invalid debate ID');
            }

            if (!response.topic || typeof response.topic !== 'string') {
                validationErrors.push('Missing or invalid debate topic');
            }

            if (!response.status || typeof response.status !== 'string') {
                validationErrors.push('Missing or invalid debate status');
            }

            if (!response.config || typeof response.config !== 'object') {
                validationErrors.push('Missing or invalid debate configuration');
            }

            if (!response.createdAt || typeof response.createdAt !== 'string') {
                validationErrors.push('Missing or invalid creation timestamp');
            }

            if (response.participants && !Array.isArray(response.participants)) {
                validationErrors.push('Invalid participants array');
            }

            if (response.rounds && !Array.isArray(response.rounds)) {
                validationErrors.push('Invalid rounds array');
            }

            return validationErrors;
        },

        // Generate test debate topics
        generatetesttopics: function() {
            return [
                "Should artificial intelligence be regulated by government?",
                "Is remote work better than office work?",
                "Should social media platforms be held responsible for content moderation?",
                "Is climate change primarily caused by human activity?",
                "Should college education be free for all students?",
                "Is genetic engineering ethically acceptable?",
                "Should cryptocurrencies replace traditional currency?",
                "Is space exploration worth the cost?",
                "Should autonomous vehicles be allowed on public roads?",
                "Is universal basic income a viable economic policy?"
            ]
        },

        // Generate performance test scenarios
        generateperformancescenarios: function() {
            return {
                concurrent_debates: {
                    description: "Create multiple debates simultaneously",
                    debateCount: 10,
                    participantsPerDebate: 4,
                    expectedMaxTime: 30000
                },
                large_debate: {
                    description: "Create debate with many participants",
                    debateCount: 1,
                    participantsPerDebate: 20,
                    expectedMaxTime: 15000
                },
                rapid_responses: {
                    description: "Submit many responses quickly",
                    responseCount: 50,
                    expectedMaxTime: 20000
                },
                websocket_load: {
                    description: "Test WebSocket connections under load",
                    connectionCount: 100,
                    expectedMaxTime: 10000
                }
            }
        },

        // Clear debate cache
        cleardebatecache: function() {
            debateFixtures.debateCache = {}
        },

        // Get sample debate data
        getsampledebatedata: function(type) {
            type = type || 'default';

            if (type === 'default') {
                return debateTestData.sampleDebates[0]
            } else if (type === 'simple') {
                return debateTestData.sampleDebates[1]
            } else if (type === 'complex') {
                return debateTestData.sampleDebates[2]
            }

            return debateTestData.sampleDebates[0]
        },

        // Get debate template
        getdebatetemplate: function(type) {
            type = type || 'academic';

            if (type === 'academic') {
                return debateTestData.debateTemplates[0]
            } else if (type === 'community') {
                return debateTestData.debateTemplates[1]
            }

            return debateTestData.debateTemplates[0]
        }
    }

    return debateFixtures;
}
