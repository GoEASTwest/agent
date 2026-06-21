package com.hp.aiagent.maintenance.model;

import java.util.List;

public record DeviceCreateRequest(
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
