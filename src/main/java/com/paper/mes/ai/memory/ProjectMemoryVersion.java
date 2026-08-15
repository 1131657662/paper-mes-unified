package com.paper.mes.ai.memory;

import java.util.regex.Pattern;

final class ProjectMemoryVersion {

    private static final Pattern VERSION = Pattern.compile("\\d+\\.\\d+\\.\\d+");

    private ProjectMemoryVersion() {
    }

    static String next(String current) {
        if (current == null || !VERSION.matcher(current).matches()) {
            throw new IllegalArgumentException("MEMORY_VERSION_INVALID");
        }
        String[] parts = current.split("\\.");
        long patch = Long.parseLong(parts[2]);
        if (patch == Integer.MAX_VALUE) throw new IllegalArgumentException("MEMORY_VERSION_OVERFLOW");
        return parts[0] + "." + parts[1] + "." + (patch + 1);
    }
}
