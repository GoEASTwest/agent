package com.hp.aiagent.maintenance.model;

public record TaskFlowUpdateRequest(
        String status,
        String operatorRole,
        String note
) {
}
