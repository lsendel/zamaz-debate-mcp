# 📊 How to View Test Evidence Reports

## Quick Start - Method 2: HTTP Server

This is the recommended method to view your test evidence reports with proper formatting and navigation.

### Step 1: Navigate to the Karate API Tests Directory

```bash
cd /Users/lsendel/IdeaProjects/zamaz-debate-mcp/karate-api-tests
```

### Step 2: Generate Evidence Reports (if not already generated)

```bash
./scripts/generate-evidence-reports.sh
```

This will create comprehensive evidence reports for all services in the `test-evidence-reports/` directory.

### Step 3: Start the HTTP Server

```bash
cd test-evidence-reports
python3 -m http.server 8080
```

### Step 4: Open Your Browser

Navigate to: **http://localhost:8080**

## 🎯 What You'll See

1. **Main Dashboard** (`index.html`)
   - Overview of all service evidence reports
   - Quick navigation to individual service reports
   - Test execution statistics

2. **Service-Specific Reports** (in `html/` directory)
   - Authentication Service evidence
   - Organization Service evidence
   - LLM Service evidence
   - Debate Controller evidence
   - RAG Service evidence
   - Integration Testing evidence

3. **Consolidated Report**
   - Combined overview of all services
   - Cross-service metrics
   - Platform-wide test coverage

## 🚀 One-Command Solution

We've also created a convenient script that handles everything:

```bash
# This will check for reports, generate if needed, and start the server
./scripts/view-evidence-reports.sh

# Generate fresh reports and view them
./scripts/view-evidence-reports.sh --generate

# Use a different port
./scripts/view-evidence-reports.sh --port 8888
```

## 📋 Report Features

Each evidence report includes:

- **API Coverage Matrix**: Visual representation of tested endpoints
- **Performance Metrics**: Response time charts and throughput analysis
- **Security Validation**: Authentication and authorization test results
- **Error Scenarios**: Comprehensive error handling verification
- **Test Execution Details**: Step-by-step test scenario evidence
- **Recommendations**: Actionable insights for improvement

## 🔍 Navigating the Reports

1. Start at the **index.html** main dashboard
2. Click on any service to view its detailed evidence report
3. Use the consolidated report for an overview of all services
4. Raw JSON data is available in the `json/` directory for programmatic access
5. Text summaries in the `summary/` directory provide quick insights

## 💡 Tips

- Keep the HTTP server running while reviewing reports
- Use browser bookmarks for frequently accessed reports
- The reports are static HTML - you can also share them by copying the entire `test-evidence-reports` directory
- For CI/CD integration, use the JSON files in the `json/` directory

## 🛑 Stopping the Server

Press `Ctrl+C` in the terminal to stop the HTTP server.

---

Happy testing! 🎉