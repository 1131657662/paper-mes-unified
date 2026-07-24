package com.paper.mes.delivery.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Order(39)
@ConditionalOnProperty(prefix = "app.schema-bootstrap", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class DeliveryCustomerRevisionSchemaBootstrap implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        createRevisionTable();
        createRevisionItemTable();
        addWeightOperand();
    }

    private void createRevisionTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS biz_delivery_customer_revision (
                  uuid VARCHAR(36) NOT NULL, delivery_uuid VARCHAR(36) NOT NULL,
                  revision_no INT NOT NULL, request_id VARCHAR(64) NOT NULL,
                  request_hash CHAR(64) NOT NULL,
                  reason VARCHAR(255) NOT NULL, item_count INT NOT NULL,
                  customer_total_weight DECIMAL(14,3) DEFAULT NULL,
                  is_deleted TINYINT NOT NULL DEFAULT 0, create_by VARCHAR(50) DEFAULT NULL,
                  update_by VARCHAR(50) DEFAULT NULL,
                  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  version INT NOT NULL DEFAULT 1, ext_str1 VARCHAR(255) DEFAULT NULL,
                  ext_str2 VARCHAR(255) DEFAULT NULL, ext_num1 DECIMAL(12,3) DEFAULT NULL,
                  ext_num2 DECIMAL(12,3) DEFAULT NULL, PRIMARY KEY (uuid),
                  UNIQUE KEY uk_delivery_customer_revision_no (delivery_uuid,revision_no),
                  UNIQUE KEY uk_delivery_customer_revision_request (delivery_uuid,request_id),
                  KEY idx_delivery_customer_revision_history (delivery_uuid,is_deleted,revision_no),
                  CONSTRAINT fk_delivery_customer_revision_order FOREIGN KEY (delivery_uuid)
                    REFERENCES biz_delivery_order(uuid) ON DELETE RESTRICT ON UPDATE RESTRICT,
                  CONSTRAINT chk_delivery_customer_revision_no CHECK (revision_no >= 1),
                  CONSTRAINT chk_delivery_customer_revision_count CHECK (item_count >= 1),
                  CONSTRAINT chk_delivery_customer_revision_total CHECK
                    (customer_total_weight IS NULL OR customer_total_weight > 0),
                  CONSTRAINT chk_delivery_customer_revision_deleted CHECK (is_deleted IN (0,1))
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Customer-facing delivery revisions'
                """);
    }

    private void createRevisionItemTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS biz_delivery_customer_revision_item (
                  uuid VARCHAR(36) NOT NULL, revision_uuid VARCHAR(36) NOT NULL,
                  delivery_detail_uuid VARCHAR(36) NOT NULL, finish_uuid VARCHAR(36) NOT NULL,
                  physical_paper_name VARCHAR(100) NOT NULL, physical_gram_weight INT NOT NULL,
                  physical_finish_width INT NOT NULL, physical_delivery_weight DECIMAL(12,3) NOT NULL,
                  customer_paper_name VARCHAR(100) NOT NULL, customer_gram_weight INT NOT NULL,
                  customer_finish_width INT NOT NULL, customer_display_weight DECIMAL(12,3) NOT NULL,
                  calculation_mode VARCHAR(16) NOT NULL, weight_operand DECIMAL(20,6) DEFAULT NULL,
                  formula_expression VARCHAR(500) DEFAULT NULL,
                  formula_inputs JSON DEFAULT NULL, rounding_scale TINYINT NOT NULL DEFAULT 3,
                  rounding_mode VARCHAR(16) NOT NULL DEFAULT 'HALF_UP',
                  zero_policy VARCHAR(16) NOT NULL DEFAULT 'SKIP', customer_remark VARCHAR(255) DEFAULT NULL,
                  is_deleted TINYINT NOT NULL DEFAULT 0, create_by VARCHAR(50) DEFAULT NULL,
                  update_by VARCHAR(50) DEFAULT NULL,
                  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  version INT NOT NULL DEFAULT 1, ext_str1 VARCHAR(255) DEFAULT NULL,
                  ext_str2 VARCHAR(255) DEFAULT NULL, ext_num1 DECIMAL(12,3) DEFAULT NULL,
                  ext_num2 DECIMAL(12,3) DEFAULT NULL, PRIMARY KEY (uuid),
                  UNIQUE KEY uk_delivery_customer_revision_item (revision_uuid,delivery_detail_uuid),
                  KEY idx_delivery_customer_revision_item_detail (delivery_detail_uuid,revision_uuid),
                  KEY idx_delivery_customer_revision_item_finish (finish_uuid,revision_uuid),
                  CONSTRAINT fk_delivery_customer_item_revision FOREIGN KEY (revision_uuid)
                    REFERENCES biz_delivery_customer_revision(uuid) ON DELETE RESTRICT ON UPDATE RESTRICT,
                  CONSTRAINT fk_delivery_customer_item_detail FOREIGN KEY (delivery_detail_uuid)
                    REFERENCES biz_delivery_detail(uuid) ON DELETE RESTRICT ON UPDATE RESTRICT,
                  CONSTRAINT fk_delivery_customer_item_finish FOREIGN KEY (finish_uuid)
                    REFERENCES biz_finish_roll(uuid) ON DELETE RESTRICT ON UPDATE RESTRICT,
                  CONSTRAINT chk_delivery_customer_item_physical CHECK
                    (physical_gram_weight > 0 AND physical_finish_width > 0 AND physical_delivery_weight > 0),
                  CONSTRAINT chk_delivery_customer_item_customer CHECK
                    (customer_gram_weight > 0 AND customer_finish_width > 0 AND customer_display_weight > 0),
                  CONSTRAINT chk_delivery_customer_item_mode CHECK
                    (calculation_mode IN ('KEEP','FIXED','DELTA','RATIO','FORMULA','MANUAL')),
                  CONSTRAINT chk_delivery_customer_item_rounding CHECK
                    (rounding_scale BETWEEN 0 AND 3 AND rounding_mode IN ('HALF_UP','UP','DOWN')),
                  CONSTRAINT chk_delivery_customer_item_zero CHECK
                    (zero_policy IN ('SKIP','ERROR','USE_ZERO')),
                  CONSTRAINT chk_delivery_customer_item_deleted CHECK (is_deleted IN (0,1))
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Customer-facing delivery revision items'
                """);
    }

    private void addWeightOperand() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema=DATABASE() AND table_name='biz_delivery_customer_revision_item'
                  AND column_name='weight_operand'
                """, Integer.class);
        if (count == null || count == 0) {
            jdbcTemplate.execute("""
                    ALTER TABLE biz_delivery_customer_revision_item
                    ADD COLUMN weight_operand DECIMAL(20,6) DEFAULT NULL AFTER calculation_mode
                    """);
        }
    }
}
