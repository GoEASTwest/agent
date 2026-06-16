package com.hp.aiagent.maintenance.model;

import java.util.List;

public record DeviceAsset(
        String id,
        String name,
        String type,
        String location,
        String status,
        String riskLevel,
        List<String> sensors,
        String lastInspectionTime
) {
}
