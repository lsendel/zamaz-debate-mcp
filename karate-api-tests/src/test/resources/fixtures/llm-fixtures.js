/**
 * LLM Service Fixtures and Utilities
 * This file contains reusable LLM service functions and test data
 */

function fn() {
    var config = karate.callSingle('classpath:karate-config.js');
    var authFixtures = karate.callSingle('classpath:fixtures/auth-fixtures.js');
    
    var llmFixtures = {
        // LLM providers configuration
        providers: {
            claude: {
                name: 'claude',
                models: ['claude-3-opus', 'claude-3-sonnet', 'claude-3-haiku'],
                maxTokens: 100000,
                temperature: { min: 0.0, max: 2.0 },
                streaming: true
            },
            openai: {
                name: 'openai',
                models: ['gpt-4', 'gpt-4-turbo', 'gpt-3.5-turbo'],
                maxTokens: 32000,
                temperature: { min: 0.0, max: 2.0 },
                streaming: true
            },
            gemini: {
                name: 'gemini',
                models: ['gemini-pro', 'gemini-ultra'],
                maxTokens: 30000,
                temperature: { min: 0.0, max: 1.0 },
                streaming: true
            }
        },
        
        // Generate completion request
        generateCompletionRequest: function(overrides) {
            var defaultRequest = {
                prompt: "What is the capital of France?",
                maxTokens: 100,
                temperature: 0.7,
                model: "claude-3-sonnet",
                provider: "claude",
                enableCaching: true,
                streaming: false
            };
            
            return Object.assign(defaultRequest, overrides || {});
        },
        
        // Generate streaming completion request
        generateStreamingRequest: function(overrides) {
            var baseRequest = llmFixtures.generateCompletionRequest(overrides);
            baseRequest.streaming = true;
            return baseRequest;
        },
        
        // Generate complex prompt for testing
        generateComplexPrompt: function(type) {
            var prompts = {
                debate: "You are participating in a debate about artificial intelligence regulation. Present three strong arguments for why AI should be regulated by government agencies, including specific examples and potential risks.",
                analysis: "Analyze the following debate transcript and provide a summary of the main arguments from each side, identifying the strongest points and any logical fallacies present.",
                creative: "Write a creative story about a future society where AI and humans collaborate to solve climate change. The story should be engaging and thought-provoking.",
                technical: "Explain the concept of machine learning in simple terms, then provide a more detailed technical explanation including key algorithms and their applications.",
                long: "Write a comprehensive essay about the history of artificial intelligence, covering major milestones, key figures, breakthrough technologies, current applications, and future possibilities. Include at least 1000 words with proper structure and citations."
            };
            
            return prompts[type] || prompts.debate;
        },
        
        // Create completion
        createCompletion: function(requestData, authToken) {
            if (!authToken) {
                var auth = authFixtures.login();
                authToken = auth.token;
            }
            
            var completionRequest = llmFixtures.generateCompletionRequest(requestData);
            
            var response = karate.call('classpath:llm/create-completion.feature', {
                completionRequest: completionRequest,
                authToken: authToken,
                baseUrl: config.serviceUrls.llm
            });
            
            if (response.response) {
                return response.response;
            }
            
            throw new Error('Failed to create completion');
        },
        
        // Create streaming completion
        createStreamingCompletion: function(requestData, authToken) {
            if (!authToken) {
                var auth = authFixtures.login();
                authToken = auth.token;
            }
            
            var streamingRequest = llmFixtures.generateStreamingRequest(requestData);
            
            var response = karate.call('classpath:llm/create-streaming-completion.feature', {
                streamingRequest: streamingRequest,
                authToken: authToken,
                baseUrl: config.serviceUrls.llm
            });
            
            if (response.response) {
                return response.response;
            }
            
            throw new Error('Failed to create streaming completion');
        },
        
        // List available providers
        listProviders: function(authToken) {
            if (!authToken) {
                var auth = authFixtures.login();
                authToken = auth.token;
            }
            
            var response = karate.call('classpath:llm/list-providers.feature', {
                authToken: authToken,
                baseUrl: config.serviceUrls.llm
            });
            
            if (response.response && response.response.providers) {
                return response.response.providers;
            }
            
            return [];
        },
        
        // Check provider health
        checkProviderHealth: function(providerId, authToken) {
            if (!authToken) {
                var auth = authFixtures.login();
                authToken = auth.token;
            }
            
            var response = karate.call('classpath:llm/check-provider-health.feature', {
                providerId: providerId,
                authToken: authToken,
                baseUrl: config.serviceUrls.llm
            });
            
            if (response.response) {
                return response.response;
            }
            
            throw new Error('Failed to check provider health: ' + providerId);
        },
        
        // Get provider models
        getProviderModels: function(providerId, authToken) {
            if (!authToken) {
                var auth = authFixtures.login();
                authToken = auth.token;
            }
            
            var response = karate.call('classpath:llm/get-provider-models.feature', {
                providerId: providerId,
                authToken: authToken,
                baseUrl: config.serviceUrls.llm
            });
            
            if (response.response && response.response.models) {
                return response.response.models;
            }
            
            return [];
        },
        
        // Estimate tokens
        estimateTokens: function(text, model, authToken) {
            if (!authToken) {
                var auth = authFixtures.login();
                authToken = auth.token;
            }
            
            var response = karate.call('classpath:llm/estimate-tokens.feature', {
                text: text,
                model: model || 'claude-3-sonnet',
                authToken: authToken,
                baseUrl: config.serviceUrls.llm
            });
            
            if (response.response && response.response.tokenCount) {
                return response.response.tokenCount;
            }
            
            return 0;
        },
        
        // Validate completion response
        validateCompletionResponse: function(response, expectedProvider, expectedModel) {
            var validationErrors = [];
            
            if (!response.id || typeof response.id !== 'string') {
                validationErrors.push('Missing or invalid completion ID');
            }
            
            if (!response.content || typeof response.content !== 'string') {
                validationErrors.push('Missing or invalid completion content');
            }
            
            if (!response.provider || typeof response.provider !== 'string') {
                validationErrors.push('Missing or invalid provider');
            }
            
            if (!response.model || typeof response.model !== 'string') {
                validationErrors.push('Missing or invalid model');
            }
            
            if (expectedProvider && response.provider !== expectedProvider) {
                validationErrors.push('Provider mismatch: expected ' + expectedProvider + ', got ' + response.provider);
            }
            
            if (expectedModel && response.model !== expectedModel) {
                validationErrors.push('Model mismatch: expected ' + expectedModel + ', got ' + response.model);
            }
            
            if (typeof response.usage !== 'object' || !response.usage.inputTokens || !response.usage.outputTokens) {
                validationErrors.push('Missing or invalid usage information');
            }
            
            if (typeof response.finishReason !== 'string') {
                validationErrors.push('Missing or invalid finish reason');
            }
            
            if (!response.createdAt || typeof response.createdAt !== 'string') {
                validationErrors.push('Missing or invalid creation timestamp');
            }
            
            return validationErrors;
        },
        
        // Generate test prompts for different scenarios
        generateTestPrompts: function() {
            return {
                simple: "Hello, how are you?",
                medium: "Explain the concept of machine learning in simple terms.",
                complex: llmFixtures.generateComplexPrompt('debate'),
                creative: llmFixtures.generateComplexPrompt('creative'),
                technical: llmFixtures.generateComplexPrompt('technical'),
                long: llmFixtures.generateComplexPrompt('long'),
                multilingual: "Bonjour, comment allez-vous? Please respond in both French and English.",
                code: "Write a Python function that calculates the fibonacci sequence up to n terms.",
                math: "Solve this equation: 2x + 5 = 15. Show your work step by step.",
                reasoning: "A farmer has 17 sheep. All but 9 died. How many sheep does the farmer have left? Explain your reasoning.",
                empty: "",
                special_chars: "Test with special characters: !@#$%^&*()_+-=[]{}|;:,.<>?",
                unicode: "Test with unicode: 🚀 🌟 💡 🎯 ✨ 🔥 🌈 🎉",
                html: "Test with HTML: <script>alert('test')</script> <b>bold</b> <i>italic</i>",
                json: "Test with JSON: {\"key\": \"value\", \"number\": 42, \"array\": [1, 2, 3]}",
                sql: "Test with SQL: SELECT * FROM users WHERE id = 1; DROP TABLE users;",
                very_long: "This is a very long prompt that exceeds normal length limits. ".repeat(100)
            };
        },
        
        // Generate performance test scenarios
        generatePerformanceScenarios: function() {
            return {
                concurrent_requests: {
                    description: "Test concurrent completion requests",
                    requests: 10,
                    prompt: "What is artificial intelligence?",
                    expected_max_time: 30000
                },
                large_prompt: {
                    description: "Test with large prompt",
                    requests: 1,
                    prompt: llmFixtures.generateComplexPrompt('long'),
                    expected_max_time: 60000
                },
                streaming_performance: {
                    description: "Test streaming completion performance",
                    requests: 5,
                    prompt: "Write a detailed explanation of quantum computing.",
                    streaming: true,
                    expected_max_time: 45000
                },
                rate_limiting: {
                    description: "Test rate limiting behavior",
                    requests: 100,
                    prompt: "Hello",
                    expected_rate_limit: true
                }
            };
        },
        
        // Mock provider responses for testing
        mockProviderResponse: function(provider, model, success) {
            if (success !== false) {
                return {
                    id: "completion-" + config.utils.randomString(10),
                    content: "This is a mock response from " + provider + " using " + model,
                    provider: provider,
                    model: model,
                    usage: {
                        inputTokens: 10,
                        outputTokens: 15,
                        totalTokens: 25
                    },
                    finishReason: "stop",
                    createdAt: new Date().toISOString()
                };
            } else {
                return {
                    error: {
                        code: "PROVIDER_ERROR",
                        message: "Mock provider error for testing",
                        provider: provider,
                        model: model
                    }
                };
            }
        },
        
        // Wait for streaming completion
        waitForStreamingCompletion: function(streamId, timeout) {
            timeout = timeout || 30000;
            var startTime = Date.now();
            var chunks = [];
            var completed = false;
            
            while (!completed && (Date.now() - startTime) < timeout) {
                // This would be replaced with actual streaming logic
                // For now, we simulate streaming completion
                java.lang.Thread.sleep(100);
                
                if (Date.now() - startTime > 1000) {
                    chunks.push({
                        id: streamId,
                        content: "Streaming chunk " + chunks.length,
                        finished: chunks.length >= 5
                    });
                    
                    if (chunks.length >= 5) {
                        completed = true;
                    }
                }
            }
            
            return {
                streamId: streamId,
                chunks: chunks,
                completed: completed,
                totalChunks: chunks.length,
                duration: Date.now() - startTime
            };
        },
        
        // Calculate response quality metrics
        calculateQualityMetrics: function(prompt, response) {
            var metrics = {
                length: response.length,
                wordCount: response.split(/\s+/).length,
                relevance: 0.0,
                coherence: 0.0,
                completeness: 0.0,
                accuracy: 0.0
            };
            
            // Simple heuristic-based quality assessment
            if (response.length > 10) {
                metrics.relevance += 0.3;
            }
            
            if (response.split('.').length > 1) {
                metrics.coherence += 0.3;
            }
            
            if (response.length > prompt.length * 0.5) {
                metrics.completeness += 0.4;
            }
            
            // Check for common quality indicators
            if (response.includes('because') || response.includes('therefore') || response.includes('however')) {
                metrics.coherence += 0.2;
            }
            
            if (response.toLowerCase().includes(prompt.toLowerCase().split(' ')[0])) {
                metrics.relevance += 0.2;
            }
            
            // Overall quality score
            metrics.overallScore = (metrics.relevance + metrics.coherence + metrics.completeness + metrics.accuracy) / 4;
            
            return metrics;
        }
    };
    
    return llmFixtures;
}