-- V1.4: widen biz_process_param.area_ratio.
-- Historical note: area_ratio stores estimateWeight in kg, not a percentage.
ALTER TABLE biz_process_param
    MODIFY area_ratio DECIMAL(10,3) DEFAULT NULL COMMENT '历史字段：预估重量kg，不再按百分比展示';
