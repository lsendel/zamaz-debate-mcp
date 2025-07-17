# Comprehensive Documentation Analysis System

A powerful, extensible system for analyzing documentation quality, completeness, and maintenance needs across software projects. This system provides comprehensive insights into documentation health and generates actionable recommendations for improvement.

## Features

### 🔍 **Multi-Format Documentation Parsing**
- **Markdown** (.md) - Full CommonMark support with extensions
- **AsciiDoc** (.adoc) - Complete AsciiDoc parsing
- **reStructuredText** (.rst) - Sphinx-compatible parsing
- **HTML** (.html) - BeautifulSoup-based parsing
- **Extensible** - Easy to add new format parsers

### 💻 **Code Documentation Extraction**
- **Java** - Javadoc, Spring annotations, Swagger annotations
- **Python** - Docstrings, type hints, Flask/FastAPI routes
- **JavaScript** - JSDoc, Express.js routes
- **TypeScript** - TSDoc, interfaces, types
- **Extensible** - Support for additional languages

### 📊 **Quality Analysis & Scoring**
- **Readability Score** - Sentence complexity, technical term density
- **Completeness Score** - Structure, examples, code blocks, images
- **Freshness Score** - Last modification time analysis
- **Overall Quality** - Weighted composite score

### 📈 **Coverage Analysis**
- **API Documentation Coverage** - REST endpoints, GraphQL schemas
- **Code Comment Density** - Inline comments, function documentation
- **Class/Function Documentation** - Comprehensive coverage metrics
- **Missing Documentation Detection** - Identify gaps automatically

### 🔎 **Advanced Search & Indexing**
- **TF-IDF Search** - Fast keyword-based search
- **Semantic Search** - AI-powered similarity matching (optional)
- **Cross-Reference Discovery** - Automatic link detection
- **Related Document Suggestions** - Find similar content

### ⚠️ **Outdated Documentation Detection**
- **Time-based Analysis** - Flag old documentation
- **Git History Integration** - Compare doc vs code changes
- **Broken Link Detection** - Internal and external links
- **API Schema Validation** - Detect outdated API documentation

### 🤖 **Intelligent Recommendations**
- **Priority-based Suggestions** - Critical, high, medium, low
- **Template Generation** - API docs, class docs, README templates
- **Tool Integration** - Javadoc, Sphinx, GitBook recommendations
- **Best Practice Guidance** - Industry standard recommendations

### 🔗 **GitHub Integration**
- **Issue Analysis** - Find documentation-related issues
- **Pull Request Correlation** - Documentation PR analysis
- **Community Metrics** - Contributor engagement analysis
- **Activity Correlation** - Development vs documentation activity

### 📋 **Comprehensive Reporting**
- **Multiple Output Formats** - JSON, Markdown, HTML, CSV
- **Executive Summaries** - High-level project health
- **Detailed Metrics** - Comprehensive statistics
- **Actionable Insights** - Specific improvement recommendations

## Installation

### Prerequisites
- Python 3.8 or higher
- Git (for repository analysis)
- Optional: Java (for Javadoc integration)

### Basic Installation
```bash
# Clone the repository
git clone <repository-url>
cd zamaz-debate-mcp

# Install basic dependencies
pip install -r .github/scripts/requirements_documentation.txt

# For enhanced functionality (optional)
pip install sentence-transformers spacy javalang
python -m spacy download en_core_web_sm
```

### Development Installation
```bash
# Install with development dependencies
pip install -r .github/scripts/requirements_documentation.txt
pip install pytest pytest-cov black flake8 mypy

# Pre-commit hooks (optional)
pre-commit install
```

## Quick Start

### Basic Analysis
```bash
# Run documentation analysis on current project
python .github/scripts/run_documentation_analysis.py .

# Analyze specific directory
python .github/scripts/run_documentation_analysis.py /path/to/project

# Custom output directory
python .github/scripts/run_documentation_analysis.py . -o ./my_analysis
```

