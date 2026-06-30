-- 仪表盘收入曲线演示数据：补近 12 个月中历史月份的加工收入。
-- 用临时表统一生成轻量加工单、原纸、成品汇总数据；可重复执行。

SET NAMES utf8mb4;

SET @customer_a = 'demo-customer-a-uuid';
SET @customer_b = 'demo-customer-b-uuid';
SET @warehouse = 'demo-warehouse-uuid';

DELETE FROM biz_finish_roll WHERE order_uuid LIKE 'demo-trend-order-%';
DELETE FROM biz_original_roll WHERE order_uuid LIKE 'demo-trend-order-%';
DELETE FROM biz_process_order WHERE uuid LIKE 'demo-trend-order-%';

DROP TEMPORARY TABLE IF EXISTS tmp_dashboard_trend_demo;
CREATE TEMPORARY TABLE tmp_dashboard_trend_demo (
  ym CHAR(6) NOT NULL PRIMARY KEY,
  order_date DATE NOT NULL,
  customer_uuid VARCHAR(36) NOT NULL,
  customer_name VARCHAR(100) NOT NULL,
  is_invoice TINYINT NOT NULL,
  settle_type TINYINT NOT NULL,
  tax_rate DECIMAL(5,2) NOT NULL,
  process_amount DECIMAL(12,2) NOT NULL,
  extra_amount DECIMAL(12,2) NOT NULL,
  tax_amount DECIMAL(12,2) NOT NULL,
  total_amount DECIMAL(12,2) NOT NULL,
  original_weight DECIMAL(10,3) NOT NULL,
  finish_weight DECIMAL(10,3) NOT NULL,
  step_count INT NOT NULL,
  knife_count INT NOT NULL,
  is_mix_process TINYINT NOT NULL,
  remark VARCHAR(255) NOT NULL
) ENGINE=MEMORY DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO tmp_dashboard_trend_demo
(ym, order_date, customer_uuid, customer_name, is_invoice, settle_type, tax_rate, process_amount, extra_amount,
 tax_amount, total_amount, original_weight, finish_weight, step_count, knife_count, is_mix_process, remark)
VALUES
('202507', '2025-07-18', @customer_a, '演示客户A-华东纸业', 1, 2, 13.00, 920.00, 60.00, 127.40, 1107.40, 7200.000, 7040.000, 3, 8, 1, '仪表盘趋势演示：7月'),
('202508', '2025-08-16', @customer_b, '演示客户B-南方包装', 2, 1, 0.00, 1280.00, 45.00, 0.00, 1325.00, 9100.000, 8880.000, 4, 0, 0, '仪表盘趋势演示：8月'),
('202509', '2025-09-20', @customer_a, '演示客户A-华东纸业', 1, 2, 13.00, 1660.00, 150.00, 235.30, 2045.30, 11800.000, 11520.000, 5, 12, 1, '仪表盘趋势演示：9月旺季'),
('202510', '2025-10-14', @customer_b, '演示客户B-南方包装', 2, 1, 0.00, 1185.00, 30.00, 0.00, 1215.00, 8600.000, 8420.000, 4, 0, 0, '仪表盘趋势演示：10月'),
('202511', '2025-11-21', @customer_a, '演示客户A-华东纸业', 1, 2, 13.00, 2100.00, 130.00, 289.90, 2519.90, 14200.000, 13860.000, 6, 14, 1, '仪表盘趋势演示：11月高峰'),
('202512', '2025-12-19', @customer_b, '演示客户B-南方包装', 2, 1, 0.00, 1850.00, 130.00, 0.00, 1980.00, 13200.000, 12860.000, 5, 0, 0, '仪表盘趋势演示：12月'),
('202602', '2026-02-23', @customer_a, '演示客户A-华东纸业', 1, 2, 13.00, 760.00, 50.00, 105.30, 915.30, 5600.000, 5460.000, 3, 6, 1, '仪表盘趋势演示：春节后恢复'),
('202604', '2026-04-17', @customer_b, '演示客户B-南方包装', 2, 1, 0.00, 1450.00, 35.00, 0.00, 1485.00, 10200.000, 9980.000, 4, 0, 0, '仪表盘趋势演示：4月');

INSERT INTO biz_process_order
(uuid, order_no, customer_uuid, customer_name, order_date, priority, warehouse_uuid, team_group, is_invoice,
 settle_type, settle_day, tax_rate, urgent_fee, pallet_fee, loading_fee, freight_fee, other_fee,
 process_amount_no_tax, process_amount_tax, extra_amount_no_tax, extra_amount_tax, total_amount_no_tax,
 total_amount_tax, total_process_amount, total_extra_amount, total_amount, total_original_weight,
 total_original_ton, total_finish_weight, total_step_count, has_extra_step, actual_total_knife,
 order_status, print_status, print_count, last_print_time, last_print_user, back_record_time,
 back_record_user, is_mix_process, remark, create_by, update_by, create_time, update_time)
