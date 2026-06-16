package com.hp.aiagent.maintenance.model;

import java.util.List;

public record VisionAnalysisResult(
        String provider,
        String model,
        List<String> detectedFeatures,
        String conclusion,
        List<FaultCase> similarCases,
        List<String> recommendedActions
) {
}
