package com.hp.aiagent.maintenance.model;

import java.util.List;

public record ModuleCapability(
        String id,
        String name,
        String ownerRole,
        String status,
        int maturity,
        List<String> functions,
        List<String> dependencies
) {
}
