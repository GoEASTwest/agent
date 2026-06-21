package com.hp.aiagent.maintenance.model;

import java.time.LocalDateTime;

public record TaskArchiveResult(
        String taskId,
        String title,
        String markdownDownloadUrl,
        String pdfDownloadUrl,
        LocalDateTime archivedAt
) {
}
