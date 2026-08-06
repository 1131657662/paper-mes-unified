package com.paper.mes.common;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class SqlErrorClassifierTest {

    @Test
    void recognizesMissingTableThroughNestedCause() {
        RuntimeException wrapped = new RuntimeException(
                new SQLException("missing table", "42S02", 1146));

        assertThat(SqlErrorClassifier.isMissingStructure(wrapped)).isTrue();
    }

    @Test
    void doesNotClassifySyntaxErrorAsMissingSchema() {
        SQLException syntaxError = new SQLException("syntax", "42000", 1064);

        assertThat(SqlErrorClassifier.isMissingStructure(syntaxError)).isFalse();
    }
}
