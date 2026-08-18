package com.paper.mes.ai.process.credential;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
class AiProviderSecretRepository {

    private final JdbcTemplate jdbcTemplate;

    Optional<AiProviderSecretRow> find(String provider) {
        List<AiProviderSecretRow> rows = jdbcTemplate.query("""
                SELECT provider, api_key_ciphertext, api_key_last_four, enabled,
                       updated_by, updated_at
                FROM sys_ai_provider_secret
                WHERE provider = ?
                """, (resultSet, rowNumber) -> new AiProviderSecretRow(
                resultSet.getString("provider"), resultSet.getString("api_key_ciphertext"),
                resultSet.getString("api_key_last_four"), resultSet.getBoolean("enabled"),
                resultSet.getString("updated_by"),
                resultSet.getTimestamp("updated_at").toLocalDateTime()), provider);
        return rows.stream().findFirst();
    }

    int upsert(AiProviderSecretRow row) {
        return jdbcTemplate.update("""
                INSERT INTO sys_ai_provider_secret
                  (provider, api_key_ciphertext, api_key_last_four, enabled, updated_by)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                  api_key_ciphertext = VALUES(api_key_ciphertext),
                  api_key_last_four = VALUES(api_key_last_four),
                  enabled = VALUES(enabled), updated_by = VALUES(updated_by)
                """, row.provider(), row.ciphertext(), row.lastFour(),
                row.enabled(), row.updatedBy());
    }

    int delete(String provider) {
        return jdbcTemplate.update(
                "DELETE FROM sys_ai_provider_secret WHERE provider = ?", provider);
    }
}
