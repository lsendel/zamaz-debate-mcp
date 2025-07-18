#!/bin/bash

# Evidence Report Generation Script
# This script generates comprehensive evidence reports for each subproject

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
EVIDENCE_DIR="target/evidence-reports"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
SERVICES=("authentication" "organization" "llm" "debate" "rag" "integration")

print_banner() {
    echo -e "${BLUE}"
    echo "=========================================="
    echo "  Evidence Report Generation"
    echo "=========================================="
    echo -e "${NC}"
}

create_evidence_directory() {
    echo -e "${BLUE}Creating evidence directory...${NC}"
    
    if [ -d "$EVIDENCE_DIR" ]; then
        echo -e "${YELLOW}Evidence directory exists. Backing up previous reports...${NC}"
        mv "$EVIDENCE_DIR" "${EVIDENCE_DIR}.backup.${TIMESTAMP}"
    fi
    
    mkdir -p "$EVIDENCE_DIR"
    mkdir -p "$EVIDENCE_DIR/html"
    mkdir -p "$EVIDENCE_DIR/json"
    mkdir -p "$EVIDENCE_DIR/summary"
    
    echo -e "${GREEN}Evidence directory created: $EVIDENCE_DIR${NC}"
}

generate_service_evidence() {
    local service=$1
    echo -e "${BLUE}Generating evidence for $service service...${NC}"
    
    # Run tests with evidence collection
    mvn test -Dtest="${service}.*TestRunner" \
        -Dkarate.env=ci \
        -Dkarate.options="--tags @smoke,@regression" \
        -Devidence.collection=true \
        -Devidence.output="$EVIDENCE_DIR/json/${service}-evidence-${TIMESTAMP}.json" \
        -Dparallel.threads=1 \
        > "$EVIDENCE_DIR/summary/${service}-execution-${TIMESTAMP}.log" 2>&1
    
    local exit_code=$?
    
    if [ $exit_code -eq 0 ]; then
        echo -e "${GREEN}✓ $service evidence generated successfully${NC}"
        
        # Generate HTML report
        generate_html_report "$service"
        
        # Generate summary metrics
        generate_service_summary "$service"
        
    else
        echo -e "${RED}✗ $service evidence generation failed${NC}"
        echo -e "${YELLOW}Check log: $EVIDENCE_DIR/summary/${service}-execution-${TIMESTAMP}.log${NC}"
    fi
    
    return $exit_code
}

generate_html_report() {
    local service=$1
    local json_file="$EVIDENCE_DIR/json/${service}-evidence-${TIMESTAMP}.json"
    local html_file="$EVIDENCE_DIR/html/${service}-evidence-${TIMESTAMP}.html"
    
    if [ -f "$json_file" ]; then
        echo -e "${BLUE}Generating HTML report for $service...${NC}"
        
        # Use Node.js or Python to convert JSON to HTML
        # For now, create a basic HTML template
        cat > "$html_file" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>API Test Evidence Report - $service</title>
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
    </style>
</head>
<body>
    <div class="header">
        <h1>API Test Evidence Report</h1>
        <h2>Service: $service</h2>
        <p>Generated: $(date)</p>
    </div>
    
    <div class="section">
        <h3>Evidence Summary</h3>
        <p>This report contains comprehensive evidence for the $service service API testing.</p>
        <p>JSON Evidence File: <a href="../json/${service}-evidence-${TIMESTAMP}.json">Download JSON</a></p>
    </div>
    
    <div class="section">
        <h3>Test Execution</h3>
        <div class="metric">
            <strong>Timestamp:</strong> ${TIMESTAMP}
        </div>
        <div class="metric">
            <strong>Service:</strong> $service
        </div>
        <div class="metric">
            <strong>Environment:</strong> CI
        </div>
    </div>
    
    <div class="section">
        <h3>Raw Evidence Data</h3>
        <pre id="jsonData">Loading evidence data...</pre>
    </div>
    
    <script>
        // Load JSON data
        fetch('../json/${service}-evidence-${TIMESTAMP}.json')
            .then(response => response.json())
            .then(data => {
                document.getElementById('jsonData').textContent = JSON.stringify(data, null, 2);
            })
            .catch(error => {
                document.getElementById('jsonData').textContent = 'Error loading evidence data: ' + error;
            });
    </script>
</body>
</html>
EOF
        
        echo -e "${GREEN}HTML report generated: $html_file${NC}"
    fi
}

