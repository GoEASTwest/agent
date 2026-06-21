package com.hp.aiagent.maintenance.model;

import java.time.LocalDateTime;
import java.util.List;

public record ReportResult(
        String reportId,
        String title,
        String riskLevel,
        List<String> sections,
        String markdown,
        LocalDateTime generatedAt,
        String markdownDownloadUrl,
        String pdfDownloadUrl
) {
}
