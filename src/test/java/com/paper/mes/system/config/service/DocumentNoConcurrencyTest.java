package com.paper.mes.system.config.service;

import com.paper.mes.system.config.constant.NoRuleBizType;
import com.paper.mes.system.config.entity.SysNoRule;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentNoConcurrencyTest {

    private static final LocalDate BIZ_DATE = LocalDate.of(2026, 7, 1);

    @ParameterizedTest
    @CsvSource({
            "process_order,JG,1,4,1,JG202607010001,JG202607010012",
            "delivery_order,CK,1,4,1,CK202607010001,CK202607010012",
            "settle_order,JS,1,4,1,JS202607010001,JS202607010012",
            "finish_roll,A,2,6,0,A000001,A000012",
            "customer,KH,2,6,0,KH000001,KH000012",
            "paper,ZZ,2,6,0,ZZ000001,ZZ000012",
            "machine,JT,2,6,0,JT000001,JT000012",
            "warehouse,CKD,2,6,0,CKD000001,CKD000012"
    })
    void next_whenBusinessDocumentsAndArchiveCodesCreatedConcurrently_returnsUniqueConfiguredNos(
            String bizType, String prefix, int patternType, int serialLength, int resetCycle,
            String firstNo, String lastNo) throws Exception {
        DocumentNoService service = service(rule(bizType, prefix, patternType, serialLength, resetCycle));

        List<String> nos = concurrentNext(service, bizType);

        assertEquals(12, nos.stream().distinct().count());
        assertTrue(nos.contains(firstNo));
        assertTrue(nos.contains(lastNo));
    }

    private List<String> concurrentNext(DocumentNoService service, String bizType) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(6);
        List<Future<String>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < 12; i++) {
                futures.add(executor.submit(() -> service.next(bizType, BIZ_DATE)));
            }
        } finally {
            executor.shutdown();
        }
        List<String> nos = new ArrayList<>(futures.size());
        for (Future<String> future : futures) {
            nos.add(future.get());
        }
        return nos;
    }

    private DocumentNoService service(SysNoRule rule) {
        NoRuleService noRuleService = mock(NoRuleService.class);
        when(noRuleService.activeRule(anyString())).thenReturn(rule);
        return new DocumentNoService(noRuleService, new LockingJdbcTemplate());
    }

    private SysNoRule rule(String bizType, String prefix, int patternType, int serialLength, int resetCycle) {
        SysNoRule rule = new SysNoRule();
        rule.setBizType(bizType);
        rule.setPrefix(prefix);
        rule.setPatternType(patternType);
        rule.setDatePattern("yyyyMMdd");
        rule.setSerialLength(serialLength);
        rule.setResetCycle(resetCycle);
        return rule;
    }

    private static class LockingJdbcTemplate extends JdbcTemplate {
        private final Map<String, Long> sequences = new ConcurrentHashMap<>();
        private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();
        private final ThreadLocal<String> lockedKey = new ThreadLocal<>();

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
        public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) {
            if (sql.contains("FROM sys_roll_no_sequence")) {
                String key = (String) args[0];
                if (sql.contains("FOR UPDATE")) {
                    lock(key);
                }
                return requiredType.cast(sequences.getOrDefault(key, 0L));
            }
            return requiredType.cast(0L);
        }

        @Override
        public <T> List<T> queryForList(String sql, Class<T> elementType, Object... args) {
            if (sql.contains("FROM biz_finish_roll")) {
                return List.of();
            }
            Long value = sequences.get((String) args[0]);
            if (value == null) {
                return List.of();
            }
            return List.of(elementType.cast(value));
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
