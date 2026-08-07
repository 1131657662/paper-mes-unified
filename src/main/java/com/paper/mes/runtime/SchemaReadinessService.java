package com.paper.mes.runtime;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SchemaReadinessService {

    private static final String MIGRATION_TABLE = "sys_schema_migration";

    private final JdbcTemplate jdbcTemplate;

    @Value("${app.schema-readiness.expected-version:3.63}")
    private String expectedVersion;

    @Value("${app.schema-readiness.require-migration-history:false}")
    private boolean requireMigrationHistory;

    private volatile SchemaReadinessReport current;

    public SchemaReadinessReport current() {
        SchemaReadinessReport report = current;
        return report == null ? refresh() : report;
    }

    public synchronized SchemaReadinessReport refresh() {
        try {
            current = inspectDatabase();
        } catch (DataAccessException exception) {
            current = unavailableDatabase(exception);
        }
        return current;
    }

    private SchemaReadinessReport inspectDatabase() {
        List<String> missing = new ArrayList<>();
        for (SchemaRequirement requirement : SchemaRequirementCatalog.criticalStructures()) {
            if (!exists(requirement)) {
                missing.add(requirement.label());
            }
        }
        String databaseVersion = databaseVersion();
        if (requireMigrationHistory && !expectedVersion.equals(databaseVersion)) {
            missing.add("migration:expected=" + expectedVersion + ",actual=" + databaseVersion);
        }
        return new SchemaReadinessReport(missing.isEmpty(), databaseVersion,
                expectedVersion, List.copyOf(missing), Instant.now());
    }

    private boolean exists(SchemaRequirement requirement) {
        return switch (requirement.kind()) {
            case TABLE -> count(TABLE_SQL, requirement.table()) > 0;
            case COLUMN -> count(COLUMN_SQL, requirement.table(), requirement.name()) > 0;
            case INDEX -> count(INDEX_SQL, requirement.table(), requirement.name()) > 0;
            case CONSTRAINT -> count(CONSTRAINT_SQL, requirement.table(), requirement.name()) > 0;
            case TRIGGER -> count(TRIGGER_SQL, requirement.name()) > 0;
        };
    }

    private int count(String sql, Object... arguments) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class, arguments);
        return value == null ? 0 : value;
    }

    private String databaseVersion() {
        if (count(TABLE_SQL, MIGRATION_TABLE) == 0) {
            return "UNTRACKED";
        }
        List<String> versions = jdbcTemplate.queryForList("""
                SELECT version FROM sys_schema_migration
                WHERE status = 'applied' ORDER BY executed_at DESC LIMIT 1
                """, String.class);
        return versions.isEmpty() ? "UNTRACKED" : versions.getFirst();
    }

    private SchemaReadinessReport unavailableDatabase(DataAccessException exception) {
        String reason = "database:" + exception.getClass().getSimpleName();
        return new SchemaReadinessReport(false, "UNAVAILABLE", expectedVersion,
                List.of(reason), Instant.now());
    }

    private static final String TABLE_SQL = """
            SELECT COUNT(*) FROM information_schema.tables
            WHERE table_schema = DATABASE() AND table_name = ?
            """;
    private static final String COLUMN_SQL = """
            SELECT COUNT(*) FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?
            """;
    private static final String INDEX_SQL = """
            SELECT COUNT(*) FROM information_schema.statistics
            WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?
            """;
    private static final String CONSTRAINT_SQL = """
            SELECT COUNT(*) FROM information_schema.table_constraints
            WHERE constraint_schema = DATABASE() AND table_name = ? AND constraint_name = ?
            """;
    private static final String TRIGGER_SQL = """
            SELECT COUNT(*) FROM information_schema.triggers
            WHERE trigger_schema = DATABASE() AND trigger_name = ?
            """;
}
