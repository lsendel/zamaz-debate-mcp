/**
 * Authentication Fixtures and Utilities
 * This file contains reusable authentication functions and test data
 */

function fn() {
    var config = karate.callSingle('classpath:karate-config.js');
    
    var authFixtures = {
        // Authentication tokens cache
        tokenCache: {},
        
        // Login and get authentication token
        login: function(email, password, organizationId) {
            email = email || config.testData.defaultUser.email;
            password = password || config.testData.defaultUser.password;
            organizationId = organizationId || config.testData.defaultOrganization;
            
            var cacheKey = email + '|' + organizationId;
            
            // Check cache first
            if (authFixtures.tokenCache[cacheKey]) {
                var cachedToken = authFixtures.tokenCache[cacheKey];
                // Check if token is still valid (simple expiry check)
                if (cachedToken.expiresAt > Date.now()) {
                    return cachedToken;
                }
            }
            
            var loginRequest = {
                email: email,
                password: password,
                organizationId: organizationId
            };
            
            var loginResponse = karate.call('classpath:authentication/login.feature', {
                loginRequest: loginRequest,
                baseUrl: config.serviceUrls.gateway
            });
            
            if (loginResponse.response && loginResponse.response.token) {
                var token = {
                    token: loginResponse.response.token,
                    refreshToken: loginResponse.response.refreshToken,
                    expiresAt: Date.now() + (3600 * 1000), // 1 hour
                    user: loginResponse.response.user
                };
                authFixtures.tokenCache[cacheKey] = token;
                return token;
            }
            
            throw new Error('Login failed for user: ' + email);
        },
        
        // Register new user
        register: function(userData) {
            userData = userData || {};
            var defaultUserData = {
                email: config.utils.generateEmail('test'),
                password: 'TestPassword123!',
                name: 'Test User ' + config.utils.randomString(5),
                organizationId: config.testData.defaultOrganization
            };
            
            var finalUserData = Object.assign(defaultUserData, userData);
            
            var registerResponse = karate.call('classpath:authentication/register.feature', {
                registerRequest: finalUserData,
                baseUrl: config.serviceUrls.gateway
            });
            
            if (registerResponse.response && registerResponse.response.token) {
                return {
                    token: registerResponse.response.token,
                    refreshToken: registerResponse.response.refreshToken,
                    user: registerResponse.response.user,
                    userData: finalUserData
                };
            }
            
            throw new Error('Registration failed for user: ' + finalUserData.email);
        },
        
        // Get authentication headers
        getAuthHeaders: function(token) {
            if (typeof token === 'string') {
                return {
                    'Authorization': 'Bearer ' + token,
                    'Content-Type': 'application/json',
                    'X-Organization-Id': config.testData.defaultOrganization
                };
            } else if (token && token.token) {
                return {
                    'Authorization': 'Bearer ' + token.token,
                    'Content-Type': 'application/json',
                    'X-Organization-Id': config.testData.defaultOrganization
                };
            }
            throw new Error('Invalid token provided');
        },
        
        // Refresh authentication token
        refreshToken: function(refreshToken) {
            var refreshRequest = {
                refreshToken: refreshToken
            };
            
            var refreshResponse = karate.call('classpath:authentication/refresh-token.feature', {
                refreshRequest: refreshRequest,
                baseUrl: config.serviceUrls.gateway
            });
            
            if (refreshResponse.response && refreshResponse.response.token) {
                return {
                    token: refreshResponse.response.token,
                    refreshToken: refreshResponse.response.refreshToken,
                    expiresAt: Date.now() + (3600 * 1000)
                };
            }
            
            throw new Error('Token refresh failed');
        },
        
        // Logout user
        logout: function(token) {
            var logoutResponse = karate.call('classpath:authentication/logout.feature', {
                token: token,
                baseUrl: config.serviceUrls.gateway
            });
            
            // Clear token from cache
            Object.keys(authFixtures.tokenCache).forEach(function(key) {
                if (authFixtures.tokenCache[key].token === token) {
                    delete authFixtures.tokenCache[key];
                }
            });
            
            return logoutResponse.response;
        },
        
        // Create admin user for testing
        createAdminUser: function() {
            var adminData = {
                email: config.utils.generateEmail('admin'),
                password: 'AdminPassword123!',
                name: 'Admin User ' + config.utils.randomString(5),
                role: 'ADMIN',
                organizationId: config.testData.defaultOrganization
            };
            
            return authFixtures.register(adminData);
        },
        
        // Create organization and admin user
        createOrganizationWithAdmin: function(orgName) {
            orgName = orgName || config.utils.generateOrgName();
            
            // First create organization
            var orgData = {
                name: orgName,
                description: 'Test organization for ' + orgName,
                settings: {
                    allowPublicDebates: true,
                    maxDebateParticipants: 10
                }
            };
            
            var orgResponse = karate.call('classpath:organization/create-organization.feature', {
                organizationRequest: orgData,
                baseUrl: config.serviceUrls.organization
            });
            
            if (orgResponse.response && orgResponse.response.id) {
                // Create admin user for this organization
                var adminData = {
                    email: config.utils.generateEmail('admin'),
                    password: 'AdminPassword123!',
                    name: 'Admin User for ' + orgName,
                    role: 'ADMIN',
                    organizationId: orgResponse.response.id
                };
                
                var adminAuth = authFixtures.register(adminData);
                
                return {
                    organization: orgResponse.response,
                    admin: adminAuth
                };
            }
            
            throw new Error('Failed to create organization: ' + orgName);
        },
        
        // Validate token structure
        validateTokenStructure: function(token) {
            if (!token || typeof token !== 'string') {
                return false;
            }
            
            var parts = token.split('.');
            if (parts.length !== 3) {
                return false;
            }
            
            try {
                // Decode header and payload (simple validation)
                var header = JSON.parse(java.util.Base64.getDecoder().decode(parts[0]));
                var payload = JSON.parse(java.util.Base64.getDecoder().decode(parts[1]));
                
                return header.alg && header.typ && payload.sub && payload.exp;
            } catch (e) {
                return false;
            }
        },
        
        // Clear all cached tokens
        clearTokenCache: function() {
            authFixtures.tokenCache = {};
        },
        
        // Get user info from token
        getUserInfoFromToken: function(token) {
            if (!token || typeof token !== 'string') {
                return null;
            }
            
            var parts = token.split('.');
            if (parts.length !== 3) {
                return null;
            }
            
            try {
                var payload = JSON.parse(java.util.Base64.getDecoder().decode(parts[1]));
                return {
                    userId: payload.sub,
                    organizationId: payload.organizationId,
                    role: payload.role,
                    email: payload.email,
                    expiresAt: payload.exp * 1000
                };
            } catch (e) {
                return null;
            }
        }
    };
    
    return authFixtures;
}