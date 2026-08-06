package com.paper.mes.common;

import java.sql.SQLException;
import java.util.Set;

final class SqlErrorClassifier {

    private static final Set<Integer> SCHEMA_ERROR_CODES = Set.of(1054, 1146);
    private static final Set<String> SCHEMA_SQL_STATES = Set.of("42S02", "42S22");

    private SqlErrorClassifier() {
    }

    static boolean isMissingStructure(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof SQLException sql && isMissingStructure(sql)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static boolean isMissingStructure(SQLException error) {
        return SCHEMA_ERROR_CODES.contains(error.getErrorCode())
                || SCHEMA_SQL_STATES.contains(error.getSQLState());
    }
}
