package com.hp.aiagent.maintenance.model;

import java.time.LocalDateTime;
import java.util.List;

public record InspectionRecord(
        String id,
        String deviceId,
        String deviceType,
        String description,
        Double temperature,
        Double vibration,
        Double current,
        List<String> imageFeatures,
        String riskLevel,
        int score,
        List<String> evidence,
        LocalDateTime createdAt
) {
}
