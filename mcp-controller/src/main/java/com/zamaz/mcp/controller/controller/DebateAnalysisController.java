package com.zamaz.mcp.controller.controller;

import com.zamaz.mcp.controller.ai.DebateQualityScorer;
import com.zamaz.mcp.controller.dto.DebateAnalysisDto;
import com.zamaz.mcp.controller.entity.Debate;
import com.zamaz.mcp.controller.service.DebateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

/**
 * Controller for debate analysis and quality scoring
 */
@RestController
@RequestMapping("/api/v1/debates/{debateId}/analysis")
@RequiredArgsConstructor
@Slf4j
public class DebateAnalysisController {
    
    private final DebateQualityScorer debateQualityScorer;
    private final DebateService debateService;
    
    /**
     * Trigger comprehensive debate quality analysis
     */
    @PostMapping("/quality")
    @PreAuthorize("hasPermission(#debateId, 'Debate', 'READ')")
    public CompletableFuture<ResponseEntity<DebateAnalysisDto>> analyzeDebateQuality(
            @PathVariable Long debateId,
            @RequestHeader("X-Organization-ID") String organizationId) {
        
        log.info("Starting debate quality analysis for debate: {} in organization: {}", debateId, organizationId);
        
        try {
            // Get the debate
            Debate debate = debateService.findById(debateId);
            
            if (debate == null) {
                return CompletableFuture.completedFuture(ResponseEntity.notFound().build());
            }
            
            // Trigger analysis
            return debateQualityScorer.analyzeDebateQuality(debate)
                .thenApply(analysis -> {
                    if (analysis != null) {
                        log.info("Debate analysis completed for debate: {} with overall score: {}", 
                            debateId, analysis.getOverallQualityScore());
                        return ResponseEntity.ok(analysis);
                    } else {
                        log.warn("Debate analysis returned null for debate: {}", debateId);
                        return ResponseEntity.noContent().build();
                    }
                })
                .exceptionally(throwable -> {
                    log.error("Error during debate analysis for debate: {}", debateId, throwable);
                    return ResponseEntity.internalServerError().build();
                });
                
        } catch (Exception e) {
            log.error("Error starting debate analysis for debate: {}", debateId, e);
            return CompletableFuture.completedFuture(ResponseEntity.internalServerError().build());
        }
    }
    
    /**
     * Get previous analysis results if available
     */
    @GetMapping("/quality/latest")
    @PreAuthorize("hasPermission(#debateId, 'Debate', 'READ')")
    public ResponseEntity<DebateAnalysisDto> getLatestAnalysis(
            @PathVariable Long debateId,
            @RequestHeader("X-Organization-ID") String organizationId) {
        
        // This would retrieve stored analysis results from database
        // For now, return not implemented
        return ResponseEntity.notFound().build();
    }
    
    /**
     * Get analysis status for long-running analyses
     */
    @GetMapping("/status")
    @PreAuthorize("hasPermission(#debateId, 'Debate', 'READ')")
    public ResponseEntity<AnalysisStatus> getAnalysisStatus(
            @PathVariable Long debateId,
            @RequestHeader("X-Organization-ID") String organizationId) {
        
        // This would check if analysis is in progress
        // For now, return simple status
        AnalysisStatus status = AnalysisStatus.builder()
            .debateId(debateId.toString())
            .status("READY")
            .message("Analysis can be triggered")
            .build();
            
        return ResponseEntity.ok(status);
    }
    
    /**
     * Simple status response for analysis operations
     */
    @lombok.Builder
    @lombok.Data
    public static class AnalysisStatus {
        private String debateId;
        private String status; // READY, IN_PROGRESS, COMPLETED, ERROR
        private String message;
        private Double progress; // 0.0 to 1.0
    }
}