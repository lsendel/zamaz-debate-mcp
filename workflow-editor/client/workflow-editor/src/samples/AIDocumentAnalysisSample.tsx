import { useWorkflowStore } from '../store/workflowStore';

interface DocumentSelection {
  text: string;
  pageNumber: number;
  startOffset: number;
  endOffset: number;
}

interface AIAnalysisResult {
  summary: string;
  entities: Array<{ type: string; value: string; confidence: number }>;
  sentiment: { label: string; score: number };
  keyPhrases: string[];
  structuredData: Record<string, any>;
}

interface ExportFormat {
  format: 'json' | 'csv' | 'xml';
  includeMetadata: boolean;
  selectedFields: string[];
}

const AIDocumentAnalysisSample: React.FC = () => {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [documentContent, setDocumentContent] = useState<string>('');
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [selections, setSelections] = useState<DocumentSelection[]>([]);
  const [analysisResults, setAnalysisResults] = useState<AIAnalysisResult | null>(null);
  const [exportFormat, setExportFormat] = useState<ExportFormat>({
    format: 'json',
    includeMetadata: true,
    selectedFields: []
  });
  const [highlightedEntities, setHighlightedEntities] = useState<Set<string>>(new Set());
  const fileInputRef = useRef<HTMLInputElement>(null);
  const { createWorkflow } = useWorkflowStore();

  // Call mcp-llm service for AI analysis
  const analyzeWithAI = useMutation({
    mutationFn: async (content: string) => {
      try {
        const response = await fetch('http://localhost:5002/api/analyze', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ 
            content,
            tasks: ['summarize', 'entities', 'sentiment', 'keyPhrases', 'structuredData']
          })
        });
        if (!response.ok) throw new Error('AI analysis failed');
        return response.json();
      } catch (error) {
        // Log error for debugging
        console.error('[AIDocumentAnalysisSample] Error:', error);
        // Rethrow if critical
        if (error.critical) throw error;
          console.error("Error:", error);
        // Return mock analysis if service is not available
        return generateMockAnalysis(content);
        console.error("Error:", error);
      }
    },
    onSuccess: (data) => {
      setAnalysisResults(data);
    }
  });

  // Generate mock AI analysis
  const generateMockAnalysis = (content: string): AIAnalysisResult => {
    const words = content.split(' ').filter(w => w.length > 3);
    const entities = [
      { type: 'PERSON', value: 'John Doe', confidence: 0.95 },
      { type: 'ORGANIZATION', value: 'Acme Corp', confidence: 0.89 },
      { type: 'LOCATION', value: 'New York', confidence: 0.92 },
      { type: 'DATE', value: '2024-01-15', confidence: 0.87 }
    ];
    
    return {
      summary: `This document contains ${words.length} significant words and discusses various topics including business operations, financial data, and strategic planning.`,
      entities: entities.filter(() => Math.random() > 0.3),
      sentiment: {
        label: ['positive', 'neutral', 'negative'][Math.floor(Math.random() * 3)],
        score: Math.random()
      },
      keyPhrases: words.slice(0, 10).filter(() => Math.random() > 0.5),
      structuredData: {
        documentType: 'Business Report',
        date: new Date().toISOString(),
        confidenceScore: 0.85,
        topics: ['Finance', 'Strategy', 'Operations'],
        metadata: {
          pageCount: totalPages,
          wordCount: words.length,
          language: 'en'
        }
      }
    };
  };

  // Handle file upload
  const handleFileUpload = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;
    
    // Validate file type
    const allowedTypes = ['text/plain', 'application/pdf', 'text/markdown', 'text/csv'];
    const maxSize = 10 * 1024 * 1024; // 10MB
    
    if (!allowedTypes.includes(file.type) && !file.name.toLowerCase().endsWith('.txt')) {
      alert('Please select a supported file type: PDF, TXT, MD, or CSV');
      return;
    }
    
    if (file.size > maxSize) {
      alert('File size must be less than 10MB');
      return;
    }
    
    setSelectedFile(file);
    
    const reader = new FileReader();
    reader.onload = (e) => {
      let content = '';
      
      if (file.type === 'application/pdf') {
        // For PDF files, show a placeholder message
        content = `PDF Document: ${file.name}
        
This is a demonstration of PDF document analysis. In a production environment, this would:

1. Extract text content from the PDF using a PDF parsing library
2. Preserve formatting and page structure
3. Handle images and tables within the document
4. Extract metadata such as creation date, author, etc.

For this demo, we'll simulate PDF content analysis with the following sample content:

Executive Summary
This quarterly business report outlines our company's performance metrics, strategic initiatives, and financial highlights for Q4 2024. Our organization has demonstrated significant growth across multiple key performance indicators.

Key Findings:
• Revenue increased by 15% compared to the previous quarter
• Customer satisfaction scores improved to 4.2/5.0
• Market expansion into three new geographic regions
• Implementation of advanced analytics platform

Financial Performance
Total revenue: $2.4M (+15% QoQ)
Operating expenses: $1.8M (+8% QoQ)
Net profit margin: 25%
Cash flow: $600K positive

Strategic Initiatives
1. Digital transformation roadmap
2. Sustainability program launch
3. Employee development program
4. Customer experience enhancement

Market Analysis
The market conditions have been favorable, with increased demand for our core services. Competitive analysis shows we maintain a strong position in our primary market segments.

Recommendations
• Continue investment in technology infrastructure
• Expand customer support capabilities
• Explore partnership opportunities
• Enhance data analytics capabilities

Risk Assessment
Primary risks include market volatility, supply chain disruptions, and regulatory changes. Mitigation strategies are in place for each identified risk factor.

Conclusion
The quarter has been successful, positioning us well for continued growth. The strategic initiatives are on track and expected to drive further improvements in the coming quarters.`;
      } else {
        // Handle text files
        content = e.target?.result as string;
      }
      
      setDocumentContent(content);
      // Simulate multi-page by splitting content into pages
      const pages = content.match(/.{1,2000}/gs) || [content];
      setTotalPages(pages.length);
      setCurrentPage(1);
    };
    
    if (file.type === 'application/pdf') {
      reader.readAsArrayBuffer(file);
    } else {
      reader.readAsText(file);
    }
  };

  // Handle text selection
  const handleTextSelection = () => {
    const selection = window.getSelection();
    if (!selection || selection.isCollapsed) return;
    
    const text = selection.toString();
    const range = selection.getRangeAt(0);
    
    const newSelection: DocumentSelection = {
      text,
      pageNumber: currentPage,
      startOffset: range.startOffset,
      endOffset: range.endOffset
    };
    
    setSelections([...selections, newSelection]);
    
    // Analyze selection
    analyzeWithAI.mutate(text);
  };

  // Create document processing workflow
  const createDocumentWorkflow = () => {
    const workflow = {
      id: 'document-analysis-workflow',
      name: 'AI Document Analysis Workflow',
      description: 'Automated document processing with AI extraction',
      status: 'published' as const,
      nodes: [
        {
          id: 'start',
          type: 'start',
          position: { x: 100, y: 100 },
          data: { 
            label: 'Document Upload',
            configuration: { trigger: 'manual' }
          }
        },
        {
          id: 'validate',
          type: 'task',
          position: { x: 300, y: 100 },
          data: {
            label: 'Validate Document',
            configuration: {
              taskType: 'VALIDATION',
              timeout: 30
            }
          }
        },
        {
          id: 'extract',
          type: 'task',
          position: { x: 500, y: 100 },
          data: {
            label: 'AI Extraction',
            configuration: {
              taskType: 'AI_EXTRACTION',
              parallel: true
            }
          }
        },
        {
          id: 'analyze',
          type: 'task',
          position: { x: 700, y: 100 },
          data: {
            label: 'Content Analysis',
            configuration: {
              taskType: 'AI_ANALYSIS'
            }
          }
        },
        {
          id: 'decision',
          type: 'decision',
          position: { x: 900, y: 100 },
          data: {
            label: 'Quality Check',
            configuration: {
              decisionType: 'simple'
            }
          }
        },
        {
          id: 'export',
          type: 'task',
          position: { x: 1100, y: 50 },
          data: {
            label: 'Export Data',
            configuration: {
              taskType: 'EXPORT'
            }
          }
        },
        {
          id: 'review',
          type: 'task',
          position: { x: 1100, y: 150 },
          data: {
            label: 'Manual Review',
            configuration: {
              taskType: 'MANUAL_REVIEW'
            }
          }
        },
        {
          id: 'end',
          type: 'end',
          position: { x: 1300, y: 100 },
          data: { label: 'Complete' }
        }
      ],
      edges: [
        { id: 'e1', source: 'start', target: 'validate', animated: true },
        { id: 'e2', source: 'validate', target: 'extract', animated: true },
        { id: 'e3', source: 'extract', target: 'analyze', animated: true },
        { id: 'e4', source: 'analyze', target: 'decision', animated: true },
        { id: 'e5', source: 'decision', sourceHandle: 'true', target: 'export', animated: true },
        { id: 'e6', source: 'decision', sourceHandle: 'false', target: 'review', animated: true },
        { id: 'e7', source: 'export', target: 'end', animated: true },
        { id: 'e8', source: 'review', target: 'end', animated: true }
      ]
    };
    
    createWorkflow(workflow);
  };

  // Export structured data
  const exportData = () => {
    if (!analysisResults) return;
    
    let exportContent = '';
    const data = {
      ...analysisResults.structuredData,
      entities: analysisResults.entities,
      keyPhrases: analysisResults.keyPhrases,
      sentiment: analysisResults.sentiment
    };
    
    switch (exportFormat.format) {
      case 'json':
        exportContent = JSON.stringify(data, null, 2);
        break;
        
      case 'csv':
        const headers = Object.keys(data).join(',');
        const values = Object.values(data).map(v => 
          typeof v === 'object' ? JSON.stringify(v) : v
        ).join(',');
        exportContent = `${headers}\n${values}`;
        break;
        
      case 'xml':
        exportContent = `<?xml version="1.0" encoding="UTF-8"?>\n<document>\n`;
        Object.entries(data).forEach(([key, value]) => {
          exportContent += `  <${key}>${
            typeof value === 'object' ? JSON.stringify(value) : value
          }</${key}>\n`;
        });
        exportContent += '</document>';
        break;
    }
    
    // Download file
    const blob = new Blob([exportContent], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `analysis.${exportFormat.format}`;
    a.click();
    URL.revokeObjectURL(url);
  };

  // Highlight entities in text
  const highlightText = (text: string) => {
    if (!analysisResults || highlightedEntities.size === 0) return text;
    
    let highlightedText = text;
    analysisResults.entities.forEach(entity => {
      if (highlightedEntities.has(entity.type)) {
        const regex = new RegExp(`\\b${entity.value}\\b`, 'gi');
        highlightedText = highlightedText.replace(
          regex,
          `<mark class="entity-${entity.type.toLowerCase()}">${entity.value}</mark>`
        );
      }
    });
    
    return highlightedText;
  };

  return (
    <div className="ai-document-sample">
      <div className="sample-header">
        <h2>AI Document Analysis Sample</h2>
        <p>Upload documents for AI-powered extraction and analysis</p>
      </div>

      <div className="upload-section">
        <input
          ref={fileInputRef}
          type="file"
          accept=".pdf,.txt,.md,.csv,text/plain,application/pdf,text/markdown,text/csv"
          onChange={handleFileUpload}
          style={{ display: 'none' }}
        />
        
        <button
          className="upload-button"
          onClick={() => fileInputRef.current?.click()}
        >
          📄 Upload Document
        </button>
        
        <button
          className="workflow-button"
          onClick={createDocumentWorkflow}
        >
          📋 Create Analysis Workflow
        </button>
        
        {selectedFile && (
          <span className="file-name">{selectedFile.name}</span>
        )}
      </div>

      {documentContent && (
        <div className="document-viewer">
          <div className="viewer-controls">
            <button
              onClick={() => setCurrentPage(Math.max(1, currentPage - 1))}
              disabled={currentPage === 1}
            >
              ◀ Previous
            </button>
            <span className="page-info">
              Page {currentPage} of {totalPages}
            </span>
            <button
              onClick={() => setCurrentPage(Math.min(totalPages, currentPage + 1))}
              disabled={currentPage === totalPages}
            >
              Next ▶
            </button>
          </div>
          
          <div
            className="document-content"
            onMouseUp={handleTextSelection}
            dangerouslySetInnerHTML={{
              __html: highlightText(
                documentContent.slice((currentPage - 1) * 3000, currentPage * 3000)
              )
            }}
          />
        </div>
      )}

      {analysisResults && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="analysis-results"
        >
          <h3>AI Analysis Results</h3>
          
          <div className="result-section">
            <h4>Summary</h4>
            <p>{analysisResults.summary}</p>
          </div>
          
          <div className="result-section">
            <h4>Entities</h4>
            <div className="entity-filters">
              {['PERSON', 'ORGANIZATION', 'LOCATION', 'DATE'].map(type => (
                <label key={type} className="entity-filter">
                  <input
                    type="checkbox"
                    checked={highlightedEntities.has(type)}
                    onChange={(e) => {
                      const newHighlighted = new Set(highlightedEntities);
                      if (e.target.checked) {
                        newHighlighted.add(type);
                      } else {
                        newHighlighted.delete(type);
                      }
                      setHighlightedEntities(newHighlighted);
                    }}
                  />
                  <span className={`entity-type ${type.toLowerCase()}`}>{type}</span>
                </label>
              ))}
            </div>
            <div className="entities-list">
              {analysisResults.entities.map((entity, idx) => (
                <div key={idx} className="entity-item">
                  <span className={`entity-type ${entity.type.toLowerCase()}`}>
                    {entity.type}
                  </span>
                  <span className="entity-value">{entity.value}</span>
                  <span className="entity-confidence">
                    {(entity.confidence * 100).toFixed(0)}%
                  </span>
                </div>
              ))}
            </div>
          </div>
          
          <div className="result-section">
            <h4>Sentiment</h4>
            <div className="sentiment-result">
              <span className={`sentiment-label ${analysisResults.sentiment.label}`}>
                {analysisResults.sentiment.label}
              </span>
              <div className="sentiment-bar">
                <div
                  className="sentiment-fill"
                  style={{ width: `${analysisResults.sentiment.score * 100}%` }}
                />
              </div>
            </div>
          </div>
          
          <div className="result-section">
            <h4>Key Phrases</h4>
            <div className="key-phrases">
              {analysisResults.keyPhrases.map((phrase, idx) => (
                <span key={idx} className="key-phrase">{phrase}</span>
              ))}
            </div>
          </div>
          
          <div className="export-section">
            <h4>Export Structured Data</h4>
            <div className="export-controls">
              <select
                value={exportFormat.format}
                onChange={(e) => setExportFormat({
                  ...exportFormat,
                  format: e.target.value as any
                })}
              >
                <option value="json">JSON</option>
                <option value="csv">CSV</option>
                <option value="xml">XML</option>
              </select>
              
              <label>
                <input
                  type="checkbox"
                  checked={exportFormat.includeMetadata}
                  onChange={(e) => setExportFormat({
                    ...exportFormat,
                    includeMetadata: e.target.checked
                  })}
                />
                Include Metadata
              </label>
              
              <button className="export-button" onClick={exportData}>
                📥 Export
              </button>
            </div>
          </div>
        </motion.div>
      )}

      {selections.length > 0 && (
        <div className="selections-panel">
          <h3>Selected Text ({selections.length})</h3>
          <div className="selections-list">
            {selections.map((selection, idx) => (
              <div key={idx} className="selection-item">
                <span className="selection-page">Page {selection.pageNumber}</span>
                <span className="selection-text">"{selection.text}"</span>
                <button
                  className="analyze-button"
                  onClick={() => analyzeWithAI.mutate(selection.text)}
                >
                  🔍
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      <style>{`
        .ai-document-sample {
          padding: 20px;
          max-width: 1400px;
          margin: 0 auto;
        }

        .sample-header {
          text-align: center;
          margin-bottom: 30px;
        }

        .sample-header h2 {
          margin: 0 0 10px 0;
          color: #333;
        }

        .upload-section {
          display: flex;
          justify-content: center;
          align-items: center;
          gap: 20px;
          margin-bottom: 30px;
        }

        .upload-button,
        .workflow-button {
          padding: 12px 24px;
          border: none;
          border-radius: 4px;
          cursor: pointer;
          font-size: 16px;
          transition: background 0.2s;
        }

        .upload-button {
          background: #2196F3;
          color: white;
        }

        .workflow-button {
          background: #4CAF50;
          color: white;
        }

        .file-name {
          font-size: 14px;
          color: #666;
        }

        .document-viewer {
          background: white;
          border: 1px solid #e0e0e0;
          border-radius: 8px;
          margin-bottom: 30px;
          overflow: hidden;
        }

        .viewer-controls {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 15px;
          background: #f5f5f5;
          border-bottom: 1px solid #e0e0e0;
        }

        .viewer-controls button {
          padding: 8px 16px;
          border: 1px solid #ddd;
          background: white;
          border-radius: 4px;
          cursor: pointer;
        }

        .viewer-controls button:disabled {
          opacity: 0.5;
          cursor: not-allowed;
        }

        .page-info {
          font-size: 14px;
          color: #666;
        }

        .document-content {
          padding: 30px;
          min-height: 400px;
          font-size: 14px;
          line-height: 1.8;
          color: #333;
          user-select: text;
        }

        .document-content mark {
          padding: 2px 4px;
          border-radius: 3px;
        }

        .document-content mark.entity-person {
          background: #FFE082;
        }

        .document-content mark.entity-organization {
          background: #90CAF9;
        }

        .document-content mark.entity-location {
          background: #A5D6A7;
        }

        .document-content mark.entity-date {
          background: #CE93D8;
        }

        .analysis-results {
          background: white;
          border-radius: 8px;
          padding: 30px;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
          margin-bottom: 30px;
        }

        .analysis-results h3 {
          margin: 0 0 20px 0;
          color: #333;
        }

        .result-section {
          margin-bottom: 30px;
        }

        .result-section h4 {
          margin: 0 0 15px 0;
          color: #555;
        }

        .entity-filters {
          display: flex;
          gap: 15px;
          margin-bottom: 15px;
        }

        .entity-filter {
          display: flex;
          align-items: center;
          gap: 5px;
          cursor: pointer;
        }

        .entity-type {
          padding: 2px 8px;
          border-radius: 3px;
          font-size: 12px;
          font-weight: 500;
          text-transform: uppercase;
        }

        .entity-type.person {
          background: #FFE082;
          color: #F57C00;
        }

        .entity-type.organization {
          background: #90CAF9;
          color: #1565C0;
        }

        .entity-type.location {
          background: #A5D6A7;
          color: #2E7D32;
        }

        .entity-type.date {
          background: #CE93D8;
          color: #6A1B9A;
        }

        .entities-list {
          display: flex;
          flex-direction: column;
          gap: 10px;
        }

        .entity-item {
          display: flex;
          align-items: center;
          gap: 15px;
          padding: 10px;
          background: #f9f9f9;
          border-radius: 4px;
        }

        .entity-value {
          flex: 1;
          font-weight: 500;
        }

        .entity-confidence {
          font-size: 12px;
          color: #666;
        }

        .sentiment-result {
          display: flex;
          align-items: center;
          gap: 20px;
        }

        .sentiment-label {
          padding: 6px 12px;
          border-radius: 4px;
          font-weight: 500;
        }

        .sentiment-label.positive {
          background: #e8f5e9;
          color: #2e7d32;
        }

        .sentiment-label.neutral {
          background: #f5f5f5;
          color: #666;
        }

        .sentiment-label.negative {
          background: #ffebee;
          color: #c62828;
        }

        .sentiment-bar {
          flex: 1;
          height: 20px;
          background: #f0f0f0;
          border-radius: 10px;
          overflow: hidden;
        }

        .sentiment-fill {
          height: 100%;
          background: #2196F3;
          transition: width 0.3s;
        }

        .key-phrases {
          display: flex;
          flex-wrap: wrap;
          gap: 10px;
        }

        .key-phrase {
          padding: 6px 12px;
          background: #e3f2fd;
          color: #1976D2;
          border-radius: 16px;
          font-size: 14px;
        }

        .export-section {
          border-top: 1px solid #e0e0e0;
          padding-top: 20px;
        }

        .export-controls {
          display: flex;
          align-items: center;
          gap: 20px;
        }

        .export-controls select {
          padding: 8px 12px;
          border: 1px solid #ddd;
          border-radius: 4px;
        }

        .export-button {
          padding: 8px 16px;
          background: #FF9800;
          color: white;
          border: none;
          border-radius: 4px;
          cursor: pointer;
        }

        .selections-panel {
          background: white;
          border-radius: 8px;
          padding: 20px;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
        }

        .selections-panel h3 {
          margin: 0 0 15px 0;
          color: #333;
        }

        .selections-list {
          display: flex;
          flex-direction: column;
          gap: 10px;
        }

        .selection-item {
          display: flex;
          align-items: center;
          gap: 15px;
          padding: 10px;
          background: #f9f9f9;
          border-radius: 4px;
        }

        .selection-page {
          font-size: 12px;
          color: #666;
        }

        .selection-text {
          flex: 1;
          font-style: italic;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .analyze-button {
          padding: 4px 8px;
          background: #2196F3;
          color: white;
          border: none;
          border-radius: 4px;
          cursor: pointer;
          font-size: 12px;
        }
      `}</style>
    </div>
  );
};

export default AIDocumentAnalysisSample;