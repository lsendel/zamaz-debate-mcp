#!/usr/bin/env python3
"""SQL Injection Security Tests"""

import argparse
import json
import requests
import time
from typing import List, Dict, Any

class SQLInjectionTester:
    def __init__(self, target_url: str):
        self.target_url = target_url.rstrip('/')
        self.results = []
        self.payloads = self.load_payloads()
    
    def load_payloads(self) -> List[str]:
        """Load SQL injection payloads"""
        return [
            # Basic injections
            "' OR '1'='1",
            "' OR '1'='1' --",
            "' OR '1'='1' /*",
            "admin'--",
            "admin' /*",
            "' or 1=1--",
            "' or 1=1#",
            "' or 1=1/*",
            
            # Union-based injections
            "' UNION SELECT NULL--",
            "' UNION SELECT NULL,NULL--",
            "' UNION SELECT NULL,NULL,NULL--",
            "' UNION ALL SELECT NULL--",
            
            # Time-based blind injections
            "'; WAITFOR DELAY '00:00:05'--",
            "'; SELECT SLEEP(5)--",
            "' AND SLEEP(5)--",
            "' OR SLEEP(5)--",
            
            # Boolean-based blind injections
            "' AND '1'='1",
            "' AND '1'='2",
            "' AND ASCII(SUBSTRING((SELECT database()),1,1))>64",
            
            # Error-based injections
            "' AND (SELECT * FROM (SELECT(SLEEP(5)))a)--",
            "' AND extractvalue(1,concat(0x7e,(SELECT database())))--",
            "' AND updatexml(1,concat(0x7e,(SELECT database())),1)--",
            
            # Advanced payloads
            "'; DROP TABLE users--",
            "'; INSERT INTO users VALUES ('hacker', 'password')--",
            "' UNION SELECT username, password FROM users--",
            
            # Encoded payloads
            "%27%20OR%20%271%27%3D%271",
            "&#39; OR &#39;1&#39;=&#39;1",
        ]
    
    def test_endpoint(self, endpoint: str, method: str = 'GET', 
                     params: Dict[str, str] = None, 
                     data: Dict[str, str] = None) -> List[Dict[str, Any]]:
        """Test an endpoint for SQL injection vulnerabilities"""
        vulnerabilities = []
        
        for payload in self.payloads:
            # Test each parameter
            if params:
                for param_name, param_value in params.items():
                    test_params = params.copy()
                    test_params[param_name] = payload
                    
                    result = self.send_request(
                        endpoint, method, params=test_params, 
                        payload=payload, param_name=param_name
                    )
                    
                    if result['vulnerable']:
                        vulnerabilities.append(result)
            
            if data:
                for field_name, field_value in data.items():
                    test_data = data.copy()
                    test_data[field_name] = payload
                    
                    result = self.send_request(
                        endpoint, method, data=test_data,
                        payload=payload, param_name=field_name
                    )
                    
                    if result['vulnerable']:
                        vulnerabilities.append(result)
        
        return vulnerabilities
    
    def send_request(self, endpoint: str, method: str, 
                    params: Dict = None, data: Dict = None,
                    payload: str = '', param_name: str = '') -> Dict[str, Any]:
        """Send request and analyze response for SQL injection"""
        url = f"{self.target_url}{endpoint}"
        
        try:
            start_time = time.time()
            
            if method == 'GET':
                response = requests.get(url, params=params, timeout=10)
            elif method == 'POST':
                response = requests.post(url, json=data, timeout=10)
            else:
                response = requests.request(method, url, params=params, json=data, timeout=10)
            
            elapsed_time = time.time() - start_time
            
            # Analyze response for SQL injection indicators
            vulnerable = self.analyze_response(response, elapsed_time, payload)
            
            return {
                'vulnerable': vulnerable,
                'endpoint': endpoint,
                'method': method,
                'parameter': param_name,
                'payload': payload,
                'status_code': response.status_code,
                'response_time': elapsed_time,
                'indicators': self.get_indicators(response, elapsed_time)
            }
            
        except requests.exceptions.Timeout:
            # Timeout might indicate time-based SQL injection
            return {
                'vulnerable': True,
                'endpoint': endpoint,
                'method': method,
                'parameter': param_name,
                'payload': payload,
                'status_code': 0,
                'response_time': 10.0,
                'indicators': ['timeout', 'possible_time_based_sqli']
            }
        except Exception as e:
            return {
                'vulnerable': False,
                'endpoint': endpoint,
                'method': method,
                'parameter': param_name,
                'payload': payload,
                'error': str(e)
            }
    
    def analyze_response(self, response: requests.Response, 
                        elapsed_time: float, payload: str) -> bool:
        """Analyze response for SQL injection indicators"""
        indicators = []
        
        # Check for SQL error messages
        sql_errors = [
            'SQL syntax',
            'mysql_fetch',
            'ORA-01756',
            'PostgreSQL',
            'SQLServer',
            'Microsoft OLE DB Provider',
            'Unclosed quotation mark',
            'ODBC Microsoft Access Driver',
            'Microsoft JET Database Engine',
            'Error Executing Database Query',
            'SQLite error',
            'mysql error',
            'supplied argument is not a valid MySQL',
            'Warning: mysql_'
        ]
        
        response_text = response.text.lower()
        for error in sql_errors:
            if error.lower() in response_text:
                indicators.append(f'sql_error:{error}')
        
        # Check for time-based indicators
        if 'SLEEP' in payload or 'WAITFOR' in payload:
            if elapsed_time > 4.5:  # Expected 5 second delay
                indicators.append('time_based_delay')
        
        # Check for boolean-based indicators
        if payload.endswith("'1'='1") and response.status_code == 200:
            indicators.append('boolean_true_condition')
        elif payload.endswith("'1'='2") and response.status_code in [400, 403, 404]:
            indicators.append('boolean_false_condition')
        
        # Check for information disclosure
        if any(keyword in response_text for keyword in ['password', 'passwd', 'pwd']):
            indicators.append('potential_data_leak')
        
        return len(indicators) > 0
    
    def get_indicators(self, response: requests.Response, 
                      elapsed_time: float) -> List[str]:
        """Get specific indicators from response"""
        indicators = []
        
        # Status code analysis
        if response.status_code == 500:
            indicators.append('internal_server_error')
        elif response.status_code == 200:
            indicators.append('successful_injection')
        
        # Response time analysis
        if elapsed_time > 5:
            indicators.append('slow_response')
        
        # Content analysis
        if 'syntax error' in response.text.lower():
            indicators.append('syntax_error')
        if 'database' in response.text.lower():
            indicators.append('database_disclosure')
        
        return indicators
    
    def run_tests(self, endpoints: List[Dict[str, Any]]) -> Dict[str, Any]:
        """Run SQL injection tests on multiple endpoints"""
        print(f"🔍 Running SQL injection tests against {self.target_url}")
        
        total_vulnerabilities = []
        
        for endpoint_config in endpoints:
            endpoint = endpoint_config.get('path', '/')
            method = endpoint_config.get('method', 'GET')
            params = endpoint_config.get('params', {})
            data = endpoint_config.get('data', {})
            
            print(f"\nTesting {method} {endpoint}...")
            vulnerabilities = self.test_endpoint(endpoint, method, params, data)
            
            if vulnerabilities:
                print(f"  ⚠️ Found {len(vulnerabilities)} SQL injection vulnerabilities!")
                total_vulnerabilities.extend(vulnerabilities)
            else:
                print(f"  ✅ No SQL injection vulnerabilities found")
        
        return {
            'target': self.target_url,
            'total_endpoints_tested': len(endpoints),
            'vulnerable_endpoints': len(set(v['endpoint'] for v in total_vulnerabilities)),
            'total_vulnerabilities': len(total_vulnerabilities),
            'vulnerabilities': total_vulnerabilities,
            'severity': 'CRITICAL' if total_vulnerabilities else 'PASS',
            'recommendations': self.get_recommendations(total_vulnerabilities)
        }
    
    def get_recommendations(self, vulnerabilities: List[Dict[str, Any]]) -> List[str]:
        """Get security recommendations based on findings"""
        recommendations = []
        
        if not vulnerabilities:
            recommendations.append("Continue using parameterized queries/prepared statements")
            recommendations.append("Maintain input validation and sanitization")
        else:
            recommendations.append("URGENT: Use parameterized queries/prepared statements for ALL database queries")
            recommendations.append("Implement strict input validation and whitelisting")
            recommendations.append("Use stored procedures where appropriate")
            recommendations.append("Apply principle of least privilege to database users")
            recommendations.append("Enable SQL query logging and monitoring")
            recommendations.append("Consider using an ORM with built-in SQL injection protection")
            recommendations.append("Implement Web Application Firewall (WAF) rules")
        
        return recommendations