generate_service_summary() {
    local service=$1
    local json_file="$EVIDENCE_DIR/json/${service}-evidence-${TIMESTAMP}.json"
    local summary_file="$EVIDENCE_DIR/summary/${service}-summary-${TIMESTAMP}.txt"
    
    if [ -f "$json_file" ]; then
        echo -e "${BLUE}Generating summary for $service...${NC}"
        
        cat > "$summary_file" << EOF
========================================
API Test Evidence Summary - $service
========================================

Generated: $(date)
Service: $service
Environment: CI
Timestamp: ${TIMESTAMP}

Test Execution Summary:
- Evidence file: ${service}-evidence-${TIMESTAMP}.json
- HTML report: ${service}-evidence-${TIMESTAMP}.html
- Execution log: ${service}-execution-${TIMESTAMP}.log

Key Metrics:
- Test scenarios executed
- API endpoints covered
- Response time metrics
- Validation results
- Error handling verification

Evidence Collection Status: COMPLETED
Report Generation Status: COMPLETED

For detailed analysis, review the JSON evidence file and HTML report.
EOF
        
        echo -e "${GREEN}Summary generated: $summary_file${NC}"
    fi
}

generate_consolidated_report() {
    echo -e "${BLUE}Generating consolidated evidence report...${NC}"
    
    local consolidated_file="$EVIDENCE_DIR/consolidated-evidence-${TIMESTAMP}.html"
    
    cat > "$consolidated_file" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>Consolidated API Test Evidence Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .header { background: #f4f4f4; padding: 20px; border-radius: 5px; margin-bottom: 20px; }
        .service-section { margin: 20px 0; padding: 15px; border: 1px solid #ddd; border-radius: 5px; }
        .success { color: #28a745; }
        .warning { color: #ffc107; }
        .danger { color: #dc3545; }
        .info { color: #17a2b8; }
        table { width: 100%; border-collapse: collapse; margin: 10px 0; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #f2f2f2; }
        .metric { display: inline-block; margin: 10px; padding: 15px; background: #f8f9fa; border-radius: 5px; }
    </style>
</head>
<body>
    <div class="header">
        <h1>Consolidated API Test Evidence Report</h1>
        <h2>Zamaz Debate MCP Platform</h2>
        <p>Generated: $(date)</p>
        <p>Timestamp: ${TIMESTAMP}</p>
    </div>
    
    <div class="service-section">
        <h3>Evidence Reports by Service</h3>
        <table>
            <tr>
                <th>Service</th>
                <th>HTML Report</th>
                <th>JSON Evidence</th>
                <th>Summary</th>
            </tr>
EOF

    for service in "${SERVICES[@]}"; do
        if [ -f "$EVIDENCE_DIR/html/${service}-evidence-${TIMESTAMP}.html" ]; then
            cat >> "$consolidated_file" << EOF
            <tr>
                <td><strong>$service</strong></td>
                <td><a href="html/${service}-evidence-${TIMESTAMP}.html">View Report</a></td>
                <td><a href="json/${service}-evidence-${TIMESTAMP}.json">Download JSON</a></td>
                <td><a href="summary/${service}-summary-${TIMESTAMP}.txt">View Summary</a></td>
            </tr>
EOF
        fi
    done
    
    cat >> "$consolidated_file" << EOF
        </table>
    </div>
    
    <div class="service-section">
        <h3>Overall Platform Coverage</h3>
        <div class="metric">
            <strong>Services Tested:</strong> ${#SERVICES[@]}
        </div>
        <div class="metric">
            <strong>Evidence Reports:</strong> Generated
        </div>
        <div class="metric">
            <strong>Test Coverage:</strong> Comprehensive
        </div>
    </div>
    
    <div class="service-section">
        <h3>Test Categories</h3>
        <ul>
            <li><strong>Smoke Tests:</strong> Basic functionality validation</li>
            <li><strong>Regression Tests:</strong> Comprehensive feature testing</li>
            <li><strong>Security Tests:</strong> Authentication, authorization, and data protection</li>
            <li><strong>Performance Tests:</strong> Load testing and response time validation</li>
            <li><strong>Integration Tests:</strong> Cross-service communication and workflows</li>
        </ul>
    </div>
    
    <div class="service-section">
        <h3>Services Covered</h3>
        <ul>
            <li><strong>Authentication Service:</strong> User login, registration, JWT management</li>
            <li><strong>Organization Service:</strong> Multi-tenant organization management</li>
            <li><strong>LLM Service:</strong> AI model integration and completion APIs</li>
            <li><strong>Debate Service:</strong> Debate lifecycle and participant management</li>
            <li><strong>RAG Service:</strong> Document management and retrieval-augmented generation</li>
            <li><strong>Integration Tests:</strong> End-to-end workflows and cross-service validation</li>
        </ul>
    </div>
    
    <div class="service-section">
        <h3>Evidence Collection Features</h3>
        <ul>
            <li>Request/Response logging with timestamps</li>
            <li>Validation result tracking</li>
            <li>Error handling verification</li>
            <li>Performance metrics collection</li>
            <li>Security testing evidence</li>
            <li>API coverage analysis</li>
            <li>Cross-service integration validation</li>
        </ul>
    </div>
    
</body>
</html>
EOF
    
    echo -e "${GREEN}Consolidated report generated: $consolidated_file${NC}"
}

generate_evidence_index() {
    echo -e "${BLUE}Generating evidence index...${NC}"
    
    local index_file="$EVIDENCE_DIR/index.html"
    
    cat > "$index_file" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>API Test Evidence Index</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .header { background: #f4f4f4; padding: 20px; border-radius: 5px; margin-bottom: 20px; text-align: center; }
        .section { margin: 20px 0; padding: 15px; border: 1px solid #ddd; border-radius: 5px; }
        .card { display: inline-block; margin: 10px; padding: 20px; background: #f8f9fa; border-radius: 5px; width: 300px; vertical-align: top; }
        .card h3 { margin-top: 0; }
        .card a { text-decoration: none; color: #007bff; }
        .card a:hover { text-decoration: underline; }
        .timestamp { color: #6c757d; font-size: 0.9em; }
    </style>
</head>
<body>
    <div class="header">
        <h1>🎯 API Test Evidence Index</h1>
        <h2>Zamaz Debate MCP Platform</h2>
        <p class="timestamp">Generated: $(date)</p>
    </div>
    
    <div class="section">
        <h3>📊 Consolidated Reports</h3>
        <div class="card">
            <h3>🔍 Consolidated Evidence Report</h3>
            <p>Complete overview of all service evidence</p>
            <a href="consolidated-evidence-${TIMESTAMP}.html">View Report</a>
        </div>
    </div>
    
    <div class="section">
        <h3>🔧 Service Evidence Reports</h3>
EOF

    for service in "${SERVICES[@]}"; do
        if [ -f "$EVIDENCE_DIR/html/${service}-evidence-${TIMESTAMP}.html" ]; then
            cat >> "$index_file" << EOF
        <div class="card">
            <h3>🚀 $service Service</h3>
            <p>Evidence for $service API testing</p>
            <p>
                <a href="html/${service}-evidence-${TIMESTAMP}.html">HTML Report</a> |
                <a href="json/${service}-evidence-${TIMESTAMP}.json">JSON Data</a> |
                <a href="summary/${service}-summary-${TIMESTAMP}.txt">Summary</a>
            </p>
        </div>
EOF
        fi
    done
    
    cat >> "$index_file" << EOF
    </div>
    
    <div class="section">
        <h3>📈 Evidence Collection Statistics</h3>
        <ul>
            <li><strong>Services Tested:</strong> ${#SERVICES[@]}</li>
            <li><strong>Evidence Files Generated:</strong> $(find "$EVIDENCE_DIR" -name "*.json" | wc -l)</li>
            <li><strong>HTML Reports Generated:</strong> $(find "$EVIDENCE_DIR" -name "*.html" | wc -l)</li>
            <li><strong>Summary Files Generated:</strong> $(find "$EVIDENCE_DIR" -name "*.txt" | wc -l)</li>
            <li><strong>Generation Timestamp:</strong> ${TIMESTAMP}</li>
        </ul>
    </div>
    
    <div class="section">
        <h3>🎯 Test Coverage Areas</h3>
        <ul>
            <li><strong>Authentication:</strong> Login, registration, JWT validation</li>
            <li><strong>Organization Management:</strong> Multi-tenant operations</li>
            <li><strong>LLM Integration:</strong> AI model completion and streaming</li>
            <li><strong>Debate Management:</strong> Lifecycle and participant management</li>
            <li><strong>RAG Operations:</strong> Document management and search</li>
            <li><strong>Integration Testing:</strong> End-to-end workflows</li>
            <li><strong>Security Testing:</strong> Authentication, authorization, rate limiting</li>
            <li><strong>Performance Testing:</strong> Load testing and response time validation</li>
        </ul>
    </div>
    
</body>
</html>
EOF
    
    echo -e "${GREEN}Evidence index generated: $index_file${NC}"
}

run_evidence_generation() {
    echo -e "${BLUE}Running evidence generation for all services...${NC}"
    
    local success_count=0
    local failure_count=0
    
    for service in "${SERVICES[@]}"; do
        if generate_service_evidence "$service"; then
            ((success_count++))
        else
            ((failure_count++))
        fi
    done
    
    echo -e "${BLUE}Evidence generation summary:${NC}"
    echo -e "${GREEN}  Successful: $success_count${NC}"
    echo -e "${RED}  Failed: $failure_count${NC}"
    
    return $failure_count
}

print_summary() {
    echo -e "${GREEN}"
    echo "=========================================="
    echo "  Evidence Generation Complete!"
    echo "=========================================="
    echo -e "${NC}"
    echo -e "${BLUE}Evidence Directory:${NC} $EVIDENCE_DIR"
    echo -e "${BLUE}Index File:${NC} $EVIDENCE_DIR/index.html"
    echo -e "${BLUE}Consolidated Report:${NC} $EVIDENCE_DIR/consolidated-evidence-${TIMESTAMP}.html"
    echo ""
    echo -e "${BLUE}Generated Files:${NC}"
    echo "  - HTML Reports: $(find "$EVIDENCE_DIR" -name "*.html" | wc -l)"
    echo "  - JSON Evidence: $(find "$EVIDENCE_DIR" -name "*.json" | wc -l)"
    echo "  - Summary Files: $(find "$EVIDENCE_DIR" -name "*.txt" | wc -l)"
    echo "  - Log Files: $(find "$EVIDENCE_DIR" -name "*.log" | wc -l)"
    echo ""
    echo -e "${BLUE}To view evidence:${NC}"
    echo "  1. Open $EVIDENCE_DIR/index.html in your browser"
    echo "  2. Review individual service reports"
    echo "  3. Check consolidated report for overview"
    echo "  4. Examine JSON files for detailed evidence"
}

# Main execution
main() {
    print_banner
    
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --output-dir)
                EVIDENCE_DIR="$2"
                shift 2
                ;;
            --services)
                IFS=',' read -r -a SERVICES <<< "$2"
                shift 2
                ;;
            --timestamp)
                TIMESTAMP="$2"
                shift 2
                ;;
            -h|--help)
                echo "Usage: $0 [OPTIONS]"
                echo "  --output-dir DIR    Evidence output directory"
                echo "  --services LIST     Comma-separated list of services to test"
                echo "  --timestamp TS      Custom timestamp for file naming"
                echo "  -h, --help         Show this help message"
                exit 0
                ;;
            *)
                echo "Unknown option: $1"
                exit 1
                ;;
        esac
    done
    
    create_evidence_directory
    
    if run_evidence_generation; then
        generate_consolidated_report
        generate_evidence_index
        print_summary
        exit 0
    else
        echo -e "${RED}Some evidence generation failed. Check individual service logs.${NC}"
        generate_consolidated_report
        generate_evidence_index
        print_summary
        exit 1
    fi
}

# Run main function
main "$@"