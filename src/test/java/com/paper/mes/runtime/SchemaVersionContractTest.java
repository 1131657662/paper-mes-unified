package com.paper.mes.runtime;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaVersionContractTest {

    private static final Pattern MIGRATION = Pattern.compile("^V([0-9]+(?:\\.[0-9]+)*)__.*\\.sql$");

    @Test
    void runtimeAndDeploymentExpectTheLatestCanonicalVersion() throws IOException {
        String latest = latestMigrationVersion();

        assertThat(Files.readString(Path.of("sql/schema-baseline.version")).trim()).isEqualTo(latest);
        assertThat(Files.readString(Path.of("src/main/resources/application.yml")))
                .contains("PAPER_MES_EXPECTED_SCHEMA_VERSION:" + latest);
        assertThat(Files.readString(Path.of("src/main/resources/application-prod.example.yml")))
                .contains("PAPER_MES_EXPECTED_SCHEMA_VERSION:" + latest);
        assertThat(Files.readString(Path.of("deploy/paper-mes.env.example")))
                .contains("PAPER_MES_EXPECTED_SCHEMA_VERSION=" + latest);
    }

    private String latestMigrationVersion() throws IOException {
        try (var files = Files.list(Path.of("sql"))) {
            return files.map(path -> migrationVersion(path.getFileName().toString()))
                    .filter(version -> !version.isEmpty())
                    .max(Comparator.comparing(this::versionParts, this::compareParts))
                    .orElseThrow();
        }
    }

    private String migrationVersion(String filename) {
        Matcher matcher = MIGRATION.matcher(filename);
        return matcher.matches() ? matcher.group(1) : "";
    }

    private List<Integer> versionParts(String version) {
        return Pattern.compile("\\.").splitAsStream(version).map(Integer::valueOf).toList();
    }

    private int compareParts(List<Integer> left, List<Integer> right) {
        int size = Math.max(left.size(), right.size());
        for (int index = 0; index < size; index++) {
            int result = Integer.compare(part(left, index), part(right, index));
            if (result != 0) return result;
        }
        return 0;
    }

    private int part(List<Integer> parts, int index) {
        return index < parts.size() ? parts.get(index) : 0;
    }
}
