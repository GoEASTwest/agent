package com.hp.aiagent.maintenance.model;

import java.util.List;

public record ImageAnalysisResult(
        String fileName,
        List<String> detectedFeatures,
        List<FaultCase> similarCases,
        List<String> inspectionTips
) {
}
