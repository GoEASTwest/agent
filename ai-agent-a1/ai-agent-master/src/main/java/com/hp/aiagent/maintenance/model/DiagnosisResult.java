package com.hp.aiagent.maintenance.model;

import java.util.List;

public record DiagnosisResult(
        String riskLevel,
        int score,
        List<String> evidence,
        List<String> possibleCauses,
        List<String> recommendedActions,
        List<FaultCase> similarCases,
        MaintenanceTask generatedTask
) {
}
