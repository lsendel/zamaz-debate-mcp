Feature: Upload Document to RAG API

  Background:
    * url baseUrl

  Scenario: Upload document to knowledge base
    Given path '/api/knowledge-bases/' + knowledgeBaseId + '/documents'
    And request documentRequest
    And header Authorization = 'Bearer ' + authToken
    And header Content-Type = 'application/json'
    When method post
    Then status 201
    And match response.id == '#string'
    And match response.title == documentRequest.title
    And match response.content == documentRequest.content
    And match response.type == documentRequest.type
    And match response.status == 'UPLOADED'
    And match response.knowledgeBaseId == knowledgeBaseId
    And match response.uploadedAt == '#string'
    And match response.metadata == '#object'
    And match response.processingStatus == '#object'