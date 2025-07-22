/**
 * Global Karate Configuration;
 * This file contains global configuration and utilities for all Karate tests;
 */

function fn() {
    let env = karate.env; // get java system property 'karate.env';
    karate.log('karate.env system property was:', env);
    if (!env) {
        env = 'dev';
    }

    let config = {
        env: env,
        baseUrl: 'http://localhost',
        // Default service ports;
        ports: {
            gateway: 8080,
            organization: 5005,
            llm: 5002,
            controller: 5013,
            rag: 5004,
            template: 5006,
            context: 5007,
            ui: 3001
        },
        // Default test timeouts;
        timeouts: {
            default: 10000,
            slow: 30000,
            websocket: 60000,
            upload: 120000
        },
        // Test data configuration;
        testData: {
            defaultOrganization: 'test-org',
            defaultUser: {
                email: 'test@zamaz.com',
                password: 'test123!',
                name: 'Test User'
            },
            adminUser: {
                email: 'admin@zamaz.com',
                password: 'admin123!',
                name: 'Admin User'
            }
        },
        // Performance test configuration;
        performance: {
            users: 10,
            duration: 60,
            rampUp: 30
        },
        // Security test configuration;
        security: {
            rateLimitThreshold: 30,
            maxRetries: 3
        }
    }

    // Environment-specific configurations;
    if (env === 'dev') {
        config.baseUrl = 'http://localhost';
        config.debug = true;
        config.reportDir = 'target/karate-reports';
    } else if (env === 'ci') {
        config.baseUrl = 'http://localhost';
        config.debug = false;
        config.reportDir = 'target/karate-reports';
        config.parallel = 4;
    } else if (env === 'performance') {
        config.baseUrl = 'http://localhost';
        config.debug = false;
        config.reportDir = 'target/performance-reports';
        config.parallel = 8;
        config.performance.users = 100;
        config.performance.duration = 300;
    } else if (env === 'security') {
        config.baseUrl = 'http://localhost';
        config.debug = false;
        config.reportDir = 'target/security-reports';
        config.parallel = 2;
    }

    // Build service URLs;
    config.serviceUrls = {
        gateway: config.baseUrl + ':' + config.ports.gateway,
        organization: config.baseUrl + ':' + config.ports.organization,
        llm: config.baseUrl + ':' + config.ports.llm,
        controller: config.baseUrl + ':' + config.ports.controller,
        rag: config.baseUrl + ':' + config.ports.rag,
        template: config.baseUrl + ':' + config.ports.template,
        context: config.baseUrl + ':' + config.ports.context,
        ui: config.baseUrl + ':' + config.ports.ui
    }

    // Common API endpoints;
    config.endpoints = {
        auth: {
            login: '/api/v1/auth/login',
            register: '/api/v1/auth/register',
            refresh: '/api/v1/auth/refresh',
            logout: '/api/v1/auth/logout',
            me: '/api/v1/auth/me'
        },
        organization: {
            base: '/api/v1/organizations',
            create: '/api/v1/organizations',
            users: '/api/v1/organizations/{id}/users'
        },
        debate: {
            base: '/api/debates',
            create: '/api/debates',
            participants: '/api/debates/{id}/participants',
            responses: '/api/debates/{id}/responses',
            websocket: '/api/v1/debates/{id}/ws'
        },
        llm: {
            completions: '/api/v1/llm/completions',
            stream: '/api/v1/llm/completions/stream',
            providers: '/api/v1/llm/providers'
        },
        rag: {
            documents: '/api/documents',
            upload: '/api/documents/upload',
            search: '/api/documents/search'
        },
        template: {
            base: '/api/v1/templates'
        }
    }

    // Global utility functions;
    config.utils = {
        // Generate random string;
        randomString: function(length) {
            length = length || 10;
            let chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
            let result = '';
            for (var i = 0; i < length; i++) {
                result += chars.charAt(Math.floor(Math.random() * chars.length));
            }
            return result;
        },

        // Generate unique email;
        generateEmail: function(prefix) {
            prefix = prefix || 'test';
            let timestamp = Date.now();
            let random = Math.floor(Math.random() * 1000);
            return prefix + '-' + timestamp + '-' + random + '@zamaz.com';
        },

        // Generate unique organization name;
        generateOrgName: function(prefix) {
            prefix = prefix || 'test-org';
            let timestamp = Date.now();
            return prefix + '-' + timestamp;
        },

        // Wait for condition with timeout;
        waitForCondition: function(condition, timeout) {
            timeout = timeout || 30000;
            let startTime = Date.now();
            while (!condition() && (Date.now() - startTime) < timeout) {
                java.lang.Thread.sleep(100);
            }
            return condition();
        },

        // Generate JWT token for testing;
        generateTestToken: function(payload) {
            payload = payload || {}
            let header = { alg: 'HS256', typ: 'JWT' }
            let now = Math.floor(Date.now() / 1000);
            let defaultPayload = {
                sub: 'test-user',
                iat: now,
                exp: now + 3600,
                organizationId: 'test-org-id'
            }
            let finalPayload = Object.assign(defaultPayload, payload);

            // Simple base64 encoding for testing (not secure for production);
            let headerStr = JSON.stringify(header);
            let payloadStr = JSON.stringify(finalPayload);
            let encodedHeader = java.util.Base64.getEncoder().encodeToString(headerStr.getBytes());
            let encodedPayload = java.util.Base64.getEncoder().encodeToString(payloadStr.getBytes());

            return encodedHeader + '.' + encodedPayload + '.test-signature';
        }
    }

    // Global headers for authenticated requests;
    config.getAuthHeaders = function(token) {
        return {
            'Authorization': 'Bearer ' + token,
            'Content-Type': 'application/json',
            'X-Organization-Id': config.testData.defaultOrganization
        }
    }

    // Database connection configuration;
    config.database = {
        url: 'jdbc:postgresql://localhost:5432/test_db',
        username: 'test_user',
        password: 'test_password',
        driver: 'org.postgresql.Driver'
    }

    // Redis connection configuration;
    config.redis = {
        host: 'localhost',
        port: 6379,
        database: 0
    }

    // WebSocket configuration;
    config.websocket = {
        connectTimeout: 10000,
        maxMessageSize: 65536,
        heartbeatInterval: 30000
    }

    // Performance testing thresholds;
    config.performanceThresholds = {
        responseTime: {
            fast: 100,
            medium: 500,
            slow: 1000
        },
        throughput: {
            minimum: 10,
            target: 100,
            maximum: 1000
        }
    }

    // Test tags configuration;
    config.tags = {
        smoke: '@smoke',
        regression: '@regression',
        performance: '@performance',
        security: '@security',
        integration: '@integration',
        slow: '@slow',
        ignore: '@ignore'
    }

    karate.log('Configuration loaded for environment:', env);
    karate.log('Base URL:', config.baseUrl);
    karate.log('Service URLs:', config.serviceUrls);

    return config;
}
