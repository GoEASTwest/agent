package com.hp.aiagent.maintenance.model;

public record VisionAnalysisRequest(
        String deviceType,
        String imageUrl,
        String visualDescription,
        String question
) {
}
