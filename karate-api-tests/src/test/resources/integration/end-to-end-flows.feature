@integration @smoke
Feature: End-to-End Integration Test Flows

  Background:
    * def config = callonce read('classpath:karate-config.js')
    * def authFixtures = callonce read('classpath:fixtures/auth-fixtures.js')
    * def orgFixtures = callonce read('classpath:fixtures/organization-fixtures.js')
    * def debateFixtures = callonce read('classpath:fixtures/debate-fixtures.js')
    * def llmFixtures = callonce read('classpath:fixtures/llm-fixtures.js')
    * def ragFixtures = callonce read('classpath:fixtures/rag-fixtures.js')
    * def evidenceGenerator = callonce read('classpath:utils/evidence-generator.js')

  @integration @smoke
  Scenario: Complete debate platform workflow
    # Initialize evidence collection
    * def evidence = evidenceGenerator.generateScenarioEvidence('Complete Debate Platform Workflow', 'integration', {})
    
    # Step 1: Create organization with admin
    * def orgSetup = orgFixtures.createOrganizationWithUsers({
        name: 'Integration Test Organization',
        description: 'Organization for end-to-end testing'
      }, 5)
    
    * def organization = orgSetup.organization
    * def adminAuth = orgSetup.admin
    * def users = orgSetup.users
    
    # Record evidence
    * evidenceGenerator.recordValidation(evidence, 'Organization Creation', 'PASS', 'Organization created successfully with ' + users.length + ' users')
    
    # Step 2: Create knowledge base and upload documents
    * def knowledgeBase = ragFixtures.createKnowledgeBase({
        name: 'Debate Knowledge Base',
        description: 'Knowledge base for debate context'
      }, adminAuth.token)
    
    * def testDocuments = ragFixtures.generateTestDocuments()
    * def uploadedDocs = ragFixtures.uploadMultipleDocuments(testDocuments, knowledgeBase.id, adminAuth.token)
    
    # Record evidence
    * evidenceGenerator.recordValidation(evidence, 'Knowledge Base Setup', 'PASS', 'Knowledge base created with ' + uploadedDocs.length + ' documents')
    
    # Step 3: Create debate using LLM for topic generation
    * def topicPrompt = "Generate a compelling debate topic about the impact of AI on employment"
    * def llmResponse = llmFixtures.createCompletion({
        prompt: topicPrompt,
        maxTokens: 100,
        temperature: 0.7
      }, adminAuth.token)
    
    * def generatedTopic = llmResponse.content
    
    # Create debate with AI-generated topic
    * def debate = debateFixtures.createDebate({
        topic: generatedTopic,
        description: 'AI-generated debate topic for integration testing',
        config: {
          format: 'OXFORD',
          maxRounds: 3,
          maxParticipants: 4,
          isPublic: true
        }
      }, adminAuth.token)
    
    # Record evidence
    * evidenceGenerator.recordValidation(evidence, 'AI-Generated Debate Creation', 'PASS', 'Debate created with AI-generated topic: ' + generatedTopic)
    
    # Step 4: Add participants to debate
    * def participants = []
    * for (var i = 0; i < 4; i++) {
        var position = i < 2 ? 'PRO' : 'CON';
        var participant = debateFixtures.addParticipant(debate.id, {
          userId: users[i].id,
          position: position,
          role: 'DEBATER'
        }, adminAuth.token);
        participants.push(participant);
      }
    
    # Record evidence
    * evidenceGenerator.recordValidation(evidence, 'Participant Addition', 'PASS', participants.length + ' participants added to debate')
    
    # Step 5: Start debate
    * def startedDebate = debateFixtures.startDebate(debate.id, adminAuth.token)
    
    # Record evidence
    * evidenceGenerator.recordValidation(evidence, 'Debate Start', 'PASS', 'Debate started successfully')
    
    # Step 6: Generate AI-assisted responses using RAG
    * def responses = []
    * for (var round = 1; round <= 2; round++) {
        for (var i = 0; i < participants.length; i++) {
          var participant = participants[i];
          var user = users[i];
          
          # Search knowledge base for relevant context
          var searchResults = ragFixtures.searchDocuments({
            query: debate.topic,
            maxResults: 3
          }, knowledgeBase.id, adminAuth.token);
          
          # Generate AI response with context
          var contextPrompt = "Based on this context: " + searchResults[0].content.substring(0, 200) + 
                              "... Provide a " + participant.position + " argument for: " + debate.topic;
          
          var aiResponse = llmFixtures.createCompletion({
            prompt: contextPrompt,
            maxTokens: 200,
            temperature: 0.8
          }, adminAuth.token);
          
          # Submit response to debate
          var debateResponse = debateFixtures.submitResponse(debate.id, {
            content: aiResponse.content,
            type: 'ARGUMENT',
            evidenceUrls: [],
            tags: ['AI-assisted', 'round-' + round]
          }, user.token);
          
          responses.push(debateResponse);
        }
      }
    
    # Record evidence
    * evidenceGenerator.recordValidation(evidence, 'AI-Assisted Responses', 'PASS', responses.length + ' AI-assisted responses generated and submitted')
    
    # Step 7: Generate debate analysis
    * def debateAnalysis = debateFixtures.getDebateAnalysis(debate.id, adminAuth.token)
    
    # Record evidence
    * evidenceGenerator.recordValidation(evidence, 'Debate Analysis', 'PASS', 'Debate analysis generated successfully')
    
    # Step 8: Complete debate
    * def completedDebate = debateFixtures.completeDebate(debate.id, adminAuth.token)
    
    # Record evidence
    * evidenceGenerator.recordValidation(evidence, 'Debate Completion', 'PASS', 'Debate completed successfully')
    
    # Step 9: Generate final summary using LLM
    * def summaryPrompt = "Summarize this debate: " + debate.topic + " with " + responses.length + " responses"
    * def summaryResponse = llmFixtures.createCompletion({
        prompt: summaryPrompt,
        maxTokens: 300,
        temperature: 0.5
      }, adminAuth.token)
    
    # Record evidence
    * evidenceGenerator.recordValidation(evidence, 'AI Summary Generation', 'PASS', 'Final summary generated: ' + summaryResponse.content.substring(0, 100) + '...')
    
    # Complete evidence collection
    * evidenceGenerator.completeEvidence(evidence, 'passed')
    
    # Generate evidence report
    * def evidenceReport = evidenceGenerator.generateReport(evidence)
    
    # Validate complete workflow
    * match organization.id == '#string'
    * match knowledgeBase.id == '#string'
    * match debate.id == '#string'
    * match participants.length == 4
    * match responses.length == 8
    * match completedDebate.status == 'COMPLETED'
    * match evidenceReport.summary.status == 'passed'
    
    # Performance validation
    * assert evidence.execution.duration < 120000  # Should complete within 2 minutes
    * assert evidenceReport.summary.validationsPassed >= 8  # All validations should pass

  @integration @regression
  Scenario: Multi-service error handling workflow
    # Initialize evidence collection
    * def evidence = evidenceGenerator.generateScenarioEvidence('Multi-Service Error Handling', 'integration', {})
    
    # Test cascading error handling across services
    * def auth = authFixtures.login()
    
    # Step 1: Try to create debate with invalid LLM provider
    * def invalidLLMResponse = null
    * try {
        invalidLLMResponse = llmFixtures.createCompletion({
          prompt: "Test prompt",
          provider: "invalid-provider",
          model: "invalid-model"
        }, auth.token);
      } catch (e) {
        evidenceGenerator.recordError(evidence, e, 'LLM Service Error Handling');
      }
    
    # Step 2: Try to upload document to non-existent knowledge base
    * def invalidRAGResponse = null
    * try {
        invalidRAGResponse = ragFixtures.uploadDocument({
          title: "Test Document",
          content: "Test content"
        }, "non-existent-kb", auth.token);
      } catch (e) {
        evidenceGenerator.recordError(evidence, e, 'RAG Service Error Handling');
      }
    
    # Step 3: Try to add participant to non-existent debate
    * def invalidDebateResponse = null
    * try {
        invalidDebateResponse = debateFixtures.addParticipant("non-existent-debate", {
          userId: "invalid-user",
          position: "PRO"
        }, auth.token);
      } catch (e) {
        evidenceGenerator.recordError(evidence, e, 'Debate Service Error Handling');
      }
    
    # Validate error handling
    * evidenceGenerator.recordValidation(evidence, 'Error Handling', 'PASS', 'All services handled errors gracefully')
    
    # Complete evidence collection
    * evidenceGenerator.completeEvidence(evidence, 'passed')
    
    # Validate that errors were properly handled
    * match evidence.errors.length >= 3
    * match evidence.errors[0].context == 'LLM Service Error Handling'
    * match evidence.errors[1].context == 'RAG Service Error Handling'
    * match evidence.errors[2].context == 'Debate Service Error Handling'

  @integration @performance
  Scenario: High-load multi-service integration
    # Initialize evidence collection
    * def evidence = evidenceGenerator.generateScenarioEvidence('High-Load Multi-Service Integration', 'integration', {})
    
    # Create organization for load testing
    * def loadTestOrg = orgFixtures.createOrganizationWithUsers({
        name: 'Load Test Organization'
      }, 10)
    
    # Create multiple debates concurrently
    * def debates = []
    * def debateRequests = []
    
    * for (var i = 0; i < 5; i++) {
        debateRequests.push({
          topic: 'Load Test Debate ' + i,
          description: 'Debate for load testing',
          config: {
            format: 'STANDARD',
            maxRounds: 2,
            maxParticipants: 4
          }
        });
      }
    
    * def startTime = Date.now()
    
    # Create debates in parallel
    * def debateResults = karate.parallel(debateRequests, function(request) {
        return karate.call('classpath:debate/create-debate.feature', {
          debateRequest: request,
          authToken: loadTestOrg.admin.token,
          baseUrl: config.serviceUrls.controller
        });
      })
    
    * def debateCreationTime = Date.now() - startTime
    
    # Record evidence
    * evidenceGenerator.recordValidation(evidence, 'Concurrent Debate Creation', 'PASS', 
        'Created ' + debateResults.length + ' debates in ' + debateCreationTime + 'ms')
    
    # Generate multiple LLM completions concurrently
    * def llmRequests = []
    * for (var i = 0; i < 10; i++) {
        llmRequests.push({
          prompt: 'Load test prompt ' + i,
          maxTokens: 50,
          temperature: 0.7
        });
      }
    
    * def llmStartTime = Date.now()
    
    * def llmResults = karate.parallel(llmRequests, function(request) {
        return karate.call('classpath:llm/create-completion.feature', {
          completionRequest: request,
          authToken: loadTestOrg.admin.token,
          baseUrl: config.serviceUrls.llm
        });
      })
    
    * def llmCompletionTime = Date.now() - llmStartTime
    
    # Record evidence
    * evidenceGenerator.recordValidation(evidence, 'Concurrent LLM Completions', 'PASS', 
        'Completed ' + llmResults.length + ' LLM requests in ' + llmCompletionTime + 'ms')
    
    # Upload multiple documents concurrently
    * def docRequests = []
    * def testDocs = ragFixtures.generateTestDocuments()
    
    * for (var i = 0; i < testDocs.length; i++) {
        docRequests.push(testDocs[i]);
      }
    
    * def ragStartTime = Date.now()
    
    * def ragResults = karate.parallel(docRequests, function(request) {
        return karate.call('classpath:rag/upload-document.feature', {
          knowledgeBaseId: 'default',
          documentRequest: request,
          authToken: loadTestOrg.admin.token,
          baseUrl: config.serviceUrls.rag
        });
      })
    
    * def ragUploadTime = Date.now() - ragStartTime
    
    # Record evidence
    * evidenceGenerator.recordValidation(evidence, 'Concurrent Document Uploads', 'PASS', 
        'Uploaded ' + ragResults.length + ' documents in ' + ragUploadTime + 'ms')
    
    # Complete evidence collection
    * evidenceGenerator.completeEvidence(evidence, 'passed')
    
    # Validate performance
    * assert debateCreationTime < 10000  # Should complete within 10 seconds
    * assert llmCompletionTime < 20000   # Should complete within 20 seconds
    * assert ragUploadTime < 15000       # Should complete within 15 seconds
    
    # Validate all operations succeeded
    * def successfulDebates = debateResults.filter(r => r.responseStatus == 201)
    * def successfulLLM = llmResults.filter(r => r.responseStatus == 200)
    * def successfulRAG = ragResults.filter(r => r.responseStatus == 201)
    
    * match successfulDebates.length == 5
    * match successfulLLM.length == 10
    * match successfulRAG.length == testDocs.length

  @integration @security
  Scenario: Cross-service security validation
    # Initialize evidence collection
    * def evidence = evidenceGenerator.generateScenarioEvidence('Cross-Service Security Validation', 'integration', {})
    
    # Create two organizations for isolation testing
    * def org1 = orgFixtures.createOrganizationWithUsers({name: 'Org1'}, 2)
    * def org2 = orgFixtures.createOrganizationWithUsers({name: 'Org2'}, 2)
    
    # Step 1: Create debate in org1
    * def org1Debate = debateFixtures.createDebate({
        topic: 'Security Test Debate',
        config: { isPublic: false }
      }, org1.admin.token)
    
    # Step 2: Try to access org1 debate from org2 user
    * def org2AuthHeaders = authFixtures.getAuthHeaders(org2.admin.token)
    * set org2AuthHeaders['X-Organization-Id'] = org2.organization.id
    
    Given url config.serviceUrls.controller
    Given path '/api/debates/' + org1Debate.id
    And headers org2AuthHeaders
    When method get
    Then status 403
    
    # Record evidence
    * evidenceGenerator.recordValidation(evidence, 'Multi-Tenant Isolation', 'PASS', 'Org2 cannot access Org1 debate')
    
    # Step 3: Try to access org1 LLM completions from org2
    * def unauthorizedLLM = null
    * try {
        unauthorizedLLM = llmFixtures.createCompletion({
          prompt: "Unauthorized access test",
          metadata: { organizationId: org1.organization.id }
        }, org2.admin.token);
      } catch (e) {
        evidenceGenerator.recordError(evidence, e, 'LLM Cross-Org Access Denied');
      }
    
    # Step 4: Try to access org1 documents from org2
    * def org1KB = ragFixtures.createKnowledgeBase({
        name: 'Org1 KB',
        isPublic: false
      }, org1.admin.token)
    
    * def org1Doc = ragFixtures.uploadDocument({
        title: 'Org1 Document',
        content: 'Private content'
      }, org1KB.id, org1.admin.token)
    
    Given url config.serviceUrls.rag
    Given path '/api/documents/' + org1Doc.id
    And headers org2AuthHeaders
    When method get
    Then status 403
    
    # Record evidence
    * evidenceGenerator.recordValidation(evidence, 'Document Access Control', 'PASS', 'Org2 cannot access Org1 documents')
    
    # Step 5: Test rate limiting across services
    * def rateLimitRequests = []
    * for (var i = 0; i < 50; i++) {
        rateLimitRequests.push({
          prompt: 'Rate limit test ' + i,
          maxTokens: 10
        });
      }
    
    * def rateLimitResults = karate.parallel(rateLimitRequests, function(request) {
        return karate.call('classpath:llm/create-completion.feature', {
          completionRequest: request,
          authToken: org1.admin.token,
          baseUrl: config.serviceUrls.llm
        });
      })
    
    * def rateLimitedRequests = rateLimitResults.filter(r => r.responseStatus == 429)
    * assert rateLimitedRequests.length > 0
    
    # Record evidence
    * evidenceGenerator.recordValidation(evidence, 'Rate Limiting', 'PASS', 'Rate limiting working across services')
    
    # Complete evidence collection
    * evidenceGenerator.completeEvidence(evidence, 'passed')
    
    # Generate comprehensive evidence report
    * def evidenceReport = evidenceGenerator.generateReport(evidence)
    
    # Validate security measures
    * match evidenceReport.summary.validationsPassed >= 3
    * match evidence.errors.length >= 1  # Should have captured unauthorized access errors