package com.paper.mes.system.config.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Keeps special-pricing columns and domain checks available in development databases. */
@Component
@RequiredArgsConstructor
@Order(34)
@ConditionalOnProperty(prefix = "app.schema-bootstrap", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ProcessStepPricingBootstrap implements ApplicationRunner {

    private static final String TABLE = "biz_process_step";
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        addColumn("billing_mode", "TINYINT NOT NULL DEFAULT 1 COMMENT '1标准计价 2指定数量 3固定金额 4免收' AFTER `step_amount`");
        addColumn("standard_quantity", "DECIMAL(12,3) DEFAULT NULL COMMENT '优惠前标准计费数量' AFTER `billing_mode`");
        addColumn("billing_quantity", "DECIMAL(12,3) DEFAULT NULL COMMENT '最终计费数量' AFTER `standard_quantity`");
        addColumn("billing_amount", "DECIMAL(12,2) DEFAULT NULL COMMENT '固定金额模式最终金额' AFTER `billing_quantity`");
        addColumn("standard_step_amount", "DECIMAL(12,2) DEFAULT NULL COMMENT '优惠前标准工序金额' AFTER `billing_amount`");
        addColumn("pricing_adjustment_amount", "DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '最终金额减标准金额' AFTER `standard_step_amount`");
        addColumn("pricing_adjustment_reason", "VARCHAR(255) DEFAULT NULL COMMENT '计价调整原因' AFTER `pricing_adjustment_amount`");
        addColumn("pricing_adjusted_by", "VARCHAR(50) DEFAULT NULL COMMENT '计价调整操作人' AFTER `pricing_adjustment_reason`");
        addColumn("pricing_adjusted_at", "DATETIME DEFAULT NULL COMMENT '计价调整时间' AFTER `pricing_adjusted_by`");
        addColumn("billing_unit_price", "DECIMAL(12,4) DEFAULT NULL COMMENT '人工核定单价' AFTER `unit_price`");
        addColumn("pricing_adjustment_batch_id", "VARCHAR(64) DEFAULT NULL COMMENT '批量计价操作标识' AFTER `pricing_adjusted_at`");
        addConstraint("chk_process_step_billing_mode",
                "CHECK (`billing_mode` IN (1,2,3,4))");
        addConstraint("chk_process_step_pricing_nonnegative",
                "CHECK ((`standard_quantity` IS NULL OR `standard_quantity` >= 0) AND (`billing_quantity` IS NULL OR `billing_quantity` > 0) AND (`billing_amount` IS NULL OR `billing_amount` >= 0) AND (`billing_unit_price` IS NULL OR `billing_unit_price` > 0))");
        addConstraint("chk_process_step_billing_unit_price",
                "CHECK (`billing_unit_price` IS NULL OR `billing_unit_price` > 0)");
    }

    private void addColumn(String name, String definition) {
        if (!columnExists(name)) {
            jdbcTemplate.execute("ALTER TABLE `" + TABLE + "` ADD COLUMN `" + name + "` " + definition);
        }
    }

    private void addConstraint(String name, String definition) {
        if (!constraintExists(name)) {
            jdbcTemplate.execute("ALTER TABLE `" + TABLE + "` ADD CONSTRAINT `" + name + "` " + definition);
        }
    }

    private boolean columnExists(String name) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?
                """, Integer.class, TABLE, name);
        return count != null && count > 0;
    }

    private boolean constraintExists(String name) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.table_constraints
                WHERE constraint_schema = DATABASE() AND table_name = ? AND constraint_name = ?
                """, Integer.class, TABLE, name);
        return count != null && count > 0;
    }
}
