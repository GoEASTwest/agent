package com.hp.aiagent.maintenance.model;

import java.util.List;

public record KnowledgeContributionRequest(
        String title,
        String deviceType,
        String faultName,
        List<String> symptoms,
        List<String> imageFeatures,
        String cause,
        String solution,
        String content,
        String submitter
) {
}
