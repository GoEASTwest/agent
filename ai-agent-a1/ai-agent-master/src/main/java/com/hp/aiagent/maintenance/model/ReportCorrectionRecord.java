package com.hp.aiagent.maintenance.model;

import java.time.LocalDateTime;
import java.util.List;

public record ReportCorrectionRecord(
        String id,
        String sourceReportId,
        String correctedReportId,
        String reviewer,
        String correctedRiskLevel,
        List<String> correctedEvidence,
        List<String> correctedCauses,
        List<String> correctedActions,
        String reviewNote,
        LocalDateTime createdAt
) {
}
