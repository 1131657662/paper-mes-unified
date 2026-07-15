package com.paper.mes.backup.config;

import com.paper.mes.backup.service.BackupTaskHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(100)
public class BackupTaskRecovery implements ApplicationRunner {

    private final BackupTaskHistoryService historyService;

    @Override
    public void run(ApplicationArguments args) {
        int recovered = historyService.recoverInterruptedTasks();
        if (recovered > 0) {
            log.warn("Recovered {} interrupted backup task(s) after service restart", recovered);
        }
    }
}
