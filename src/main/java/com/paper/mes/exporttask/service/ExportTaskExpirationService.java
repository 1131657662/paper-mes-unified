package com.paper.mes.exporttask.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.paper.mes.exporttask.entity.ExportTask;
import com.paper.mes.exporttask.mapper.ExportTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExportTaskExpirationService {

    private static final int STATUS_SUCCESS = 3;
    private static final int STATUS_EXPIRED = 6;

    private final ExportTaskMapper taskMapper;
    private final JdbcTemplate jdbcTemplate;

    @Transactional(rollbackFor = Exception.class)
    public int expire(String taskUuid) {
        int updated = taskMapper.update(null, new LambdaUpdateWrapper<ExportTask>()
                .eq(ExportTask::getUuid, taskUuid).eq(ExportTask::getTaskStatus, STATUS_SUCCESS)
                .set(ExportTask::getTaskStatus, STATUS_EXPIRED).set(ExportTask::getFilePath, null)
                .setSql("version = version + 1"));
        if (updated > 0) {
            jdbcTemplate.update("DELETE FROM sys_export_snapshot WHERE task_uuid = ?", taskUuid);
        }
        return updated;
    }
}
