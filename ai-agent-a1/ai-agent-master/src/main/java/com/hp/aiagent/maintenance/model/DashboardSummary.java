package com.hp.aiagent.maintenance.model;

import java.util.List;
import java.util.Map;

public record DashboardSummary(
        int deviceCount,
        int warningCount,
        int openTaskCount,
        int knowledgeCount,
        Map<String, Integer> riskDistribution,
        List<MaintenanceTask> recentTasks
) {
}
