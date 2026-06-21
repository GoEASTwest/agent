package com.hp.aiagent.maintenance.model;

import java.util.List;

public record ReportCorrectionRequest(
        String reportId,
        String correctedRiskLevel,
        List<String> correctedEvidence,
        List<String> correctedCauses,
        List<String> correctedActions,
        String reviewer,
        String reviewNote
) {
}
