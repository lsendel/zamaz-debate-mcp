/**
 * RAG Service Fixtures and Utilities
 * This file contains reusable RAG service functions and test data
 */

function fn() {
    const config = karate.callSingle('classpath:karate-config.js');
    const authFixtures = karate.callSingle('classpath:fixtures/auth-fixtures.js');

    const ragFixtures = {
        // Document cache
        documentCache: {},

        // Generate document upload request
        generatedocumentrequest: function(overrides) {
            let defaultRequest = {
                title: "Test Document",
                content: "This is a test document for RAG processing. It contains information about artificial intelligence and machine learning concepts.",
                type: "text/plain",
                metadata: {
                    author: "Test Author",
                    source: "Test Source",
                    category: "technology",
                    tags: ["AI", "ML", "test"]
                },
                processingOptions: {
                    chunkSize: 512,
                    chunkOverlap: 128,
                    generateEmbeddings: true,
                    extractEntities: true,
                    generateSummary: true
                }
            }

            return Object.assign({}, defaultRequest, overrides || {});
        },

        // Generate search request
        generatesearchrequest: function(query, overrides) {
            let defaultRequest = {
                query: query || "artificial intelligence",
                maxResults: 10,
                minScore: 0.5,
                includeMetadata: true,
                includeContent: true,
                filters: {},
                searchType: "semantic"
            }

            return Object.assign({}, defaultRequest, overrides || {});
        },

        // Generate knowledge base request
        generateknowledgebaserequest: function(overrides) {
            const defaultRequest = {
                name: "Test Knowledge Base",
                description: "A test knowledge base for RAG operations",
                settings: {
                    embeddingModel: "text-embedding-ada-002",
                    chunkSize: 512,
                    chunkOverlap: 128,
                    maxDocuments: 1000,
                    retentionDays: 30
                },
                isPublic: false
            }

            return Object.assign({}, defaultRequest, overrides || {});
        },

        // Upload document
        uploaddocument: function(documentData, knowledgeBaseId, authToken) {
            if (!authToken) {
                let auth = authFixtures.login();
                authToken = auth.token;
            }

            const documentRequest = ragFixtures.generateDocumentRequest(documentData);

            let response = karate.call('classpath:rag/upload-document.feature', {
                knowledgeBaseId: knowledgeBaseId || 'default',
                documentRequest: documentRequest,
                authToken: authToken,
                baseUrl: config.serviceUrls.rag
            });

            if (response.response && response.response.id) {
                let document = response.response;
                ragFixtures.documentCache[document.id] = document;
                return document;
            }

            throw new Error('Failed to upload document');
        },

        // Get document
        getdocument: function(documentId, authToken) {
            if (!authToken) {
                let auth = authFixtures.login();
                authToken = auth.token;
            }

            let response = karate.call('classpath:rag/get-document.feature', {
                documentId: documentId,
                authToken: authToken,
                baseUrl: config.serviceUrls.rag
            });

            if (response.response && response.response.id) {
                return response.response;
            }

            throw new Error('Failed to get document: ' + documentId);
        },

        // Search documents
        searchdocuments: function(searchData, knowledgeBaseId, authToken) {
            if (!authToken) {
                let auth = authFixtures.login();
                authToken = auth.token;
            }

            const searchRequest = ragFixtures.generateSearchRequest(searchData.query, searchData);

            let response = karate.call('classpath:rag/search-documents.feature', {
                knowledgeBaseId: knowledgeBaseId || 'default',
                searchRequest: searchRequest,
                authToken: authToken,
                baseUrl: config.serviceUrls.rag
            });

            if (response.response && response.response.results) {
                return response.response.results;
            }

            return []
        },

        // Delete document
        deletedocument: function(documentId, authToken) {
            if (!authToken) {
                let auth = authFixtures.login();
                authToken = auth.token;
            }

            let response = karate.call('classpath:rag/delete-document.feature', {
                documentId: documentId,
                authToken: authToken,
                baseUrl: config.serviceUrls.rag
            });

            // Remove from cache
            if (ragFixtures.documentCache[documentId]) {
                delete ragFixtures.documentCache[documentId]
            }

            return response.response;
        },

        // Create knowledge base
        createknowledgebase: function(kbData, authToken) {
            if (!authToken) {
                let auth = authFixtures.login();
                authToken = auth.token;
            }

            const kbRequest = ragFixtures.generateKnowledgeBaseRequest(kbData);

            let response = karate.call('classpath:rag/create-knowledge-base.feature', {
                knowledgeBaseRequest: kbRequest,
                authToken: authToken,
                baseUrl: config.serviceUrls.rag
            });

            if (response.response && response.response.id) {
                return response.response;
            }

            throw new Error('Failed to create knowledge base');
        },

        // Process document
        processdocument: function(documentId, processingOptions, authToken) {
            if (!authToken) {
                let auth = authFixtures.login();
                authToken = auth.token;
            }

            let response = karate.call('classpath:rag/process-document.feature', {
                documentId: documentId,
                processingOptions: processingOptions || {},
                authToken: authToken,
                baseUrl: config.serviceUrls.rag
            });

            if (response.response) {
                return response.response;
            }

            throw new Error('Failed to process document: ' + documentId);
        },

        // Generate embeddings
        generateembeddings: function(text, model, authToken) {
            if (!authToken) {
                let auth = authFixtures.login();
                authToken = auth.token;
            }

            let response = karate.call('classpath:rag/generate-embeddings.feature', {
                text: text,
                model: model || 'text-embedding-ada-002',
                authToken: authToken,
                baseUrl: config.serviceUrls.rag
            });

            if (response.response && response.response.embeddings) {
                return response.response.embeddings;
            }

            throw new Error('Failed to generate embeddings');
        },

        // Upload multiple documents
        uploadmultipledocuments: function(documents, knowledgeBaseId, authToken) {
            if (!authToken) {
                let auth = authFixtures.login();
                authToken = auth.token;
            }

            const uploadedDocs = []
            for (var item of documents) {
                const doc = ragFixtures.uploadDocument(item, knowledgeBaseId, authToken);
                uploadedDocs.push(doc);
            }

            return uploadedDocs;
        },

        // Create knowledge base with documents
        createknowledgebasewithdocuments: function(kbData, documents, authToken) {
            if (!authToken) {
                let auth = authFixtures.login();
                authToken = auth.token;
            }

            // Create knowledge base
            const knowledgeBase = ragFixtures.createKnowledgeBase(kbData, authToken);

            // Upload documents
            const uploadedDocuments = ragFixtures.uploadMultipleDocuments(documents, knowledgeBase.id, authToken);

            return {
                knowledgeBase: knowledgeBase,
                documents: uploadedDocuments
            }
        },

        // Get document chunks
        getdocumentchunks: function(documentId, authToken) {
            if (!authToken) {
                const auth = authFixtures.login();
                authToken = auth.token;
            }

            const response = karate.call('classpath:rag/get-document-chunks.feature', {
                documentId: documentId,
                authToken: authToken,
                baseUrl: config.serviceUrls.rag
            });

            if (response.response && response.response.chunks) {
                return response.response.chunks;
            }

            return []
        },

        // Validate document response
        validatedocumentresponse: function(response) {
            const validationErrors = []

            if (!response.id || typeof response.id !== 'string') {
                validationErrors.push('Missing or invalid document ID');
            }

            if (!response.title || typeof response.title !== 'string') {
                validationErrors.push('Missing or invalid document title');
            }

            if (!response.content || typeof response.content !== 'string') {
                validationErrors.push('Missing or invalid document content');
            }

            if (!response.type || typeof response.type !== 'string') {
                validationErrors.push('Missing or invalid document type');
            }

            if (!response.status || typeof response.status !== 'string') {
                validationErrors.push('Missing or invalid document status');
            }

            if (!response.uploadedAt || typeof response.uploadedAt !== 'string') {
                validationErrors.push('Missing or invalid upload timestamp');
            }

            if (response.metadata && typeof response.metadata !== 'object') {
                validationErrors.push('Invalid metadata object');
            }

            if (response.processingStatus && typeof response.processingStatus !== 'object') {
                validationErrors.push('Invalid processing status object');
            }

            return validationErrors;
        },

        // Generate test documents
        generatetestdocuments: function() {
            return [
                {
                    title: "Artificial Intelligence Overview",
                    content: "Artificial Intelligence (AI) is a branch of computer science that aims to create intelligent machines capable of performing tasks that typically require human intelligence. AI systems can learn, reason, perceive, and make decisions. Machine learning is a subset of AI that enables computers to learn and improve from experience without being explicitly programmed.",
                    type: "text/plain",
                    metadata: {
                        author: "AI Research Team",
                        source: "AI Handbook",
                        category: "technology",
                        tags: ["AI", "machine learning", "technology"]
                    }
                },
                {
                    title: "Machine Learning Fundamentals",
                    content: "Machine Learning (ML) is a method of data analysis that automates analytical model building. It uses algorithms that iteratively learn from data, allowing computers to find hidden insights without being explicitly programmed where to look. ML algorithms build mathematical models based on training data to make predictions or decisions.",
                    type: "text/plain",
                    metadata: {
                        author: "ML Experts",
                        source: "ML Textbook",
                        category: "education",
                        tags: ["machine learning", "algorithms", "data science"]
                    }
                },
                {
                    title: "Deep Learning Applications",
                    content: "Deep Learning is a subset of machine learning that uses neural networks with multiple layers to model and understand complex patterns in data. It has revolutionized fields like computer vision, natural language processing, and speech recognition. Deep learning models can automatically learn features from raw data without manual feature engineering.",
                    type: "text/plain",
                    metadata: {
                        author: "Deep Learning Lab",
                        source: "Research Paper",
                        category: "research",
                        tags: ["deep learning", "neural networks", "computer vision"]
                    }
                },
                {
                    title: "Natural Language Processing",
                    content: "Natural Language Processing (NLP) is a field of artificial intelligence that focuses on the interaction between computers and humans using natural language. NLP combines computational linguistics with statistical, machine learning, and deep learning models to enable computers to process and analyze large amounts of natural language data.",
                    type: "text/plain",
                    metadata: {
                        author: "NLP Team",
                        source: "NLP Guide",
                        category: "linguistics",
                        tags: ["NLP", "computational linguistics", "text processing"]
                    }
                },
                {
                    title: "Computer Vision Basics",
                    content: "Computer Vision is a field of artificial intelligence that trains computers to interpret and understand the visual world. Using digital images from cameras and videos and deep learning models, machines can accurately identify and classify objects and react to what they see. Computer vision applications include facial recognition, autonomous vehicles, and medical image analysis.",
                    type: "text/plain",
                    metadata: {
                        author: "Vision Research Group",
                        source: "Computer Vision Manual",
                        category: "technology",
                        tags: ["computer vision", "image processing", "object detection"]
                    }
                }
            ]
        },

        // Generate test search queries
        generatetestsearchqueries: function() {
            return [
                "What is artificial intelligence?",
                "How does machine learning work?",
                "Deep learning neural networks",
                "Natural language processing applications",
                "Computer vision image recognition",
                "AI algorithms and methods",
                "Machine learning training data",
                "Deep learning vs machine learning",
                "NLP text analysis techniques",
                "Computer vision object detection"
            ]
        },

        // Generate performance test scenarios
        generateperformancescenarios: function() {
            return {
                bulk_upload: {
                    description: "Upload multiple documents simultaneously",
                    documentCount: 50,
                    expectedMaxTime: 60000
                },
                concurrent_search: {
                    description: "Perform multiple searches concurrently",
                    searchCount: 20,
                    expectedMaxTime: 30000
                },
                large_document: {
                    description: "Upload and process large document",
                    documentSize: 1048576, // 1MB
                    expectedMaxTime: 120000
                },
                embedding_generation: {
                    description: "Generate embeddings for multiple texts",
                    textCount: 100,
                    expectedMaxTime: 45000
                }
            }
        },

        // Clear document cache
        cleardocumentcache: function() {
            ragFixtures.documentCache = {}
        },

        // Generate file content for different types
        generatefilecontent: function(type, size) {
            size = size || 1000;

            let content = ""
            const baseContent = "This is test content for RAG processing. It contains information about various topics including artificial intelligence, machine learning, deep learning, natural language processing, and computer vision. "

            switch (type) {
                case 'text':;
                    while (content.length < size) {
                        content += baseContent;
                    }
                    break
                case 'json':;
                    content = JSON.stringify({
                        title: "Test JSON Document",
                        content: baseContent,
                        metadata: {
                            type: "json",
                            size: size,
                            generated: new Date().toISOString()
                        }
                    });
                    break
                case 'markdown':;
                    content = "# Test Markdown Document\n\n" + baseContent + "\n\n## Section 1\n\n" + baseContent + "\n\n## Section 2\n\n" + baseContent;
                    break
                default:;
                    content = baseContent.repeat(Math.ceil(size / baseContent.length));
            }

            return content.substring(0, size);
        },

        // Wait for document processing
        waitfordocumentprocessing: function(documentId, timeout, authToken) {
            timeout = timeout || 30000;
            const startTime = Date.now();

            while ((Date.now() - startTime) < timeout) {
                const document = ragFixtures.getDocument(documentId, authToken);

                if (document.processingStatus && document.processingStatus.status === 'completed') {
                    return document;
                } else if (document.processingStatus && document.processingStatus.status === 'failed') {
                    throw new Error('Document processing failed: ' + document.processingStatus.error);
                }

                java.lang.Thread.sleep(1000);
            }

            throw new Error('Document processing timeout');
        }
    }

    return ragFixtures;
}
