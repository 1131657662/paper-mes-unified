package com.paper.mes.runtime;

record SchemaRequirement(Kind kind, String table, String name) {

    enum Kind {
        TABLE,
        COLUMN,
        INDEX,
        CONSTRAINT,
        TRIGGER
    }

    String label() {
        return switch (kind) {
            case TABLE -> "table:" + table;
            case COLUMN -> "column:" + table + "." + name;
            case INDEX -> "index:" + table + "." + name;
            case CONSTRAINT -> "constraint:" + table + "." + name;
            case TRIGGER -> "trigger:" + name;
        };
    }
}
