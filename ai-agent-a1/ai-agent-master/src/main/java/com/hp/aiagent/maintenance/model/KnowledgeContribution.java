package com.hp.aiagent.maintenance.model;

import java.time.LocalDateTime;
import java.util.List;

public record KnowledgeContribution(
        String id,
        String title,
        String deviceType,
        String faultName,
        List<String> symptoms,
        List<String> imageFeatures,
        String cause,
        String solution,
        String content,
        String submitter,
        String status,
        String reviewNote,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt,
        String markdownDownloadUrl,
        String pdfDownloadUrl
) {
}
