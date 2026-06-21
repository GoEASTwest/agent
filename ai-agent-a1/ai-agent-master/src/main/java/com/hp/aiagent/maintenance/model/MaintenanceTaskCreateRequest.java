package com.hp.aiagent.maintenance.model;

import java.util.List;

public record MaintenanceTaskCreateRequest(
        String id,
        String deviceId,
        String title,
        String priority,
        String status,
        List<String> steps,
        List<String> spareParts,
        List<String> acceptanceCriteria
) {
}
