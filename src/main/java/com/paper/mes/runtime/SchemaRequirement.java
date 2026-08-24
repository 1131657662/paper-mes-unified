package com.paper.mes.runtime;

record SchemaRequirement(Kind kind, String table, String name) {

    enum Kind {
        TABLE,
        COLUMN,
        COLUMN_NULLABILITY,
        INDEX,
        INDEX_COLUMNS,
        CONSTRAINT,
        FOREIGN_KEY_DELETE_RULE,
        TRIGGER
    }

    String label() {
        return switch (kind) {
            case TABLE -> "table:" + table;
            case COLUMN -> "column:" + table + "." + name;
            case COLUMN_NULLABILITY -> "column-nullable:" + table + "." + name;
            case INDEX -> "index:" + table + "." + name;
            case INDEX_COLUMNS -> "index-columns:" + table + "." + name;
            case CONSTRAINT -> "constraint:" + table + "." + name;
            case FOREIGN_KEY_DELETE_RULE -> "foreign-key-delete-rule:" + table + "." + name;
            case TRIGGER -> "trigger:" + name;
        };
    }
}
