-- V1.4: 修复 biz_process_param.area_ratio DECIMAL(5,2) 溢出问题
-- area_ratio 实际存储的是 estimateWeight（预估重量 kg），不是百分比
-- 当单件成品重量 >= 1000kg（1吨）时，DECIMAL(5,2) 最大只能存 999.99，会溢出
-- 扩宽为 DECIMAL(10,3) 以支持最大 9,999,999.999 kg
ALTER TABLE biz_process_param
    MODIFY area_ratio DECIMAL(10,3) DEFAULT NULL COMMENT '面积/重量分摊占比%';
