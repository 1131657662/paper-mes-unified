package com.paper.mes.system.config.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Order(32)
public class ProcessRouteIntegrityBootstrap implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        createStageInputRelTable();
    }

    private void createStageInputRelTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `biz_process_stage_input_rel` (
                  `uuid`              VARCHAR(36)   NOT NULL                COMMENT '阶段输入关联主键',
                  `order_uuid`        VARCHAR(36)   NOT NULL                COMMENT '关联加工单',
                  `original_uuid`     VARCHAR(36)   NOT NULL                COMMENT '根母卷UUID',
                  `step_uuid`         VARCHAR(36)   NOT NULL                COMMENT '消费这些阶段产出的工序UUID',
                  `input_output_uuid` VARCHAR(36)   NOT NULL                COMMENT '被消费的阶段产出UUID',
                  `source_step_uuid`  VARCHAR(36)   DEFAULT NULL            COMMENT '阶段产出来源工序UUID',
                  `input_sort`        INT           NOT NULL DEFAULT 1      COMMENT '本工序输入顺序',
                  `stage_level`       INT           NOT NULL DEFAULT 1      COMMENT '消费工序所在阶段层级',
                  `is_deleted`        TINYINT       NOT NULL DEFAULT 0      COMMENT '0正常 1删除',
                  `create_by`         VARCHAR(50)   DEFAULT NULL            COMMENT '创建人',
                  `update_by`         VARCHAR(50)   DEFAULT NULL            COMMENT '更新人',
                  `create_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                  `update_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                  `version`           INT           NOT NULL DEFAULT 1      COMMENT '乐观锁版本号',
                  `ext_str1`          VARCHAR(255)  DEFAULT NULL            COMMENT '扩展文本1',
                  `ext_str2`          VARCHAR(255)  DEFAULT NULL            COMMENT '扩展文本2',
                  `ext_num1`          DECIMAL(12,3) DEFAULT NULL            COMMENT '扩展数值1',
                  `ext_num2`          DECIMAL(12,3) DEFAULT NULL            COMMENT '扩展数值2',
                  PRIMARY KEY (`uuid`),
                  KEY `idx_order_uuid` (`order_uuid`),
                  KEY `idx_original_uuid` (`original_uuid`),
                  KEY `idx_step_uuid` (`step_uuid`),
                  KEY `idx_input_output_uuid` (`input_output_uuid`),
                  KEY `idx_source_step_uuid` (`source_step_uuid`),
                  KEY `idx_is_deleted` (`is_deleted`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工艺阶段输入关联表'
                """);
    }
}
