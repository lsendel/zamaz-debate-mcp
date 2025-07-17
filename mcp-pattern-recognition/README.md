# MCP Pattern Recognition System

A comprehensive codebase pattern recognition system that provides intelligent analysis of software patterns, code smells, anti-patterns, and team-specific coding practices using machine learning.

## Features

### 🔍 Pattern Detection
- **Design Patterns**: GoF patterns (Singleton, Factory, Observer, etc.)
- **Enterprise Patterns**: MVC, Repository, Service Layer, etc.
- **Architectural Patterns**: Microservices, Hexagonal Architecture, Clean Architecture
- **Code Smells**: Long methods, God classes, feature envy, etc.
- **Anti-Patterns**: Spaghetti code, golden hammer, magic numbers

### 🧠 Machine Learning Capabilities
- **Team Pattern Learning**: Learns from team coding patterns and preferences
- **Adaptive Detection**: Adjusts detection thresholds based on team feedback
- **Pattern Prediction**: Suggests patterns based on code context
- **Continuous Learning**: Improves accuracy over time

### 🚀 Performance Optimization
- **Parallel Processing**: Multi-threaded analysis for large codebases
- **Intelligent Caching**: Reduces redundant analysis
- **Selective Detection**: Focus on specific patterns or file types
- **Batch Processing**: Efficient handling of large repositories

### 📊 Reporting and Visualization
- **Comprehensive Reports**: Executive summaries, technical details, trends
- **Interactive Dashboards**: Real-time metrics and visualizations
- **Export Formats**: PDF, HTML, JSON, CSV, Excel
- **Custom Charts**: Pattern distribution, quality metrics, trends

### 🔧 Integration
- **GitHub Integration**: Analyze repositories and pull requests
- **MCP Compatibility**: Works with existing MCP infrastructure
- **Webhook Support**: Continuous analysis on code changes
- **REST API**: Programmatic access to all features

## Quick Start

### Prerequisites
- Java 21 or higher
- Maven 3.6+
- PostgreSQL 12+
- Redis 6+

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/zamaz/mcp-pattern-recognition.git
   cd mcp-pattern-recognition
   ```

2. **Set up database**
   ```sql
   CREATE DATABASE mcp_pattern_recognition;
   CREATE USER mcp_user WITH ENCRYPTED PASSWORD 'mcp_password';
   GRANT ALL PRIVILEGES ON DATABASE mcp_pattern_recognition TO mcp_user;
   ```

3. **Configure environment variables**
   ```bash
   export DB_USERNAME=mcp_user
   export DB_PASSWORD=mcp_password
   export REDIS_HOST=localhost
   export REDIS_PORT=6379
   ```

4. **Build and run**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

5. **Access the application**
   - API: `http://localhost:8080/api/pattern-recognition`
   - Health: `http://localhost:8080/api/pattern-recognition/actuator/health`
   - Metrics: `http://localhost:8080/api/pattern-recognition/actuator/prometheus`

## Configuration

### Basic Configuration
```yaml
pattern-recognition:
  detection:
    max-threads: 10
    confidence-threshold: 0.7
    enable-caching: true
    supported-extensions:
      - .java
      - .kt
      - .scala
  
  ml:
    enable-learning: true
    training-batch-size: 100
    learning-rate: 0.001
  
  performance:
    enable-optimizations: true
    default-strategy: PARALLEL_PROCESSING
```

### Advanced Configuration
```yaml
pattern-recognition:
  detection:
    timeout-per-file: 30
    max-file-size: 1048576
    max-lines-of-code: 10000
    excluded-patterns:
      - ".*\\.generated\\..*"
      - ".*/target/.*"
  
  ml:
    feature-extraction:
      enable-ast-features: true
      enable-text-features: true
      max-feature-vector-size: 1000
  
  reporting:
    export-formats: [PDF, HTML, JSON]
    retention-days: 90
```

## Usage

### REST API

#### Analyze a Repository
```bash
curl -X POST "http://localhost:8080/api/pattern-recognition/analyze/repository" \
  -H "Content-Type: application/json" \
  -d '{
    "organizationId": "org-123",
    "repositoryUrl": "https://github.com/user/repo",
    "analysisOptions": {
      "optimizationStrategy": {
        "type": "PARALLEL_PROCESSING",
        "threadPoolSize": 8
      }
    }
  }'
```

#### Get Pattern Detection Results
```bash
curl "http://localhost:8080/api/pattern-recognition/results/{analysisId}"
```

#### Generate Report
```bash
curl -X POST "http://localhost:8080/api/pattern-recognition/reports/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "organizationId": "org-123",
    "analysisId": "analysis-456",
    "reportType": "COMPREHENSIVE"
  }'
```

### MCP Integration

#### Analyze Repository via MCP
```json
{
  "method": "pattern_analysis/analyze_repository",
  "params": {
    "organizationId": "org-123",
    "repositoryUrl": "https://github.com/user/repo",
    "analysisOptions": {
      "optimizationStrategy": {
        "type": "SELECTIVE_DETECTION",
        "selectedPatternTypes": ["SINGLETON", "FACTORY", "GOD_CLASS"]
      }
    }
  }
}
```

#### Get Team Patterns
```json
{
  "method": "pattern_analysis/get_team_patterns",
  "params": {
    "organizationId": "org-123"
  }
}
```

### GitHub Integration

#### Webhook Configuration
1. Configure webhook in GitHub repository settings
2. Set webhook URL: `https://your-domain.com/api/pattern-recognition/webhook`
3. Select events: `pull_request`, `push`, `release`
4. Set secret token for security

#### Pull Request Analysis
The system automatically analyzes pull requests and provides:
- Pattern changes introduced
- Code quality impact assessment
- Approval recommendations
- Inline comments on issues