### Advanced Usage
```bash
# Generate only HTML report
python .github/scripts/run_documentation_analysis.py . --format html

# Search documentation
python .github/scripts/run_documentation_analysis.py . --search "API authentication"

# Quick analysis (skip time-consuming tasks)
python .github/scripts/run_documentation_analysis.py . --quick

# Analyze only documentation files
python .github/scripts/run_documentation_analysis.py . --docs-only

# Verbose output
python .github/scripts/run_documentation_analysis.py . -v
```

### Comprehensive Analysis with GitHub Integration
```bash
# Run with GitHub token for enhanced analysis
export GITHUB_TOKEN=your_github_token
python .github/scripts/main_with_documentation.py . --github-token $GITHUB_TOKEN
```

## Configuration

The system uses a YAML configuration file for customization:

```yaml
# .github/scripts/config/documentation_analysis_config.yaml

analysis:
  enable_documentation_parsing: true
  enable_code_extraction: true
  enable_quality_analysis: true
  enable_coverage_analysis: true
  enable_search_indexing: true
  enable_outdated_detection: true
  enable_github_integration: true

quality_analysis:
  scoring_weights:
    readability: 0.25
    completeness: 0.40
    freshness: 0.35

coverage_analysis:
  minimum_coverage_threshold: 50
  target_coverage_threshold: 80
```

## Python API Usage

### Basic Analysis
```python
from analyzers.documentation_analyzer import DocumentationAnalysisSystem

# Initialize the system
system = DocumentationAnalysisSystem()

# Analyze a project
results = system.analyze_project('/path/to/project')

# Access metrics
metrics = results['metrics']
print(f"Quality Score: {metrics['quality_score']:.1f}")
print(f"Coverage: {metrics['coverage_percentage']:.1f}%")

# Search documentation
search_results = system.search_documentation("API endpoints")
for doc, score in search_results:
    print(f"{doc.title}: {score:.3f}")
```

### Advanced Integration
```python
from main_with_documentation import ComprehensiveAnalysisSystem

# Initialize with GitHub integration
system = ComprehensiveAnalysisSystem(
    project_path='/path/to/project',
    github_token='your_token'
)

# Run comprehensive analysis
results = system.run_comprehensive_analysis()

# Access combined metrics
combined_metrics = results['combined_metrics']
print(f"Overall Health: {combined_metrics['overall_health_score']:.1f}")
print(f"Documentation Health: {combined_metrics['documentation_health']:.1f}")
```

## Output Examples

### JSON Output
```json
{
  "timestamp": "2024-01-15T10:30:00",
  "project_path": "/path/to/project",
  "metrics": {
    "total_files": 42,
    "total_words": 15420,
    "quality_score": 78.5,
    "coverage_percentage": 67.2,
    "freshness_score": 82.1
  },
  "issues": [
    {
      "severity": "warning",
      "type": "outdated",
      "message": "Documentation may be outdated",
      "file_path": "docs/api.md",
      "suggestion": "Review and update API documentation"
    }
  ],
  "recommendations": {
    "high_priority": [
      {
        "title": "Improve API Documentation",
        "description": "Found 15 undocumented API endpoints",
        "action": "Add documentation for REST endpoints"
      }
    ]
  }
}
```

### Markdown Report
```markdown
# Documentation Analysis Report

**Generated:** 2024-01-15T10:30:00
**Project:** /path/to/project

## Summary
- **Total Documentation Files:** 42
- **Quality Score:** 78.5/100
- **Coverage:** 67.2%
- **Issues Found:** 8

## High Priority Recommendations
- **Improve API Documentation**
  - Found 15 undocumented API endpoints
  - Action: Add documentation for REST endpoints

## Issues Found
- **Warning** in `docs/api.md`: Documentation may be outdated
  - Suggestion: Review and update API documentation
```

### HTML Report
The system generates rich HTML reports with:
- Interactive charts and graphs
- Color-coded metrics
- Expandable sections
- Responsive design

## Integration with Existing Tools

### Javadoc Integration
```bash
# Generate Javadoc and analyze
mvn javadoc:javadoc
python run_documentation_analysis.py . --include-javadoc
```

### Sphinx Integration
```bash
# Generate Sphinx docs and analyze
sphinx-build -b html docs docs/_build
python run_documentation_analysis.py . --include-sphinx
```