SELECT CONCAT('demo-trend-order-', ym),
       CONCAT('DEMO-TREND-', ym),
       customer_uuid,
       customer_name,
       order_date,
       IF(ym IN ('202509', '202512'), 2, 1),
       @warehouse,
       IF(customer_uuid = @customer_a, 'A班', 'B班'),
       is_invoice,
       settle_type,
       IF(settle_type = 2, 25, NULL),
       tax_rate,
       IF(ym IN ('202509', '202512'), 60.00, 0.00),
       IF(ym IN ('202510', '202511'), 30.00, 0.00),
       IF(ym IN ('202507', '202509', '202511', '202602'), extra_amount, 0.00),
       IF(ym IN ('202508', '202512', '202604'), extra_amount, 0.00),
       0.00,
       process_amount,
       IF(tax_rate = 0, 0.00, ROUND(process_amount * tax_rate / 100, 2)),
       extra_amount,
       IF(tax_rate = 0, 0.00, ROUND(extra_amount * tax_rate / 100, 2)),
       process_amount + extra_amount,
       tax_amount,
       process_amount,
       extra_amount,
       total_amount,
       original_weight,
       ROUND(original_weight / 1000, 3),
       finish_weight,
       step_count,
       IF(is_mix_process = 1, 1, 0),
       knife_count,
       5,
       1,
       1,
       CONCAT(order_date, ' 09:20:00'),
       '系统管理员',
       CONCAT(order_date, ' 17:30:00'),
       '系统管理员',
       is_mix_process,
       remark,
       'system',
       'system',
       CONCAT(order_date, ' 08:40:00'),
       CONCAT(order_date, ' 17:30:00')
FROM tmp_dashboard_trend_demo;

INSERT INTO biz_original_roll
(uuid, order_uuid, row_sort, roll_no, paper_name, gram_weight, original_width, original_diameter, core_diameter,
 roll_weight, actual_weight, piece_num, total_weight, process_mode, main_step_type, roll_status, process_amount,
 total_loss_weight, total_loss_ratio, customer_name, order_no, remark, create_by, update_by, create_time, update_time)
SELECT CONCAT('demo-trend-orig-', ym),
       CONCAT('demo-trend-order-', ym),
       1,
       CONCAT('TREND-MW-', ym),
       IF(customer_uuid = @customer_a, '白卡', '牛卡'),
       IF(customer_uuid = @customer_a, 300, 200),
       IF(customer_uuid = @customer_a, 2520, 2000),
       1300,
       76,
       original_weight,
       original_weight,
       1,
       original_weight,
       1,
       IF(is_mix_process = 1, 1, 2),
       3,
       process_amount,
       ROUND(original_weight - finish_weight, 3),
       ROUND((original_weight - finish_weight) / original_weight * 100, 2),
       customer_name,
       CONCAT('DEMO-TREND-', ym),
       '仪表盘趋势演示原纸',
       'system',
       'system',
       CONCAT(order_date, ' 08:40:00'),
       CONCAT(order_date, ' 17:30:00')
FROM tmp_dashboard_trend_demo;

INSERT INTO biz_finish_roll
(uuid, order_uuid, row_sort, finish_roll_no, roll_no_status, is_spare, paper_name, gram_weight, finish_width,
 finish_diameter, finish_core_diameter, source_type, estimate_weight, actual_weight, trim_width_share,
 trim_weight_share, finish_status, warehouse_uuid, original_roll_nos, actual_remark, remark, create_by,
 update_by, create_time, update_time)
SELECT CONCAT('demo-trend-fin-', ym),
       CONCAT('demo-trend-order-', ym),
       1,
       CONCAT('T', SUBSTRING(ym, 3), '001'),
       2,
       0,
       IF(customer_uuid = @customer_a, '白卡', '牛卡'),
       IF(customer_uuid = @customer_a, 300, 200),
       IF(customer_uuid = @customer_a, 840, 1000),
       1300,
       76,
       1,
       finish_weight,
       finish_weight,
       0,
       ROUND(original_weight - finish_weight, 3),
       3,
       @warehouse,
       CONCAT('TREND-MW-', ym),
       '仪表盘趋势演示成品',
       '趋势演示',
       'system',
       'system',
       CONCAT(order_date, ' 08:40:00'),
       CONCAT(order_date, ' 17:30:00')
FROM tmp_dashboard_trend_demo;
