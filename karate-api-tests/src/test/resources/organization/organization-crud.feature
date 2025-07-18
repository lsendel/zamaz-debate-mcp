@smoke @regression
Feature: Organization CRUD Operations

  Background:
    * def config = callonce read('classpath:karate-config.js')
    * def authFixtures = callonce read('classpath:fixtures/auth-fixtures.js')
    * def orgFixtures = callonce read('classpath:fixtures/organization-fixtures.js')
    * def orgUrl = config.serviceUrls.organization
    * url orgUrl

  @smoke
  Scenario: Create organization with valid data
    # Login as admin
    * def adminAuth = authFixtures.login(config.testData.adminUser.email, config.testData.adminUser.password)
    * def authHeaders = authFixtures.getAuthHeaders(adminAuth.token)
    
    # Prepare organization data
    * def orgData = 
      """
      {
        "name": "#(config.utils.generateOrgName('test-org'))",
        "description": "Test organization created by Karate tests",
        "settings": {
          "allowPublicDebates": true,
          "maxDebateParticipants": 10,
          "debateTimeout": 300,
          "requireEmailVerification": true
        },
        "tier": "BASIC"
      }
      """
    
    Given path config.endpoints.organization.create
    And request orgData
    And headers authHeaders
    When method post
    Then status 201
    And match response.id == '#string'
    And match response.name == orgData.name
    And match response.description == orgData.description
    And match response.settings.allowPublicDebates == true
    And match response.settings.maxDebateParticipants == 10
    And match response.tier == 'BASIC'
    And match response.active == true
    And match response.createdAt == '#string'
    And match response.updatedAt == '#string'
    And match response.features == '#object'
    And match response.limits == '#object'
    
    # Store organization ID for cleanup
    * def createdOrgId = response.id

  @smoke
  Scenario: Get organization by ID
    # Create organization first
    * def adminAuth = authFixtures.login(config.testData.adminUser.email, config.testData.adminUser.password)
    * def org = orgFixtures.createOrganization({}, adminAuth.token)
    * def authHeaders = authFixtures.getAuthHeaders(adminAuth.token)
    
    Given path config.endpoints.organization.base + '/' + org.id
    And headers authHeaders
    When method get
    Then status 200
    And match response.id == org.id
    And match response.name == org.name
    And match response.description == org.description
    And match response.settings == '#object'
    And match response.tier == '#string'
    And match response.active == true
    And match response.createdAt == '#string'
    And match response.updatedAt == '#string'

  @smoke
  Scenario: Update organization
    # Create organization first
    * def adminAuth = authFixtures.login(config.testData.adminUser.email, config.testData.adminUser.password)
    * def org = orgFixtures.createOrganization({}, adminAuth.token)
    * def authHeaders = authFixtures.getAuthHeaders(adminAuth.token)
    
    # Update data
    * def updateData = 
      """
      {
        "name": "#(org.name + ' Updated')",
        "description": "Updated description for test organization",
        "settings": {
          "allowPublicDebates": false,
          "maxDebateParticipants": 15,
          "debateTimeout": 600
        }
      }
      """
    
    Given path config.endpoints.organization.base + '/' + org.id
    And request updateData
    And headers authHeaders
    When method put
    Then status 200
    And match response.id == org.id
    And match response.name == updateData.name
    And match response.description == updateData.description
    And match response.settings.allowPublicDebates == false
    And match response.settings.maxDebateParticipants == 15
    And match response.settings.debateTimeout == 600
    And match response.updatedAt != org.updatedAt

  @regression
  Scenario: Delete organization
    # Create organization first
    * def adminAuth = authFixtures.login(config.testData.adminUser.email, config.testData.adminUser.password)
    * def org = orgFixtures.createOrganization({}, adminAuth.token)
    * def authHeaders = authFixtures.getAuthHeaders(adminAuth.token)
    
    Given path config.endpoints.organization.base + '/' + org.id
    And headers authHeaders
    When method delete
    Then status 204
    
    # Verify organization is deleted
    Given path config.endpoints.organization.base + '/' + org.id
    And headers authHeaders
    When method get
    Then status 404

  @regression
  Scenario: List organizations with pagination
    # Create multiple organizations
    * def adminAuth = authFixtures.login(config.testData.adminUser.email, config.testData.adminUser.password)
    * def authHeaders = authFixtures.getAuthHeaders(adminAuth.token)
    
    * def orgs = []
    * for (var i = 0; i < 5; i++) orgs.push(orgFixtures.createOrganization({name: 'Test Org ' + i}, adminAuth.token))
    
    # Test pagination
    Given path config.endpoints.organization.base
    And param page = 0
    And param size = 3
    And headers authHeaders
    When method get
    Then status 200
    And match response.organizations == '#array'
    And match response.organizations.length == 3
    And match response.totalElements == '#number'
    And match response.totalPages == '#number'
    And match response.currentPage == 0
    And match response.pageSize == 3
    And match response.hasNext == true
    And match response.hasPrevious == false

  @regression
  Scenario: Search organizations by name
    # Create organizations with specific names
    * def adminAuth = authFixtures.login(config.testData.adminUser.email, config.testData.adminUser.password)
    * def authHeaders = authFixtures.getAuthHeaders(adminAuth.token)
    
    * def searchOrg = orgFixtures.createOrganization({name: 'SearchableOrg-' + config.utils.randomString(5)}, adminAuth.token)
    * def otherOrg = orgFixtures.createOrganization({name: 'OtherOrg-' + config.utils.randomString(5)}, adminAuth.token)
    
    # Search by name
    Given path config.endpoints.organization.base
    And param search = 'SearchableOrg'
    And headers authHeaders
    When method get
    Then status 200
    And match response.organizations == '#array'
    And match response.organizations.length >= 1
    And match response.organizations[0].name contains 'SearchableOrg'

  @security
  Scenario: Create organization without authentication
    * def orgData = 
      """
      {
        "name": "Unauthorized Organization",
        "description": "This should fail",
        "tier": "BASIC"
      }
      """
    
    Given path config.endpoints.organization.create
    And request orgData
    When method post
    Then status 401

  @security
  Scenario: Create organization with insufficient permissions
    # Login as regular user (not admin)
    * def userAuth = authFixtures.login(config.testData.defaultUser.email, config.testData.defaultUser.password)
    * def authHeaders = authFixtures.getAuthHeaders(userAuth.token)
    
    * def orgData = 
      """
      {
        "name": "Forbidden Organization",
        "description": "This should fail due to insufficient permissions",
        "tier": "BASIC"
      }
      """
    
    Given path config.endpoints.organization.create
    And request orgData
    And headers authHeaders
    When method post
    Then status 403

  @security
  Scenario: Access organization from different tenant
    # Create two organizations with different admins
    * def org1Setup = orgFixtures.createOrganizationWithUsers({name: 'Org1'}, 1)
    * def org2Setup = orgFixtures.createOrganizationWithUsers({name: 'Org2'}, 1)
    
    # Try to access org2 using org1 admin credentials
    * def org1AuthHeaders = authFixtures.getAuthHeaders(org1Setup.admin.token)
    * set org1AuthHeaders['X-Organization-Id'] = org1Setup.organization.id
    
    Given path config.endpoints.organization.base + '/' + org2Setup.organization.id
    And headers org1AuthHeaders
    When method get
    Then status 403

  @regression
  Scenario: Create organization with invalid data
    * def adminAuth = authFixtures.login(config.testData.adminUser.email, config.testData.adminUser.password)
    * def authHeaders = authFixtures.getAuthHeaders(adminAuth.token)
    
    # Test with missing required fields
    * def invalidOrgData = 
      """
      {
        "description": "Missing name field"
      }
      """
    
    Given path config.endpoints.organization.create
    And request invalidOrgData
    And headers authHeaders
    When method post
    Then status 400
    And match response.error.code == 'VALIDATION_ERROR'
    And match response.error.details == '#object'

  @regression
  Scenario: Create organization with duplicate name
    * def adminAuth = authFixtures.login(config.testData.adminUser.email, config.testData.adminUser.password)
    * def authHeaders = authFixtures.getAuthHeaders(adminAuth.token)
    
    # Create first organization
    * def orgName = 'DuplicateOrg-' + config.utils.randomString(5)
    * def org1 = orgFixtures.createOrganization({name: orgName}, adminAuth.token)
    
    # Try to create second organization with same name
    * def duplicateOrgData = 
      """
      {
        "name": "#(orgName)",
        "description": "Duplicate organization name",
        "tier": "BASIC"
      }
      """
    
    Given path config.endpoints.organization.create
    And request duplicateOrgData
    And headers authHeaders
    When method post
    Then status 409
    And match response.error.code == 'ORGANIZATION_NAME_EXISTS'

  @regression
  Scenario: Update non-existent organization
    * def adminAuth = authFixtures.login(config.testData.adminUser.email, config.testData.adminUser.password)
    * def authHeaders = authFixtures.getAuthHeaders(adminAuth.token)
    
    * def updateData = 
      """
      {
        "name": "Non-existent Organization",
        "description": "This should fail"
      }
      """
    
    Given path config.endpoints.organization.base + '/non-existent-id'
    And request updateData
    And headers authHeaders
    When method put
    Then status 404

  @regression
  Scenario: Organization tier upgrade
    * def adminAuth = authFixtures.login(config.testData.adminUser.email, config.testData.adminUser.password)
    * def authHeaders = authFixtures.getAuthHeaders(adminAuth.token)
    
    # Create basic tier organization
    * def basicOrg = orgFixtures.createOrganizationForTier('BASIC', adminAuth.token)
    
    # Upgrade to PRO tier
    * def upgradeData = 
      """
      {
        "tier": "PRO"
      }
      """
    
    Given path config.endpoints.organization.base + '/' + basicOrg.id
    And request upgradeData
    And headers authHeaders
    When method put
    Then status 200
    And match response.tier == 'PRO'
    And match response.features.aiAssistant == true
    And match response.features.advancedAnalytics == true
    And match response.features.apiAccess == true

  @performance
  Scenario: Bulk organization operations
    * def adminAuth = authFixtures.login(config.testData.adminUser.email, config.testData.adminUser.password)
    * def authHeaders = authFixtures.getAuthHeaders(adminAuth.token)
    
    # Create multiple organizations quickly
    * def startTime = Date.now()
    * def orgs = []
    * for (var i = 0; i < 10; i++) orgs.push(orgFixtures.createOrganization({name: 'BulkOrg-' + i}, adminAuth.token))
    * def endTime = Date.now()
    * def duration = endTime - startTime
    
    # Verify all organizations were created
    * match orgs.length == 10
    * assert duration < 10000  # Should complete within 10 seconds
    
    # Clean up
    * for (var i = 0; i < orgs.length; i++) orgFixtures.deleteOrganization(orgs[i].id, adminAuth.token)