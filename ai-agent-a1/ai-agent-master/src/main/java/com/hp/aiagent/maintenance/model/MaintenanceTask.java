package com.hp.aiagent.maintenance.model;

import java.time.LocalDateTime;
import java.util.List;

public record MaintenanceTask(
        String id,
        String deviceId,
        String title,
        String priority,
        String status,
        List<String> steps,
        List<String> spareParts,
        List<String> acceptanceCriteria,
        LocalDateTime createdAt
) {
}
