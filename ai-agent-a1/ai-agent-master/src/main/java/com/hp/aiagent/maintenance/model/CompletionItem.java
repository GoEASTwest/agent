package com.hp.aiagent.maintenance.model;

public record CompletionItem(
        String module,
        String status,
        int percent,
        String result,
        String nextStep
) {
}
