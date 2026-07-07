package com.paper.mes.system.config.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Order(36)
public class OperationLogIntegrityBootstrap implements ApplicationRunner {

    private static final String TABLE = "sys_operation_log";
    private static final String REMARK_COLUMN = "remark";
    private static final String TARGET_TYPE = "text";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        widenRemarkColumn();
    }

    private void widenRemarkColumn() {
        String dataType = remarkDataType();
        if (dataType == null || TARGET_TYPE.equalsIgnoreCase(dataType)) {
            return;
        }
        jdbcTemplate.execute("""
                ALTER TABLE `sys_operation_log`
                MODIFY `remark` TEXT COMMENT 'Operation remark, including rollback and release reasons'
                """);
    }

    private String remarkDataType() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT data_type
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """, TABLE, REMARK_COLUMN);
        if (rows.isEmpty()) {
            return null;
        }
        Object dataType = rows.getFirst().get("data_type");
        return dataType == null ? null : dataType.toString();
    }
}