def main():
    parser = argparse.ArgumentParser(description='SQL Injection Security Tests')
    parser.add_argument('--target', required=True, help='Target URL')
    parser.add_argument('--endpoints', help='JSON file with endpoints to test')
    parser.add_argument('--report', required=True, help='Output report file')
    
    args = parser.parse_args()
    
    # Default endpoints if none provided
    if args.endpoints and os.path.exists(args.endpoints):
        with open(args.endpoints, 'r') as f:
            endpoints = json.load(f)
    else:
        endpoints = [
            {
                'path': '/api/auth/login',
                'method': 'POST',
                'data': {'username': 'test', 'password': 'test'}
            },
            {
                'path': '/api/users',
                'method': 'GET',
                'params': {'search': 'admin', 'role': 'user'}
            },
            {
                'path': '/api/organizations',
                'method': 'GET',
                'params': {'name': 'test', 'type': 'business'}
            },
            {
                'path': '/api/debates',
                'method': 'GET',
                'params': {'topic': 'test', 'status': 'active'}
            }
        ]
    
    tester = SQLInjectionTester(args.target)
    results = tester.run_tests(endpoints)
    
    # Save report
    with open(args.report, 'w') as f:
        json.dump(results, f, indent=2)
    
    print(f"\n📄 Report saved to {args.report}")
    print(f"🎯 Severity: {results['severity']}")
    
    # Exit with error if vulnerabilities found
    if results['total_vulnerabilities'] > 0:
        exit(1)

if __name__ == '__main__':
    main()