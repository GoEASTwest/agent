package com.hp.aiagent.maintenance.model;

public record ImageAnalysisRequest(
        String deviceType,
        String fileName,
        String visualDescription
) {
}
