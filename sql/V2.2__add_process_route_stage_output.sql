-- V2.2: 二段工艺路线底座。
-- 目标：表达“第二道工艺消费第一道工艺的某个/多个阶段产出”，避免中间产出误入库存和结算。

SET @input_type_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'biz_process_step'
    AND column_name = 'input_type'
);

SET @add_input_type_sql := IF(
  @input_type_exists = 0,
  'ALTER TABLE biz_process_step ADD COLUMN input_type TINYINT NOT NULL DEFAULT 1 COMMENT ''输入来源：1原纸 2上一阶段产出'' AFTER original_uuid',
  'SELECT 1'
);
PREPARE add_input_type_stmt FROM @add_input_type_sql;
EXECUTE add_input_type_stmt;
DEALLOCATE PREPARE add_input_type_stmt;

SET @input_output_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'biz_process_step'
    AND column_name = 'input_output_uuid'
);

SET @add_input_output_sql := IF(
  @input_output_exists = 0,
  'ALTER TABLE biz_process_step ADD COLUMN input_output_uuid VARCHAR(36) DEFAULT NULL COMMENT ''输入阶段产出UUID，input_type=2时使用'' AFTER input_type',
  'SELECT 1'
);
PREPARE add_input_output_stmt FROM @add_input_output_sql;
EXECUTE add_input_output_stmt;
DEALLOCATE PREPARE add_input_output_stmt;

SET @stage_level_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'biz_process_step'
    AND column_name = 'stage_level'
);

SET @add_stage_level_sql := IF(
  @stage_level_exists = 0,
  'ALTER TABLE biz_process_step ADD COLUMN stage_level INT NOT NULL DEFAULT 1 COMMENT ''工艺阶段层级：1第一道 2第二道'' AFTER input_output_uuid',
  'SELECT 1'
);
PREPARE add_stage_level_stmt FROM @add_stage_level_sql;
EXECUTE add_stage_level_stmt;
DEALLOCATE PREPARE add_stage_level_stmt;

SET @parent_step_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'biz_process_step'
    AND column_name = 'parent_step_uuid'
);

SET @add_parent_step_sql := IF(
  @parent_step_exists = 0,
  'ALTER TABLE biz_process_step ADD COLUMN parent_step_uuid VARCHAR(36) DEFAULT NULL COMMENT ''上一阶段工序UUID'' AFTER stage_level',
  'SELECT 1'
);
PREPARE add_parent_step_stmt FROM @add_parent_step_sql;
EXECUTE add_parent_step_stmt;
DEALLOCATE PREPARE add_parent_step_stmt;

CREATE TABLE IF NOT EXISTS `biz_process_stage_output` (
  `uuid`                 VARCHAR(36)   NOT NULL                COMMENT '阶段产出主键',
  `order_uuid`           VARCHAR(36)   NOT NULL                COMMENT '关联加工单',
  `original_uuid`        VARCHAR(36)   NOT NULL                COMMENT '根母卷UUID',
  `step_uuid`            VARCHAR(36)   NOT NULL                COMMENT '产出所属工序',
  `parent_output_uuid`   VARCHAR(36)   DEFAULT NULL            COMMENT '上一阶段产出UUID',
  `stage_level`          INT           NOT NULL DEFAULT 1      COMMENT '工艺阶段层级',
  `output_sort`          INT           NOT NULL DEFAULT 1      COMMENT '阶段内产出排序',
  `output_type`          TINYINT       NOT NULL DEFAULT 2      COMMENT '1中间产出 2最终产出',
  `output_status`        TINYINT       NOT NULL DEFAULT 1      COMMENT '1计划 2已被下道消耗 3已生成成品卷 4作废',
  `output_no`            VARCHAR(50)   DEFAULT NULL            COMMENT '阶段产出临时编号',
  `finish_roll_uuid`     VARCHAR(36)   DEFAULT NULL            COMMENT '最终产出生成的成品卷UUID',
  `paper_name`           VARCHAR(100)  DEFAULT NULL            COMMENT '产出品名',
  `gram_weight`          INT           DEFAULT NULL            COMMENT '产出克重 g/㎡',
  `finish_width`         INT           DEFAULT NULL            COMMENT '产出门幅 mm',
  `finish_diameter`      INT           DEFAULT NULL            COMMENT '产出直径 英寸',
  `finish_core_diameter` INT           DEFAULT NULL            COMMENT '产出纸芯 英寸',
  `estimate_weight`      DECIMAL(10,3) DEFAULT NULL            COMMENT '预估重量 kg',
  `actual_weight`        DECIMAL(10,3) DEFAULT NULL            COMMENT '实际重量 kg',
  `source_step_type`     TINYINT       DEFAULT NULL            COMMENT '产出来源工艺：1锯纸 2复卷',
  `source_summary`       VARCHAR(255)  DEFAULT NULL            COMMENT '来源摘要',
  `remark`               VARCHAR(255)  DEFAULT NULL            COMMENT '备注',
  `is_deleted`           TINYINT       NOT NULL DEFAULT 0      COMMENT '0正常 1删除',
  `create_by`            VARCHAR(50)   DEFAULT NULL            COMMENT '创建人',
  `update_by`            VARCHAR(50)   DEFAULT NULL            COMMENT '更新人',
  `create_time`          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`              INT           NOT NULL DEFAULT 1      COMMENT '乐观锁版本号',
  `ext_str1`             VARCHAR(255)  DEFAULT NULL            COMMENT '扩展文本1',
  `ext_str2`             VARCHAR(255)  DEFAULT NULL            COMMENT '扩展文本2',
  `ext_num1`             DECIMAL(12,3) DEFAULT NULL            COMMENT '扩展数值1',
  `ext_num2`             DECIMAL(12,3) DEFAULT NULL            COMMENT '扩展数值2',
  PRIMARY KEY (`uuid`),
  KEY `idx_order_uuid` (`order_uuid`),
  KEY `idx_original_uuid` (`original_uuid`),
  KEY `idx_step_uuid` (`step_uuid`),
  KEY `idx_parent_output_uuid` (`parent_output_uuid`),
  KEY `idx_finish_roll_uuid` (`finish_roll_uuid`),
  KEY `idx_output_type` (`output_type`),
  KEY `idx_output_status` (`output_status`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工艺阶段产出表';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工艺阶段输入关联表';
