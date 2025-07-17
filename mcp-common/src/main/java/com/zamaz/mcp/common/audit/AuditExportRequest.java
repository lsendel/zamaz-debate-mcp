package com.zamaz.mcp.common.audit;

import lombok.Builder;
import lombok.Data;

/**
 * Request for exporting audit trail data
 */
@Data
@Builder
public class AuditExportRequest {
    
    /**
     * Search criteria for filtering audit entries
     */
    private AuditSearchCriteria criteria;
    
    /**
     * Export format
     */
    private ExportFormat format;
    
    /**
     * Include sensitive data in export
     */
    private boolean includeSensitiveData;
    
    /**
     * Export format enum
     */
    public enum ExportFormat {
        CSV,
        JSON,
        XML,
        PDF
    }
}