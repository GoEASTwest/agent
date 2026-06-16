package com.hp.aiagent.maintenance.model;

import java.util.List;

public record FaultCase(
        String id,
        String deviceType,
        String faultName,
        List<String> symptoms,
        List<String> imageFeatures,
        String cause,
        String solution,
        int severity
) {
}
