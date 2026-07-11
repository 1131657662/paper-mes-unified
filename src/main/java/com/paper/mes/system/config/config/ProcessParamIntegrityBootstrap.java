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
@Order(30)
public class ProcessParamIntegrityBootstrap implements ApplicationRunner {

    private static final String TABLE = "biz_process_param";
    private static final String AREA_RATIO_COLUMN = "area_ratio";
    private static final int TARGET_SCALE = 3;
    private static final int MIN_INTEGER_DIGITS = 7;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        widenAreaRatioColumn();
    }

    private void widenAreaRatioColumn() {
        ColumnDecimal decimal = areaRatioDecimal();
        if (decimal == null || !decimal.needsWidening()) {
            return;
        }
        jdbcTemplate.execute("""
                ALTER TABLE `biz_process_param`
                MODIFY `area_ratio` DECIMAL(%d,%d) DEFAULT NULL COMMENT '历史字段：预估重量kg，不再按百分比展示'
                """.formatted(decimal.nextPrecision(), TARGET_SCALE));
    }

    private ColumnDecimal areaRatioDecimal() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT numeric_precision, numeric_scale
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """, TABLE, AREA_RATIO_COLUMN);
        if (rows.isEmpty()) {
            return null;
        }
        Map<String, Object> row = rows.getFirst();
        return new ColumnDecimal(number(row.get("numeric_precision")), number(row.get("numeric_scale")));
    }

    private Integer number(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    private record ColumnDecimal(Integer precision, Integer scale) {
        boolean needsWidening() {
            return precision == null || scale == null || scale < TARGET_SCALE || integerDigits() < MIN_INTEGER_DIGITS;
        }

        int nextPrecision() {
            return MIN_INTEGER_DIGITS + TARGET_SCALE;
        }

        private int integerDigits() {
            if (precision == null || scale == null) {
                return 0;
            }
            return precision - scale;
        }
    }
}
