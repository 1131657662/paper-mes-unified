package com.paper.mes.system.config.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.system.config.constant.NoRuleBizType;
import com.paper.mes.system.config.entity.SysNoRule;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentNoServiceTest {

    private static final LocalDate BIZ_DATE = LocalDate.of(2026, 7, 1);

    @Test
    void next_whenPrefixDateSerialRule_formatsWithDateAndSerial() {
        DocumentNoService service = service(rule(NoRuleBizType.PROCESS_ORDER, "GD", 1, 4, 1));

        String no = service.next(NoRuleBizType.PROCESS_ORDER, BIZ_DATE);

        assertEquals("GD202607010001", no);
    }

    @Test
    void next_whenPrefixSerialRule_formatsWithoutDate() {
        DocumentNoService service = service(rule(NoRuleBizType.CUSTOMER, "KH", 2, 6, 0));

        String no = service.next(NoRuleBizType.CUSTOMER, BIZ_DATE);

        assertEquals("KH000001", no);
    }

    @Test
    void sequenceKey_whenResetCycleDiffers_matchesConfiguredScope() {
        DocumentNoService service = service(rule(NoRuleBizType.DELIVERY_ORDER, "CK", 1, 4, 1));

        assertEquals("delivery_order", service.sequenceKey(rule(NoRuleBizType.DELIVERY_ORDER, "CK", 1, 4, 0), BIZ_DATE));
        assertEquals("delivery_order:20260701", service.sequenceKey(rule(NoRuleBizType.DELIVERY_ORDER, "CK", 1, 4, 1), BIZ_DATE));
        assertEquals("delivery_order:202607", service.sequenceKey(rule(NoRuleBizType.DELIVERY_ORDER, "CK", 1, 4, 2), BIZ_DATE));
        assertEquals("delivery_order:2026", service.sequenceKey(rule(NoRuleBizType.DELIVERY_ORDER, "CK", 1, 4, 3), BIZ_DATE));
    }

    @Test
    void next_whenPersistedNoIsGreater_continuesAfterPersistedMax() {
        FakeJdbcTemplate jdbcTemplate = new FakeJdbcTemplate();
        jdbcTemplate.setMaxPersistedValue(18L);
        DocumentNoService service = service(rule(NoRuleBizType.SETTLE_ORDER, "JS", 1, 4, 1), jdbcTemplate);

        String no = service.next(NoRuleBizType.SETTLE_ORDER, BIZ_DATE);

        assertEquals("JS202607010019", no);
        assertEquals(19L, jdbcTemplate.current("settle_order:20260701"));
    }

    @Test
    void nextPreviewValue_whenPersistedNoIsGreater_usesPersistedMax() {
        FakeJdbcTemplate jdbcTemplate = new FakeJdbcTemplate();
        jdbcTemplate.setMaxPersistedValue(18L);
        SysNoRule rule = rule(NoRuleBizType.SETTLE_ORDER, "JS", 1, 4, 1);
        DocumentNoService service = service(rule, jdbcTemplate);

        long next = service.nextPreviewValue(rule, BIZ_DATE);

        assertEquals(19L, next);
        assertEquals("JS202607010019", service.preview(rule, BIZ_DATE, next));
    }

    @Test
    void next_whenCalledConcurrently_returnsUniqueIncreasingNumbers() throws Exception {
        DocumentNoService service = service(rule(NoRuleBizType.FINISH_ROLL, "A", 2, 6, 0));
        ExecutorService executor = Executors.newFixedThreadPool(6);
        List<Future<String>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < 12; i++) {
                futures.add(executor.submit(() -> service.next(NoRuleBizType.FINISH_ROLL, BIZ_DATE)));
            }
        } finally {
            executor.shutdown();
        }

        List<String> nos = new ArrayList<>();
        for (Future<String> future : futures) {
            nos.add(future.get());
        }

        assertEquals(12, nos.stream().distinct().count());
        assertTrue(nos.contains("A000001"));
        assertTrue(nos.contains("A000012"));
    }

    @Test
    void preview_whenFinishRollSequenceOverflowsLetter_carriesToNextAllowedLetter() {
        SysNoRule rule = rule(NoRuleBizType.FINISH_ROLL, "A", 2, 6, 0);
        DocumentNoService service = service(rule);

        assertEquals("A000001", service.preview(rule, BIZ_DATE, 1L));
        assertEquals("A999999", service.preview(rule, BIZ_DATE, 999_999L));
        assertEquals("B000001", service.preview(rule, BIZ_DATE, 1_000_000L));
    }

    @Test
    void preview_whenFinishRollLetterReachesExcludedLetters_skipsIOLZ() {
        DocumentNoService service = service(rule(NoRuleBizType.FINISH_ROLL, "A", 2, 6, 0));

        assertEquals("J000001", service.preview(rule(NoRuleBizType.FINISH_ROLL, "H", 2, 6, 0),
                BIZ_DATE, 1_000_000L));
        assertEquals("M000001", service.preview(rule(NoRuleBizType.FINISH_ROLL, "K", 2, 6, 0),
                BIZ_DATE, 1_000_000L));
        assertEquals("P000001", service.preview(rule(NoRuleBizType.FINISH_ROLL, "N", 2, 6, 0),
                BIZ_DATE, 1_000_000L));
    }

    @Test
    void preview_whenFinishRollLetterRangeExhausted_throwsBusinessException() {
        SysNoRule rule = rule(NoRuleBizType.FINISH_ROLL, "Y", 2, 6, 0);
        DocumentNoService service = service(rule);

        assertThrows(BusinessException.class, () -> service.preview(rule, BIZ_DATE, 1_000_000L));
    }

    @Test
    void next_whenPersistedFinishRollReachedLetterLimit_continuesWithNextAllowedLetter() {
        FakeJdbcTemplate jdbcTemplate = new FakeJdbcTemplate();
        jdbcTemplate.setPersistedFinishRollNos(List.of("A999999"));
        DocumentNoService service = service(rule(NoRuleBizType.FINISH_ROLL, "A", 2, 6, 0), jdbcTemplate);

        String no = service.next(NoRuleBizType.FINISH_ROLL, BIZ_DATE);

        assertEquals("B000001", no);
        assertEquals(1_000_000L, jdbcTemplate.current(NoRuleBizType.FINISH_ROLL));
    }

    private DocumentNoService service(SysNoRule rule) {
        return service(rule, new FakeJdbcTemplate());
    }

    private DocumentNoService service(SysNoRule rule, JdbcTemplate jdbcTemplate) {
        NoRuleService noRuleService = mock(NoRuleService.class);
        when(noRuleService.activeRule(anyString())).thenReturn(rule);
        return new DocumentNoService(noRuleService, jdbcTemplate);
    }

    private static SysNoRule rule(String bizType, String prefix, int patternType, int serialLength, int resetCycle) {
        SysNoRule rule = new SysNoRule();
        rule.setBizType(bizType);
        rule.setPrefix(prefix);
        rule.setPatternType(patternType);
        rule.setDatePattern("yyyyMMdd");
        rule.setSerialLength(serialLength);
        rule.setResetCycle(resetCycle);
        return rule;
    }

    private static class FakeJdbcTemplate extends JdbcTemplate {
        private final Map<String, Long> sequences = new ConcurrentHashMap<>();
        private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();
        private final ThreadLocal<String> lockedKey = new ThreadLocal<>();
        private List<String> persistedFinishRollNos = List.of();
        private long maxPersistedValue;

        void setMaxPersistedValue(long maxPersistedValue) {
            this.maxPersistedValue = maxPersistedValue;
        }

        void setPersistedFinishRollNos(List<String> persistedFinishRollNos) {
            this.persistedFinishRollNos = persistedFinishRollNos;
        }

        long current(String key) {
            return sequences.getOrDefault(key, 0L);
        }

        @Override
        public int update(String sql, Object... args) {
            if (sql.contains("INSERT INTO sys_roll_no_sequence")) {
                sequences.putIfAbsent((String) args[0], 0L);
                return 1;
            }
            if (sql.contains("UPDATE sys_roll_no_sequence")) {
                String key = (String) args[1];
                sequences.put(key, ((Number) args[0]).longValue());
                unlockIfHeld(key);
                return 1;
            }
            throw new UnsupportedOperationException(sql);
        }

        @Override
        public <T> List<T> queryForList(String sql, Class<T> elementType, Object... args) {
            if (sql.contains("FROM biz_finish_roll")) {
                return persistedFinishRollNos.stream().map(elementType::cast).toList();
            }
            Long value = sequences.get((String) args[0]);
            if (value == null) {
                return List.of();
            }
            return List.of(elementType.cast(value));
        }

        @Override
        public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) {
            Long value;
            if (sql.contains("FROM sys_roll_no_sequence")) {
                String key = (String) args[0];
                if (sql.contains("FOR UPDATE")) {
                    lock(key);
                }
                value = sequences.getOrDefault(key, 0L);
            } else {
                value = maxPersistedValue;
            }
            return requiredType.cast(value);
        }

        private void lock(String key) {
            ReentrantLock lock = locks.computeIfAbsent(key, ignored -> new ReentrantLock());
            lock.lock();
            lockedKey.set(key);
        }

        private void unlockIfHeld(String key) {
            if (!key.equals(lockedKey.get())) {
                return;
            }
            lockedKey.remove();
            locks.get(key).unlock();
        }
    }
}
