package com.paper.mes.processorder.service;

import com.paper.mes.processorder.statemachine.FinishRollNoGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RollNoSequenceService {

    private static final String SEQUENCE_KEY = "finish_roll";

    private final JdbcTemplate jdbcTemplate;

    @Transactional(rollbackFor = Exception.class)
    public String nextFinishRollNo() {
        ensureSequenceRow();
        long current = lockCurrentValue();
        long next = Math.max(current, maxPersistedSequence()) + 1;
        jdbcTemplate.update(
                "UPDATE sys_roll_no_sequence SET current_value = ? WHERE sequence_key = ?",
                next, SEQUENCE_KEY);
        return FinishRollNoGenerator.encode(next);
    }

    private void ensureSequenceRow() {
        jdbcTemplate.update("""
                INSERT INTO sys_roll_no_sequence(sequence_key, current_value)
                VALUES (?, 0)
                ON DUPLICATE KEY UPDATE sequence_key = sequence_key
                """, SEQUENCE_KEY);
    }

    private long lockCurrentValue() {
        Long current = jdbcTemplate.queryForObject("""
                SELECT current_value
                FROM sys_roll_no_sequence
                WHERE sequence_key = ?
                FOR UPDATE
                """, Long.class, SEQUENCE_KEY);
        return current == null ? 0L : current;
    }

    private long maxPersistedSequence() {
        List<String> rollNos = jdbcTemplate.queryForList("""
                SELECT finish_roll_no
                FROM biz_finish_roll
                WHERE finish_roll_no REGEXP '^[ABCDEFGHJKMNPQRSTUVWXY][0-9]{6}$'
                ORDER BY finish_roll_no DESC
                LIMIT 1
                """, String.class);
        if (rollNos.isEmpty()) {
            return 0L;
        }
        return FinishRollNoGenerator.decode(rollNos.get(0));
    }
}
