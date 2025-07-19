package com.example.workflow.application;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class DocumentAnalysisApplicationService {
    
    public DocumentAnalysisResult analyzeDocument(String documentContent) {
        return new DocumentAnalysisResult(
            "Sample Document",
            List.of("Key Point 1", "Key Point 2"),
            List.of("Suggestion 1", "Suggestion 2")
        );
    }
}

record DocumentAnalysisResult(String title, List<String> keyPoints, List<String> suggestions) {}