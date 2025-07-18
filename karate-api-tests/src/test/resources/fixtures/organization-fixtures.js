/**
 * Organization Fixtures and Utilities
 * This file contains reusable organization management functions and test data
 */

function fn() {
    var config = karate.callSingle('classpath:karate-config.js');
    var authFixtures = karate.callSingle('classpath:fixtures/auth-fixtures.js');
    var orgTestData = karate.callSingle('classpath:test-data/organizations.json');
    
    var organizationFixtures = {
        // Organization cache
        organizationCache: {},
        
        // Create organization
        createOrganization: function(orgData, authToken) {
            orgData = orgData || {};
            var defaultOrgData = {
                name: config.utils.generateOrgName('test-org'),
                description: 'Test organization created by Karate tests',
                settings: orgTestData.defaultOrganization.settings,
                tier: 'BASIC',
                features: {
                    aiAssistant: false,
                    advancedAnalytics: false,
                    customBranding: false,
                    apiAccess: true,
                    webhooks: false,
                    sso: false,
                    auditLogs: false
                }
            };
            
            var finalOrgData = Object.assign(defaultOrgData, orgData);
            
            // If no auth token provided, use admin token
            if (!authToken) {
                var adminAuth = authFixtures.login(config.testData.adminUser.email, config.testData.adminUser.password);
                authToken = adminAuth.token;
            }
            
            var createResponse = karate.call('classpath:organization/create-organization.feature', {
                organizationRequest: finalOrgData,
                authToken: authToken,
                baseUrl: config.serviceUrls.organization
            });
            
            if (createResponse.response && createResponse.response.id) {
                var org = createResponse.response;
                organizationFixtures.organizationCache[org.id] = org;
                return org;
            }
            
            throw new Error('Failed to create organization: ' + finalOrgData.name);
        },
        
        // Get organization by ID
        getOrganization: function(orgId, authToken) {
            if (!authToken) {
                var adminAuth = authFixtures.login(config.testData.adminUser.email, config.testData.adminUser.password);
                authToken = adminAuth.token;
            }
            
            var getResponse = karate.call('classpath:organization/get-organization.feature', {
                organizationId: orgId,
                authToken: authToken,
                baseUrl: config.serviceUrls.organization
            });
            
            if (getResponse.response && getResponse.response.id) {
                return getResponse.response;
            }
            
            throw new Error('Failed to get organization: ' + orgId);
        },
        
        // Update organization
        updateOrganization: function(orgId, updateData, authToken) {
            if (!authToken) {
                var adminAuth = authFixtures.login(config.testData.adminUser.email, config.testData.adminUser.password);
                authToken = adminAuth.token;
            }
            
            var updateResponse = karate.call('classpath:organization/update-organization.feature', {
                organizationId: orgId,
                updateRequest: updateData,
                authToken: authToken,
                baseUrl: config.serviceUrls.organization
            });
            
            if (updateResponse.response && updateResponse.response.id) {
                var org = updateResponse.response;
                organizationFixtures.organizationCache[org.id] = org;
                return org;
            }
            
            throw new Error('Failed to update organization: ' + orgId);
        },
        
        // Delete organization
        deleteOrganization: function(orgId, authToken) {
            if (!authToken) {
                var adminAuth = authFixtures.login(config.testData.adminUser.email, config.testData.adminUser.password);
                authToken = adminAuth.token;
            }
            
            var deleteResponse = karate.call('classpath:organization/delete-organization.feature', {
                organizationId: orgId,
                authToken: authToken,
                baseUrl: config.serviceUrls.organization
            });
            
            // Remove from cache
            if (organizationFixtures.organizationCache[orgId]) {
                delete organizationFixtures.organizationCache[orgId];
            }
            
            return deleteResponse.response;
        },
        
        // List organizations
        listOrganizations: function(filters, authToken) {
            if (!authToken) {
                var adminAuth = authFixtures.login(config.testData.adminUser.email, config.testData.adminUser.password);
                authToken = adminAuth.token;
            }
            
            var listResponse = karate.call('classpath:organization/list-organizations.feature', {
                filters: filters || {},
                authToken: authToken,
                baseUrl: config.serviceUrls.organization
            });
            
            if (listResponse.response && listResponse.response.organizations) {
                return listResponse.response.organizations;
            }
            
            return [];
        },
        
        // Add user to organization
        addUserToOrganization: function(orgId, userData, authToken) {
            if (!authToken) {
                var adminAuth = authFixtures.login(config.testData.adminUser.email, config.testData.adminUser.password);
                authToken = adminAuth.token;
            }
            
            var defaultUserData = {
                email: config.utils.generateEmail('user'),
                name: 'Test User ' + config.utils.randomString(5),
                role: 'USER',
                permissions: ['PARTICIPATE_DEBATES', 'CREATE_DEBATES', 'VIEW_DEBATES']
            };
            
            var finalUserData = Object.assign(defaultUserData, userData);
            
            var addUserResponse = karate.call('classpath:organization/add-user.feature', {
                organizationId: orgId,
                userRequest: finalUserData,
                authToken: authToken,
                baseUrl: config.serviceUrls.organization
            });
            
            if (addUserResponse.response && addUserResponse.response.id) {
                return addUserResponse.response;
            }
            
            throw new Error('Failed to add user to organization: ' + orgId);
        },
        
        // Remove user from organization
        removeUserFromOrganization: function(orgId, userId, authToken) {
            if (!authToken) {
                var adminAuth = authFixtures.login(config.testData.adminUser.email, config.testData.adminUser.password);
                authToken = adminAuth.token;
            }
            
            var removeUserResponse = karate.call('classpath:organization/remove-user.feature', {
                organizationId: orgId,
                userId: userId,
                authToken: authToken,
                baseUrl: config.serviceUrls.organization
            });
            
            return removeUserResponse.response;
        },
        
        // List organization users
        listOrganizationUsers: function(orgId, authToken) {
            if (!authToken) {
                var adminAuth = authFixtures.login(config.testData.adminUser.email, config.testData.adminUser.password);
                authToken = adminAuth.token;
            }
            
            var listUsersResponse = karate.call('classpath:organization/list-users.feature', {
                organizationId: orgId,
                authToken: authToken,
                baseUrl: config.serviceUrls.organization
            });
            
            if (listUsersResponse.response && listUsersResponse.response.users) {
                return listUsersResponse.response.users;
            }
            
            return [];
        },
        
        // Update user role in organization
        updateUserRole: function(orgId, userId, newRole, authToken) {
            if (!authToken) {
                var adminAuth = authFixtures.login(config.testData.adminUser.email, config.testData.adminUser.password);
                authToken = adminAuth.token;
            }
            
            var updateRoleResponse = karate.call('classpath:organization/update-user-role.feature', {
                organizationId: orgId,
                userId: userId,
                roleRequest: { role: newRole },
                authToken: authToken,
                baseUrl: config.serviceUrls.organization
            });
            
            if (updateRoleResponse.response && updateRoleResponse.response.id) {
                return updateRoleResponse.response;
            }
            
            throw new Error('Failed to update user role in organization: ' + orgId);
        },
        
        // Create organization with full setup
        createOrganizationWithUsers: function(orgData, userCount) {
            userCount = userCount || 3;
            
            // Create organization
            var org = organizationFixtures.createOrganization(orgData);
            
            // Create admin user
            var adminUser = organizationFixtures.addUserToOrganization(org.id, {
                email: config.utils.generateEmail('admin'),
                name: 'Admin User for ' + org.name,
                role: 'ADMIN',
                permissions: [
                    'MANAGE_ORGANIZATION',
                    'MANAGE_USERS',
                    'MANAGE_DEBATES',
                    'VIEW_ANALYTICS',
                    'MANAGE_SETTINGS'
                ]
            });
            
            // Create regular users
            var users = [];
            for (var i = 0; i < userCount; i++) {
                var user = organizationFixtures.addUserToOrganization(org.id, {
                    email: config.utils.generateEmail('user' + i),
                    name: 'User ' + i + ' for ' + org.name,
                    role: 'USER'
                });
                users.push(user);
            }
            
            return {
                organization: org,
                admin: adminUser,
                users: users
            };
        },
        
        // Get organization statistics
        getOrganizationStats: function(orgId, authToken) {
            if (!authToken) {
                var adminAuth = authFixtures.login(config.testData.adminUser.email, config.testData.adminUser.password);
                authToken = adminAuth.token;
            }
            
            var statsResponse = karate.call('classpath:organization/get-organization-stats.feature', {
                organizationId: orgId,
                authToken: authToken,
                baseUrl: config.serviceUrls.organization
            });
            
            if (statsResponse.response) {
                return statsResponse.response;
            }
            
            throw new Error('Failed to get organization statistics: ' + orgId);
        },
        
        // Validate organization data
        validateOrganizationData: function(orgData) {
            var errors = [];
            
            if (!orgData.name || orgData.name.length < 2 || orgData.name.length > 100) {
                errors.push('Organization name must be between 2 and 100 characters');
            }
            
            if (orgData.description && orgData.description.length > 500) {
                errors.push('Organization description must be less than 500 characters');
            }
            
            if (orgData.tier && !['BASIC', 'PRO', 'ENTERPRISE'].includes(orgData.tier)) {
                errors.push('Organization tier must be BASIC, PRO, or ENTERPRISE');
            }
            
            return errors;
        },
        
        // Clear organization cache
        clearOrganizationCache: function() {
            organizationFixtures.organizationCache = {};
        },
        
        // Get test organization data
        getTestOrganizationData: function(type) {
            type = type || 'default';
            
            if (type === 'default') {
                return orgTestData.defaultOrganization;
            } else if (type === 'academic') {
                return orgTestData.testOrganizations[0];
            } else if (type === 'corporate') {
                return orgTestData.testOrganizations[1];
            } else if (type === 'community') {
                return orgTestData.testOrganizations[2];
            }
            
            return orgTestData.defaultOrganization;
        },
        
        // Create organization for specific tier
        createOrganizationForTier: function(tier, authToken) {
            var orgData = organizationFixtures.getTestOrganizationData('default');
            orgData.name = config.utils.generateOrgName(tier.toLowerCase() + '-org');
            orgData.tier = tier;
            
            // Set features based on tier
            if (tier === 'BASIC') {
                orgData.features = {
                    aiAssistant: false,
                    advancedAnalytics: false,
                    customBranding: false,
                    apiAccess: false,
                    webhooks: false,
                    sso: false,
                    auditLogs: false
                };
            } else if (tier === 'PRO') {
                orgData.features = {
                    aiAssistant: true,
                    advancedAnalytics: true,
                    customBranding: false,
                    apiAccess: true,
                    webhooks: false,
                    sso: true,
                    auditLogs: true
                };
            } else if (tier === 'ENTERPRISE') {
                orgData.features = {
                    aiAssistant: true,
                    advancedAnalytics: true,
                    customBranding: true,
                    apiAccess: true,
                    webhooks: true,
                    sso: true,
                    auditLogs: true
                };
            }
            
            return organizationFixtures.createOrganization(orgData, authToken);
        }
    };
    
    return organizationFixtures;
}