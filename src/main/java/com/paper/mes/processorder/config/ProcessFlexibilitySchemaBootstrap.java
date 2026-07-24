package com.paper.mes.processorder.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Order(36)
@ConditionalOnProperty(prefix = "app.schema-bootstrap", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class ProcessFlexibilitySchemaBootstrap implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        addColumn("biz_finish_roll", "customer_finish_width", """
                ALTER TABLE `biz_finish_roll` ADD COLUMN `customer_finish_width` INT DEFAULT NULL
                COMMENT '客户销售门幅 mm' AFTER `finish_width`
                """);
        addColumn("biz_finish_roll", "customer_spec_override_reason", """
                ALTER TABLE `biz_finish_roll` ADD COLUMN `customer_spec_override_reason` VARCHAR(255) DEFAULT NULL
                COMMENT '客户规格改写原因' AFTER `customer_finish_width`
                """);
        addColumn("biz_finish_roll", "customer_spec_override_by", """
                ALTER TABLE `biz_finish_roll` ADD COLUMN `customer_spec_override_by` VARCHAR(50) DEFAULT NULL
                COMMENT '客户规格改写人' AFTER `customer_spec_override_reason`
                """);
        addColumn("biz_finish_roll", "customer_spec_override_at", """
                ALTER TABLE `biz_finish_roll` ADD COLUMN `customer_spec_override_at` DATETIME DEFAULT NULL
                COMMENT '客户规格改写时间' AFTER `customer_spec_override_by`
                """);
        addColumn("biz_process_step", "width_difference_policy", """
                ALTER TABLE `biz_process_step` ADD COLUMN `width_difference_policy` VARCHAR(16) DEFAULT NULL
                COMMENT 'LOSS计损耗 ALLOCATE分摊 REMAINDER留余料' AFTER `pricing_adjustment_batch_id`
                """);
        addColumn("biz_process_step", "planned_loss_width", """
                ALTER TABLE `biz_process_step` ADD COLUMN `planned_loss_width` INT DEFAULT NULL
                COMMENT '计划非库存损耗门幅 mm' AFTER `width_difference_policy`
                """);
        addColumn("biz_process_step", "planned_loss_weight", """
                ALTER TABLE `biz_process_step` ADD COLUMN `planned_loss_weight` DECIMAL(10,3) DEFAULT NULL
                COMMENT '计划非库存损耗重量 kg' AFTER `planned_loss_width`
                """);
        addColumn("biz_process_step", "billing_basis", """
                ALTER TABLE `biz_process_step` ADD COLUMN `billing_basis` VARCHAR(16) DEFAULT NULL
                COMMENT '服务计费基准 TON按吨 PIECE按件' AFTER `process_weight`
                """);
        addColumn("biz_process_step", "service_quantity", """
                ALTER TABLE `biz_process_step` ADD COLUMN `service_quantity` DECIMAL(12,3) DEFAULT NULL
                COMMENT '整理或包装服务数量' AFTER `billing_basis`
                """);
    }

    private void addColumn(String table, String column, String ddl) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?
                """, Integer.class, table, column);
        if (count == null || count == 0) {
            jdbcTemplate.execute(ddl);
        }
    }
}
