package com.hp.aiagent.maintenance.model;

import java.util.List;

public record RoleProfile(
        String id,
        String name,
        String scene,
        List<String> permissions
) {
}
