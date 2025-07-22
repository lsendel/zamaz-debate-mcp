/**
 * Evidence Generator for Karate API Tests
 * This utility generates comprehensive evidence for each subproject
 */

function fn() {
        // Generate evidence for a test scenario
        generatescenarioevidence: function(scenarioName, service, testData) {
            var timestamp = new Date().toISOString();
            var evidence = {
                scenario: scenarioName,
                service: service,
                timestamp: timestamp,
                testData: testData,
                execution: {
                    startTime: Date.now(),
                    endTime: null,
                    duration: null,
                    status: 'running'
                },
                requests: [],
                responses: [],
                validations: [],
                errors: [],
                metrics: {
                    totalRequests: 0,
                    successfulRequests: 0,
                    failedRequests: 0,
                    averageResponseTime: 0,
                    maxResponseTime: 0,
                    minResponseTime: 0
                }
            };
            
            return evidence;
        },
        
        // Record HTTP request
        recordrequest: function(evidence, request) {
            var requestRecord = {
                timestamp: new Date().toISOString(),
                method: request.method,
                url: request.url,
                headers: request.headers,
                body: request.body,
                startTime: Date.now()
            };
            
            evidence.requests.push(requestRecord);
            evidence.metrics.totalRequests++;
            
            return requestRecord;
        },
        
        // Record HTTP response
        recordresponse: function(evidence, requestRecord, response) {
            var endTime = Date.now();
            var responseTime = endTime - requestRecord.startTime;
            
            var responseRecord = {
                timestamp: new Date().toISOString(),
                status: response.status,
                headers: response.headers,
                body: response.body,
                responseTime: responseTime,
                requestId: evidence.requests.length - 1
            };
            
            evidence.responses.push(responseRecord);
            
            // Update metrics
            if (response.status >= 200 && response.status < 300) {
                evidence.metrics.successfulRequests++;
            } else {
                evidence.metrics.failedRequests++;
            }
            
            // Update response time metrics
            if (evidence.metrics.maxResponseTime === 0 || responseTime > evidence.metrics.maxResponseTime) {
                evidence.metrics.maxResponseTime = responseTime;
            }
            
            if (evidence.metrics.minResponseTime === 0 || responseTime < evidence.metrics.minResponseTime) {
                evidence.metrics.minResponseTime = responseTime;
            }
            
            var totalResponseTime = evidence.responses.reduce(function(sum, r) { return sum + r.responseTime; }, 0);
            evidence.metrics.averageResponseTime = totalResponseTime / evidence.responses.length;
            
            return responseRecord;
        },
        
        // Record validation result
        recordvalidation: function(evidence, validationType, result, details) {
            var validationRecord = {
                timestamp: new Date().toISOString(),
                type: validationType,
                result: result,
                details: details,
                passed: result === 'PASS'
            };
            
            evidence.validations.push(validationRecord);
            
            return validationRecord;
        },
        
        // Record error
        recorderror: function(evidence, error, context) {
            var errorRecord = {
                timestamp: new Date().toISOString(),
                message: error.message || error,
                stack: error.stack,
                context: context,
                severity: 'ERROR'
            };
            
            evidence.errors.push(errorRecord);
            
            return errorRecord;
        },
        
        // Complete evidence recording
        completeevidence: function(evidence, status) {
            evidence.execution.endTime = Date.now();
            evidence.execution.duration = evidence.execution.endTime - evidence.execution.startTime;
            evidence.execution.status = status || 'completed';
            
            return evidence;
        },
        
        // Generate evidence report
        generatereport: function(evidence) {
            var report = {
                summary: {
                    scenario: evidence.scenario,
                    service: evidence.service,
                    timestamp: evidence.timestamp,
                    duration: evidence.execution.duration,
                    status: evidence.execution.status,
                    totalRequests: evidence.metrics.totalRequests,
                    successRate: (evidence.metrics.successfulRequests / evidence.metrics.totalRequests * 100).toFixed(2) + '%',
                    averageResponseTime: evidence.metrics.averageResponseTime.toFixed(2) + 'ms',
                    validationsPassed: evidence.validations.filter(v => v.passed).length,
                    validationsFailed: evidence.validations.filter(v => !v.passed).length,
                    errorsCount: evidence.errors.length
                },
                details: {
                    requests: evidence.requests,
                    responses: evidence.responses,
                    validations: evidence.validations,
                    errors: evidence.errors,
                    metrics: evidence.metrics
                },
                recommendations: evidenceGenerator.generateRecommendations(evidence)
            };
            
            return report;
        },
        
        // Generate recommendations based on evidence
        generaterecommendations: function(evidence) {
            var recommendations = [];
            
            // Performance recommendations
            if (evidence.metrics.averageResponseTime > 1000) {
                recommendations.push({
                    type: 'PERFORMANCE',
                    priority: 'HIGH',
                    message: 'Average response time is high (' + evidence.metrics.averageResponseTime.toFixed(2) + 'ms). Consider optimizing API performance.',
                    suggestion: 'Review database queries, add caching, or optimize business logic.'
                });
            }
            
            // Error rate recommendations
            var errorRate = evidence.metrics.failedRequests / evidence.metrics.totalRequests;
            if (errorRate > 0.05) {
                recommendations.push({
                    type: 'RELIABILITY',
                    priority: 'HIGH',
                    message: 'Error rate is high (' + (errorRate * 100).toFixed(2) + '%). Review error handling and input validation.',
                    suggestion: 'Implement better error handling, input validation, and add monitoring.'
                });
            }
            
            // Validation recommendations
            var validationFailures = evidence.validations.filter(v => !v.passed);
            if (validationFailures.length > 0) {
                recommendations.push({
                    type: 'QUALITY',
                    priority: 'MEDIUM',
                    message: validationFailures.length + ' validation(s) failed. Review API contracts and response formats.',
                    suggestion: 'Update API documentation, fix response schemas, or adjust validation rules.'
                });
            }
            
            // Security recommendations
            var securityErrors = evidence.errors.filter(e => e.context && e.context.includes('security'));
            if (securityErrors.length > 0) {
                recommendations.push({
                    type: 'SECURITY',
                    priority: 'CRITICAL',
                    message: 'Security-related errors detected. Review authentication and authorization.',
                    suggestion: 'Implement proper authentication, authorization, and input sanitization.'
                });
            }
            
            return recommendations;
        },
        
        // Generate service-specific evidence
        generateserviceevidence: function(serviceName, testResults) {
            var serviceEvidence = {
                service: serviceName,
                timestamp: new Date().toISOString(),
                testSuite: {
                    totalScenarios: testResults.length,
                    passedScenarios: testResults.filter(r => r.execution.status === 'passed').length,
                    failedScenarios: testResults.filter(r => r.execution.status === 'failed').length,
                    skippedScenarios: testResults.filter(r => r.execution.status === 'skipped').length
                },
                coverage: {
                    apiEndpoints: evidenceGenerator.calculateApiCoverage(serviceName, testResults),
                    httpMethods: evidenceGenerator.calculateMethodCoverage(testResults),
                    statusCodes: evidenceGenerator.calculateStatusCodeCoverage(testResults)
                },
                performance: {
                    totalRequests: testResults.reduce((sum, r) => sum + r.metrics.totalRequests, 0),
                    averageResponseTime: evidenceGenerator.calculateOverallAverageResponseTime(testResults),
                    maxResponseTime: Math.max(...testResults.map(r => r.metrics.maxResponseTime)),
                    minResponseTime: Math.min(...testResults.map(r => r.metrics.minResponseTime))
                },
                quality: {
                    totalValidations: testResults.reduce((sum, r) => sum + r.validations.length, 0),
                    passedValidations: testResults.reduce((sum, r) => sum + r.validations.filter(v => v.passed).length, 0),
                    failedValidations: testResults.reduce((sum, r) => sum + r.validations.filter(v => !v.passed).length, 0)
                },
                errors: testResults.reduce((acc, r) => acc.concat(r.errors), []),
                recommendations: testResults.reduce((acc, r) => acc.concat(evidenceGenerator.generateRecommendations(r)), [])
            };
            
            return serviceEvidence;
        },
        
        // Calculate API coverage
        calculateapicoverage: function(serviceName, testResults) {
            var endpointMap = {
                'organization': [
                    'POST /api/v1/organizations',
                    'GET /api/v1/organizations',
                    'GET /api/v1/organizations/{id}',
                    'PUT /api/v1/organizations/{id}',
                    'DELETE /api/v1/organizations/{id}',
                    'POST /api/v1/organizations/{id}/users',
                    'GET /api/v1/organizations/{id}/users',
                    'DELETE /api/v1/organizations/{id}/users/{userId}'
                ],
                'llm': [
                    'POST /api/v1/llm/completions',
                    'POST /api/v1/llm/completions/stream',
                    'GET /api/v1/llm/providers',
                    'GET /api/v1/llm/providers/{id}/health',
                    'GET /api/v1/llm/providers/{id}/models',
                    'POST /api/v1/llm/tokens/estimate'
                ],
                'debate': [
                    'POST /api/debates',
                    'GET /api/debates',
                    'GET /api/debates/{id}',
                    'PUT /api/debates/{id}',
                    'DELETE /api/debates/{id}',
                    'POST /api/debates/{id}/participants',
                    'POST /api/debates/{id}/start',
                    'POST /api/debates/{id}/responses',
                    'GET /api/debates/{id}/analysis'
                ],
                'rag': [
                    'POST /api/knowledge-bases/{id}/documents',
                    'GET /api/documents/{id}',
                    'DELETE /api/documents/{id}',
                    'POST /api/knowledge-bases/{id}/search',
                    'POST /api/documents/{id}/process',
                    'POST /api/embeddings/generate'
                ]
            };
            
            var expectedEndpoints = endpointMap[serviceName] || [];
            var testedEndpoints = [];
            
            testResults.forEach(function(result) {
                result.requests.forEach(function(request) {
                    var endpoint = request.method + ' ' + request.url.replace(/\/\d+/g, '/{id}');
                    if (testedEndpoints.indexOf(endpoint) === -1) {
                        testedEndpoints.push(endpoint);
                    }
                });
            });
            
            return {
                expected: expectedEndpoints.length,
                tested: testedEndpoints.length,
                coverage: expectedEndpoints.length > 0 ? (testedEndpoints.length / expectedEndpoints.length * 100).toFixed(2) + '%' : '0%',
                missing: expectedEndpoints.filter(e => testedEndpoints.indexOf(e) === -1),
                tested: testedEndpoints
            };
        },
        
        // Calculate method coverage
        calculatemethodcoverage: function(testResults) {
            var methods = {};
            
            testResults.forEach(function(result) {
                result.requests.forEach(function(request) {
                    methods[request.method] = (methods[request.method] || 0) + 1;
                });
            });
            
            return methods;
        },
        
        // Calculate status code coverage
        calculatestatuscodecoverage: function(testResults) {
            var statusCodes = {};
            
            testResults.forEach(function(result) {
                result.responses.forEach(function(response) {
                    statusCodes[response.status] = (statusCodes[response.status] || 0) + 1;
                });
            });
            
            return statusCodes;
        },
        
        // Calculate overall average response time
        calculateoverallaverageresponsetime: function(testResults) {
            var totalResponseTime = 0;
            var totalRequests = 0;
            
            testResults.forEach(function(result) {
                totalResponseTime += result.metrics.averageResponseTime * result.metrics.totalRequests;
                totalRequests += result.metrics.totalRequests;
            });
            
            return totalRequests > 0 ? totalResponseTime / totalRequests : 0;
        },
        
        // Generate HTML evidence report
        generatehtmlreport: function(serviceEvidence) {
            var html = `
<!DOCTYPE html>
<html>
<head>
    <title>API Test Evidence Report - ${serviceEvidence.service}</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .header { background: #f4f4f4; padding: 20px; border-radius: 5px; margin-bottom: 20px; }
        .section { margin: 20px 0; padding: 15px; border: 1px solid #ddd; border-radius: 5px; }
        .success { color: #28a745; }
        .warning { color: #ffc107; }
        .danger { color: #dc3545; }
        .info { color: #17a2b8; }
        table { width: 100%; border-collapse: collapse; margin: 10px 0; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #f2f2f2; }
        .metric { display: inline-block; margin: 10px; padding: 15px; background: #f8f9fa; border-radius: 5px; }
        .recommendations { background: #fff3cd; padding: 15px; border-radius: 5px; }
        .recommendation { margin: 10px 0; padding: 10px; border-left: 4px solid #ffc107; }
        .recommendation.high { border-left-color: #dc3545; }
        .recommendation.critical { border-left-color: #721c24; }
    </style>
</head>
<body>
    <div class="header">
        <h1>API Test Evidence Report</h1>
        <h2>Service: ${serviceEvidence.service}</h2>
        <p>Generated: ${serviceEvidence.timestamp}</p>
    </div>
    
    <div class="section">
        <h3>Test Suite Summary</h3>
        <div class="metric">
            <strong>Total Scenarios:</strong> ${serviceEvidence.testSuite.totalScenarios}
        </div>
        <div class="metric success">
            <strong>Passed:</strong> ${serviceEvidence.testSuite.passedScenarios}
        </div>
        <div class="metric danger">
            <strong>Failed:</strong> ${serviceEvidence.testSuite.failedScenarios}
        </div>
        <div class="metric warning">
            <strong>Skipped:</strong> ${serviceEvidence.testSuite.skippedScenarios}
        </div>
    </div>
    
    <div class="section">
        <h3>API Coverage</h3>
        <div class="metric">
            <strong>Endpoint Coverage:</strong> ${serviceEvidence.coverage.apiEndpoints.coverage}
        </div>
        <div class="metric">
            <strong>Tested Endpoints:</strong> ${serviceEvidence.coverage.apiEndpoints.tested}
        </div>
        <div class="metric">
            <strong>Expected Endpoints:</strong> ${serviceEvidence.coverage.apiEndpoints.expected}
        </div>
        
        <h4>HTTP Methods Coverage</h4>
        <table>
            <tr><th>Method</th><th>Count</th></tr>
            ${Object.keys(serviceEvidence.coverage.httpMethods).map(method => 
                `<tr><td>${method}</td><td>${serviceEvidence.coverage.httpMethods[method]}</td></tr>`
            ).join('')}
        </table>
        
        <h4>Status Codes Coverage</h4>
        <table>
            <tr><th>Status Code</th><th>Count</th></tr>
            ${Object.keys(serviceEvidence.coverage.statusCodes).map(code => 
                `<tr><td>${code}</td><td>${serviceEvidence.coverage.statusCodes[code]}</td></tr>`
            ).join('')}
        </table>
    </div>
    
    <div class="section">
        <h3>Performance Metrics</h3>
        <div class="metric">
            <strong>Total Requests:</strong> ${serviceEvidence.performance.totalRequests}
        </div>
        <div class="metric">
            <strong>Average Response Time:</strong> ${serviceEvidence.performance.averageResponseTime.toFixed(2)}ms
        </div>
        <div class="metric">
            <strong>Max Response Time:</strong> ${serviceEvidence.performance.maxResponseTime}ms
        </div>
        <div class="metric">
            <strong>Min Response Time:</strong> ${serviceEvidence.performance.minResponseTime}ms
        </div>
    </div>
    
    <div class="section">
        <h3>Quality Metrics</h3>
        <div class="metric">
            <strong>Total Validations:</strong> ${serviceEvidence.quality.totalValidations}
        </div>
        <div class="metric success">
            <strong>Passed Validations:</strong> ${serviceEvidence.quality.passedValidations}
        </div>
        <div class="metric danger">
            <strong>Failed Validations:</strong> ${serviceEvidence.quality.failedValidations}
        </div>
    </div>
    
    ${serviceEvidence.recommendations.length > 0 ? `
    <div class="section recommendations">
        <h3>Recommendations</h3>
        ${serviceEvidence.recommendations.map(rec => `
            <div class="recommendation ${rec.priority.toLowerCase()}">
                <strong>[${rec.priority}] ${rec.type}:</strong> ${rec.message}
                <br><em>Suggestion: ${rec.suggestion}</em>
            </div>
        `).join('')}
    </div>
    ` : ''}
    
    ${serviceEvidence.errors.length > 0 ? `
    <div class="section">
        <h3>Errors</h3>
        <table>
            <tr><th>Timestamp</th><th>Message</th><th>Context</th></tr>
            ${serviceEvidence.errors.map(error => `
                <tr>
                    <td>${error.timestamp}</td>
                    <td>${error.message}</td>
                    <td>${error.context || 'N/A'}</td>
                </tr>
            `).join('')}
        </table>
    </div>
    ` : ''}
    
</body>
</html>
            `;
            
            return html;
        },
        
        // Save evidence to file
        saveevidence: function(evidence, filename) {
            var timestamp = new Date().toISOString().replace(/[:.]/g, '-');
            var fullFilename = filename || `evidence-${evidence.service}-${timestamp}.json`;
            
            // In a real implementation, this would save to file system
            // For now, we'll just return the evidence object
            return {
                filename: fullFilename,
                evidence: evidence,
                saved: true
            };
        }
    };
    
    return evidenceGenerator;
}