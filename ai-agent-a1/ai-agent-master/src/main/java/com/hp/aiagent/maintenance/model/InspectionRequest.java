package com.hp.aiagent.maintenance.model;

import java.util.List;

public record InspectionRequest(
        String deviceId,
        String deviceType,
        String description,
        Double temperature,
        Double vibration,
        Double current,
        List<String> imageFeatures
) {
}
