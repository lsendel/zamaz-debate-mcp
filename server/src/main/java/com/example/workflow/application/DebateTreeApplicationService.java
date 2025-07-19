package com.example.workflow.application;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class DebateTreeApplicationService {
    
    public List<DebateNode> buildDebateTree() {
        return List.of(
            new DebateNode("debate-1", "Root Debate", null, List.of("debate-2", "debate-3")),
            new DebateNode("debate-2", "Child Debate 1", "debate-1", List.of()),
            new DebateNode("debate-3", "Child Debate 2", "debate-1", List.of())
        );
    }
}

record DebateNode(String id, String title, String parentId, List<String> childIds) {}