### CI/CD Integration
```yaml
# GitHub Actions example
name: Documentation Analysis
on: [push, pull_request]
jobs:
  analyze:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v2
    - name: Setup Python
      uses: actions/setup-python@v2
      with:
        python-version: '3.9'
    - name: Install dependencies
      run: pip install -r .github/scripts/requirements_documentation.txt
    - name: Run analysis
      run: |
        python .github/scripts/run_documentation_analysis.py . \
          --format json \
          --output-dir ./analysis_results
    - name: Upload results
      uses: actions/upload-artifact@v2
      with:
        name: documentation-analysis
        path: analysis_results/
```

## Architecture

### Core Components

1. **DocumentationParser** - Abstract base for format parsers
2. **CodeDocumentationExtractor** - Extracts docs from code
3. **DocumentationQualityAnalyzer** - Quality scoring engine
4. **DocumentationCoverageAnalyzer** - Coverage analysis
5. **DocumentationSearchIndex** - Search and indexing
6. **OutdatedDocumentationDetector** - Freshness analysis
7. **DocumentationRecommendationEngine** - Suggestion system

### Data Models

- **DocumentationFile** - Parsed documentation with metadata
- **CodeFile** - Code analysis results
- **DocumentationIssue** - Problems and suggestions
- **DocumentationMetrics** - Comprehensive metrics

### Extensibility

The system is designed for easy extension:

```python
# Add a new format parser
class MyFormatParser(DocumentationParser):
    def parse(self, content: str, file_path: str) -> DocumentationFile:
        # Implementation
        pass

# Add a new code extractor
class MyLanguageExtractor:
    def extract(self, content: str, file_path: str) -> CodeFile:
        # Implementation
        pass
```

## Performance Considerations

### Optimization Features
- **Parallel Processing** - Multi-threaded analysis
- **Caching** - Intelligent result caching
- **Memory Management** - Efficient large file handling
- **Incremental Analysis** - Only analyze changed files

### Performance Tips
```python
# For large projects
system = DocumentationAnalysisSystem()
system.config.performance.parallel_processing.enabled = True
system.config.performance.parallel_processing.max_workers = 8

# Enable caching
system.config.performance.caching.enabled = True
system.config.performance.caching.cache_ttl = 3600
```

## Contributing

### Development Setup
```bash
# Fork and clone the repository
git clone https://github.com/yourusername/zamaz-debate-mcp.git
cd zamaz-debate-mcp

# Create virtual environment
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install development dependencies
pip install -r .github/scripts/requirements_documentation.txt
pip install -r requirements-dev.txt

# Run tests
pytest .github/scripts/tests/
```

### Adding New Features
1. Create feature branch
2. Implement with tests
3. Update documentation
4. Submit pull request

### Code Style
- Follow PEP 8
- Use type hints
- Write comprehensive docstrings
- Include unit tests

## Troubleshooting

### Common Issues

**ImportError: No module named 'sentence_transformers'**
```bash
# Install optional dependencies
pip install sentence-transformers
```

**Java parsing errors**
```bash
# Install javalang
pip install javalang
```

**Memory issues with large projects**
```bash
# Use chunked processing
python run_documentation_analysis.py . --quick
```

### Debug Mode
```bash
# Enable verbose logging
python run_documentation_analysis.py . -v

# Check log file
tail -f documentation_analysis.log
```

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

- **Documentation**: See this README and inline documentation
- **Issues**: Report bugs via GitHub Issues
- **Discussions**: Join GitHub Discussions for questions
- **Contributing**: See CONTRIBUTING.md for guidelines

## Roadmap

### Version 2.0 (Planned)
- [ ] Real-time analysis with file watching
- [ ] Advanced semantic search with transformers
- [ ] Integration with more documentation tools
- [ ] Web-based dashboard
- [ ] API endpoint for CI/CD integration

### Version 2.1 (Future)
- [ ] Machine learning-powered quality scoring
- [ ] Automated documentation generation
- [ ] Multi-language support (non-English)
- [ ] Plugin system for custom analyzers
- [ ] Advanced visualization and reporting

---

For more information, see the [project documentation](docs/) or contact the development team.