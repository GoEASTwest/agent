package com.hp.aiagent.maintenance.model;

import java.time.LocalDateTime;

public record TaskFlowEvent(
        String taskId,
        String fromStatus,
        String toStatus,
        String operatorRole,
        String note,
        LocalDateTime operatedAt
) {
}