## Pattern Types

### Design Patterns
- **Creational**: Singleton, Factory, Builder, Prototype
- **Structural**: Adapter, Decorator, Facade, Proxy
- **Behavioral**: Observer, Strategy, Command, State

### Code Smells
- **Long Method**: Methods with too many lines
- **Large Class**: Classes with too many responsibilities
- **Feature Envy**: Methods using more of another class
- **Data Clumps**: Repeated groups of parameters

### Anti-Patterns
- **God Class**: Classes that know too much or do too much
- **Spaghetti Code**: Unstructured control flow
- **Magic Numbers**: Unexplained numeric constants
- **Copy-Paste Programming**: Duplicated code blocks

## Machine Learning Models

### Pattern Classification
- **Algorithm**: Ensemble of Random Forest and SVM
- **Features**: AST structure, metrics, text patterns
- **Training**: Incremental learning from team feedback
- **Accuracy**: Adaptive based on team preferences

### Team Pattern Learning
- **Clustering**: Identifies team-specific patterns
- **Anomaly Detection**: Finds unusual code patterns
- **Recommendation Engine**: Suggests improvements
- **Feedback Loop**: Learns from developer feedback

## Performance Optimization

### Strategies
1. **Parallel Processing**: Multi-threaded analysis
2. **Selective Detection**: Focus on specific patterns
3. **Cached Detection**: Reuse previous results
4. **Batched Processing**: Process files in batches
5. **Adaptive Optimization**: Choose strategy based on codebase

### Benchmarks
- **Small Codebase** (< 100 files): ~10 seconds
- **Medium Codebase** (100-1000 files): ~2-5 minutes
- **Large Codebase** (1000+ files): ~10-30 minutes

## Reporting

### Report Types
1. **Executive Summary**: High-level overview for management
2. **Technical Report**: Detailed analysis for developers
3. **Trend Analysis**: Quality changes over time
4. **Team Insights**: Team-specific patterns and recommendations

### Export Formats
- **PDF**: Professional reports for sharing
- **HTML**: Interactive reports with charts
- **JSON**: Machine-readable format for integration
- **CSV**: Data for spreadsheet analysis
- **Excel**: Comprehensive data with charts

## Monitoring and Metrics

### Application Metrics
- Pattern detection performance
- Machine learning model accuracy
- Cache hit ratios
- API response times

### Business Metrics
- Code quality trends
- Pattern adoption rates
- Team productivity indicators
- Technical debt metrics

### Health Checks
- Database connectivity
- Redis availability
- Model performance
- GitHub API status

## Security

### Authentication
- JWT token-based authentication
- OAuth2 integration with GitHub
- Role-based access control
- API key authentication

### Authorization
- Organization-level access control
- Repository-level permissions
- Feature-based authorization
- Team-specific data isolation

### Data Protection
- Encrypted data at rest
- Secure API communication
- Audit logging
- GDPR compliance

## Troubleshooting

### Common Issues

#### High Memory Usage
```bash
# Increase JVM heap size
export JAVA_OPTS="-Xmx4g -Xms2g"

# Enable garbage collection logging
export JAVA_OPTS="$JAVA_OPTS -XX:+PrintGC -XX:+PrintGCDetails"
```

#### Slow Analysis
```yaml
# Enable performance optimizations
pattern-recognition:
  performance:
    enable-optimizations: true
    default-strategy: PARALLEL_PROCESSING
    thread-pool:
      max-size: 20
```

#### Database Connection Issues
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      connection-timeout: 30000
      validation-timeout: 5000
```

### Logs
- Application logs: `logs/pattern-recognition.log`
- Performance logs: `logs/performance.log`
- Error logs: `logs/error.log`

## Contributing

### Development Setup
1. Fork the repository
2. Create feature branch
3. Follow coding standards
4. Add tests for new features
5. Submit pull request

### Code Style
- Follow Google Java Style Guide
- Use meaningful variable names
- Add Javadoc for public methods
- Write unit tests for all new code

### Testing
```bash
# Run unit tests
mvn test

# Run integration tests
mvn verify -P integration-tests

# Run performance tests
mvn verify -P performance-tests
```

## Architecture

### System Architecture
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   GitHub API    │    │   MCP Client    │    │   REST Client   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                    ┌─────────────────┐
                    │  API Gateway    │
                    └─────────────────┘
                                 │
                    ┌─────────────────┐
                    │  Pattern        │
                    │  Recognition    │
                    │  Service        │
                    └─────────────────┘
                                 │
         ┌───────────────────────┼───────────────────────┐
         │                       │                       │
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   PostgreSQL    │    │     Redis       │    │   File System   │
│   Database      │    │     Cache       │    │   (Reports)     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### Component Overview
- **Pattern Detectors**: Identify specific patterns in code
- **ML Engine**: Learn team patterns and improve detection
- **Performance Optimizer**: Optimize analysis for large codebases
- **Report Generator**: Create comprehensive reports
- **Integration Layer**: GitHub and MCP integration

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

- **Documentation**: [Wiki](https://github.com/zamaz/mcp-pattern-recognition/wiki)
- **Issues**: [GitHub Issues](https://github.com/zamaz/mcp-pattern-recognition/issues)
- **Discussions**: [GitHub Discussions](https://github.com/zamaz/mcp-pattern-recognition/discussions)
- **Email**: support@zamaz.com

## Changelog

### v1.0.0 (2024-01-15)
- Initial release
- Core pattern detection capabilities
- Machine learning integration
- GitHub integration
- Comprehensive reporting system
- Performance optimization features

### v1.1.0 (2024-02-01) - Planned
- Additional language support (Python, JavaScript)
- Enhanced ML models
- Real-time analysis capabilities
- Advanced visualization features
- Mobile-responsive dashboard