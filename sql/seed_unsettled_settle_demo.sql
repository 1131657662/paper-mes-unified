-- 演示数据：未结清结算单，用于验收登记收款、部分收款、撤销收款、再次收款。
-- 可重复执行：只清理 DEMO-MES-202606-C / DEMO-CK-202606-C / DEMO-JS-202606-C 这一组。

SET NAMES utf8mb4;

SET @customer_c = 'demo-customer-c-uuid';
SET @machine_saw = 'demo-machine-saw-uuid';
SET @machine_rewind = 'demo-machine-rewind-uuid';
SET @warehouse = 'demo-warehouse-uuid';
SET @order_c = 'demo-order-c-uuid';
SET @delivery_c = 'demo-delivery-c-uuid';
SET @settle_c = 'demo-settle-c-uuid';

DELETE FROM biz_receive_record WHERE settle_uuid = @settle_c;
DELETE FROM biz_settle_detail WHERE settle_uuid = @settle_c;
DELETE FROM biz_settle_order WHERE uuid = @settle_c;
DELETE FROM biz_delivery_detail WHERE delivery_uuid = @delivery_c;
DELETE FROM biz_delivery_order WHERE uuid = @delivery_c;
DELETE FROM biz_finish_original_rel WHERE order_uuid = @order_c;
DELETE FROM biz_finish_roll WHERE order_uuid = @order_c;
DELETE FROM biz_process_param WHERE order_uuid = @order_c;
DELETE FROM biz_process_step WHERE order_uuid = @order_c;
DELETE FROM biz_original_roll WHERE order_uuid = @order_c;
DELETE FROM biz_process_order WHERE uuid = @order_c;
DELETE FROM sys_operation_log WHERE biz_uuid IN (@order_c, @delivery_c, @settle_c);

INSERT INTO sys_customer
(uuid, customer_code, customer_name, contact, phone, settle_type, settle_day, saw_price, rewind_price, default_invoice, price_include_tax, tax_rate, remark)
VALUES
(@customer_c, 'DEMO-C', '演示客户C-待收款', '周经理', '13800000003', 1, NULL, 10.00, 150.00, 2, 2, 0.00, '未结清结算验收客户')
ON DUPLICATE KEY UPDATE
  customer_name = VALUES(customer_name),
  saw_price = VALUES(saw_price),
  rewind_price = VALUES(rewind_price),
  default_invoice = VALUES(default_invoice),
  tax_rate = VALUES(tax_rate);

INSERT INTO sys_machine (uuid, machine_code, machine_name, machine_type, status, remark)
VALUES
(@machine_saw, 'DEMO-SAW', '演示锯纸机', 1, 1, '演示数据'),
(@machine_rewind, 'DEMO-REWIND', '演示复卷机', 2, 1, '演示数据')
ON DUPLICATE KEY UPDATE machine_name = VALUES(machine_name), machine_type = VALUES(machine_type), status = VALUES(status);

INSERT INTO sys_warehouse (uuid, warehouse_code, warehouse_name, status, remark)
VALUES (@warehouse, 'DEMO-WH', '演示成品仓', 1, '演示数据')
ON DUPLICATE KEY UPDATE warehouse_name = VALUES(warehouse_name), status = VALUES(status);

INSERT INTO biz_process_order
(uuid, order_no, customer_uuid, customer_name, order_date, priority, warehouse_uuid, team_group, is_invoice, settle_type, settle_day,
 tax_rate, loading_fee, freight_fee, process_amount_no_tax, process_amount_tax, extra_amount_no_tax, extra_amount_tax,
 total_amount_no_tax, total_amount_tax, total_process_amount, total_extra_amount, total_amount, total_original_weight,
 total_original_ton, total_finish_weight, total_step_count, actual_total_knife, order_status, print_status, print_count,
 last_print_time, last_print_user, back_record_time, back_record_user, is_mix_process, remark)
VALUES
(@order_c, 'DEMO-MES-202606-C', @customer_c, '演示客户C-待收款', '2026-06-18', 2, @warehouse, 'C班', 2, 1, NULL,
 0.00, 60.00, 40.00, 1425.00, 0.00, 100.00, 0.00, 1525.00, 0.00, 1425.00, 100.00, 1525.00,
 6500.000, 6.500, 6320.000, 2, 8, 5, 1, 1, '2026-06-18 09:30:00', '系统管理员',
 '2026-06-18 18:10:00', '系统管理员', 1, '演示：未结清收款验收单');

INSERT INTO biz_original_roll
(uuid, order_uuid, row_sort, roll_no, paper_name, gram_weight, original_width, original_diameter, core_diameter,
 roll_weight, actual_weight, piece_num, total_weight, process_mode, main_step_type, roll_status, machine_uuid,
 operator, process_amount, total_loss_weight, total_loss_ratio, customer_name, order_no, remark)
VALUES
('demo-c-orig-1', @order_c, 1, 'C-MW-001', '白卡', 350, 2500, 1300, 76, 3000.000, 3000.000, 1, 3000.000, 1, 1, 3, @machine_saw, '陈师傅', 180.00, 42.000, 1.40, '演示客户C-待收款', 'DEMO-MES-202606-C', '锯纸 8刀'),
('demo-c-orig-2', @order_c, 2, 'C-MW-002', '白卡', 350, 2500, 1300, 76, 3500.000, 3500.000, 1, 3500.000, 1, 2, 3, @machine_rewind, '孙师傅', 1245.00, 58.000, 1.66, '演示客户C-待收款', 'DEMO-MES-202606-C', '复卷改门幅');

INSERT INTO biz_process_step
(uuid, order_uuid, original_uuid, step_sort, step_type, step_name, is_main, knife_count, process_weight, unit_price, step_amount, loss_weight, operator, remark)
VALUES
('demo-step-c-1', @order_c, 'demo-c-orig-1', 1, 1, '标准锯纸', 1, 8, 3000.000, 22.50, 180.00, 42.000, '陈师傅', '625mm×4，含切边'),
('demo-step-c-2', @order_c, 'demo-c-orig-2', 1, 2, '复卷改门幅', 1, 0, 8300.000, 150.00, 1245.00, 58.000, '孙师傅', '1250mm×2');

INSERT INTO biz_process_param
(uuid, order_uuid, original_uuid, step_uuid, param_mode, layer_sort, out_diameter, core_diameter, layer_width, area_ratio, remark)
VALUES
('demo-param-c-2', @order_c, 'demo-c-orig-2', 'demo-step-c-2', 1, 1, 1300, 76, 1250, 50.00, '复卷改门幅');

INSERT INTO biz_finish_roll
(uuid, order_uuid, row_sort, finish_roll_no, roll_no_status, is_spare, paper_name, gram_weight, finish_width,
 finish_diameter, finish_core_diameter, source_type, estimate_weight, actual_weight, trim_width_share,
 trim_weight_share, finish_status, warehouse_uuid, original_roll_nos, actual_remark, remark)
VALUES
('demo-fin-c-1', @order_c, 1, 'Z900101', 2, 0, '白卡', 350, 625, 1300, 76, 1, 730.000, 720.000, 20, 10.000, 3, @warehouse, 'C-MW-001', '锯纸成品', '未结清演示'),
('demo-fin-c-2', @order_c, 2, 'Z900102', 2, 0, '白卡', 350, 625, 1300, 76, 1, 735.000, 728.000, 20, 10.000, 3, @warehouse, 'C-MW-001', '锯纸成品', '未结清演示'),
('demo-fin-c-3', @order_c, 3, 'Z900103', 2, 0, '白卡', 350, 1250, 1300, 76, 1, 2450.000, 2420.000, 0, 18.000, 3, @warehouse, 'C-MW-002', '复卷成品', '未结清演示'),
('demo-fin-c-4', @order_c, 4, 'Z900104', 2, 0, '白卡', 350, 1250, 1300, 76, 1, 2480.000, 2452.000, 0, 20.000, 3, @warehouse, 'C-MW-002', '复卷成品', '未结清演示');

INSERT INTO biz_finish_original_rel (uuid, finish_uuid, original_uuid, order_uuid, share_ratio, share_weight, remark)
VALUES
('demo-rel-c-1', 'demo-fin-c-1', 'demo-c-orig-1', @order_c, 100.00, 720.000, '锯纸来源'),
('demo-rel-c-2', 'demo-fin-c-2', 'demo-c-orig-1', @order_c, 100.00, 728.000, '锯纸来源'),
('demo-rel-c-3', 'demo-fin-c-3', 'demo-c-orig-2', @order_c, 100.00, 2420.000, '复卷来源'),
('demo-rel-c-4', 'demo-fin-c-4', 'demo-c-orig-2', @order_c, 100.00, 2452.000, '复卷来源');

INSERT INTO biz_delivery_order
(uuid, delivery_no, customer_uuid, customer_name, delivery_date, total_count, total_weight, picker_name, car_no, container_no,
 sign_user, sign_time, settle_block_action, delivery_status, remark)
VALUES
(@delivery_c, 'DEMO-CK-202606-C', @customer_c, '演示客户C-待收款', '2026-06-19', 4, 6320.000, '周司机', '苏C-D003', '柜C03', '周司机', '2026-06-19 16:10:00', 0, 2, '演示出库C');

INSERT INTO biz_delivery_detail (uuid, delivery_uuid, finish_uuid, order_uuid, finish_roll_no, paper_name, out_weight, remark)
VALUES
('demo-del-c-1', @delivery_c, 'demo-fin-c-1', @order_c, 'Z900101', '白卡', 720.000, '未结清演示出库'),
('demo-del-c-2', @delivery_c, 'demo-fin-c-2', @order_c, 'Z900102', '白卡', 728.000, '未结清演示出库'),
('demo-del-c-3', @delivery_c, 'demo-fin-c-3', @order_c, 'Z900103', '白卡', 2420.000, '未结清演示出库'),
('demo-del-c-4', @delivery_c, 'demo-fin-c-4', @order_c, 'Z900104', '白卡', 2452.000, '未结清演示出库');

SET @snap_c = JSON_OBJECT(
  'schema_version', '1.1',
  'snapshot_type', 'settle_bill',
  'settle_uuid', @settle_c,
  'settle_no', 'DEMO-JS-202606-C',
  'customer_uuid', @customer_c,
  'customer_name', '演示客户C-待收款',
  'settle_type', 1,
  'settle_date', '2026-06-20',
  'period_start', '2026-06-18',
  'period_end', '2026-06-20',
  'is_invoice', 2,
  'settle_status', 1,
  'amount_no_tax', 1525.00,
  'tax_amount', 0.00,
  'saw_amount', 180.00,
  'rewind_amount', 1245.00,
  'extra_amount', 100.00,
  'total_amount', 1525.00,
  'received_amount', 0.00,
  'unreceived_amount', 1525.00,
  'remark', '演示结算C，待收款',
  'details', JSON_ARRAY(
    JSON_OBJECT(
      'uuid', 'demo-settle-detail-c',
      'settle_uuid', @settle_c,
      'order_uuid', @order_c,
      'order_no', 'DEMO-MES-202606-C',
      'saw_amount', 180.00,
      'rewind_amount', 1245.00,
      'extra_amount', 100.00,
      'order_amount', 1525.00,
      'remark', '未结清收款验收明细'
    )
  ),
  'print_lines', JSON_ARRAY(
    JSON_OBJECT(
      'settle_uuid', @settle_c,
      'order_uuid', @order_c,
      'order_no', 'DEMO-MES-202606-C',
      'order_date', '2026-06-18',
      'original_uuid', 'demo-c-orig-1',
      'original_label', 'C-MW-001',
      'original_roll_no', 'C-MW-001',
      'paper_name', '白卡',
      'gram_weight', 350,
      'actual_gram_weight', 350,
      'original_width', 2500,
      'actual_width', 2500,
      'original_diameter', 1300,
      'core_diameter', 76,
      'original_weight', 3000.000,
      'process_mode', 1,
      'main_step_type', 1,
      'process_text', '锯纸',
      'process_step_summary', '标准锯纸（8刀 / 3000.000kg / 单价 22.50）',
      'finish_summary', 'Z900101、Z900102',
      'finish_detail_summary', 'Z900101（625mm / φ1300 / 720.000kg）；Z900102（625mm / φ1300 / 728.000kg）',
      'finish_count', 2,
      'finish_weight', 1448.000,
      'trim_weight', 20.000,
      'trim_summary', '40mm / 20.000kg',
      'saw_weight', 3000.000,
      'rewind_weight', 0.000,
      'saw_unit_price', 22.50,
      'saw_invoice_unit_price', 22.50,
      'rewind_unit_price', NULL,
      'rewind_invoice_unit_price', NULL,
      'saw_amount', 180.00,
      'rewind_amount', 0.00,
      'process_amount', 180.00,
      'extra_amount', 46.15,
      'extra_fee_summary', '装卸费 60.00；运费 40.00',
      'tax_amount', 0.00,
      'tax_rate', 0.00,
      'line_amount', 226.15,
      'is_invoice', 2,
      'remark', '锯纸 8刀'
    ),
    JSON_OBJECT(
      'settle_uuid', @settle_c,
      'order_uuid', @order_c,
      'order_no', 'DEMO-MES-202606-C',
      'order_date', '2026-06-18',
      'original_uuid', 'demo-c-orig-2',
      'original_label', 'C-MW-002',
      'original_roll_no', 'C-MW-002',
      'paper_name', '白卡',
      'gram_weight', 350,
      'actual_gram_weight', 351,
      'original_width', 2500,
      'actual_width', 2498,
      'original_diameter', 1300,
      'core_diameter', 76,
      'original_weight', 3500.000,
      'process_mode', 1,
      'main_step_type', 2,
      'process_text', '复卷',
      'process_step_summary', '复卷改门幅（8300.000kg / 单价 150.00）',
      'finish_summary', 'Z900103、Z900104',
      'finish_detail_summary', 'Z900103（1250mm / φ1300 / 2420.000kg）；Z900104（1250mm / φ1300 / 2452.000kg）',
      'finish_count', 2,
      'finish_weight', 4872.000,
      'trim_weight', 38.000,
      'trim_summary', '0mm / 38.000kg',
      'saw_weight', 0.000,
      'rewind_weight', 8300.000,
      'saw_unit_price', NULL,
      'saw_invoice_unit_price', NULL,
      'rewind_unit_price', 150.00,
      'rewind_invoice_unit_price', 150.00,
      'saw_amount', 0.00,
      'rewind_amount', 1245.00,
      'process_amount', 1245.00,
      'extra_amount', 53.85,
      'extra_fee_summary', '装卸费 60.00；运费 40.00',
      'tax_amount', 0.00,
      'tax_rate', 0.00,
      'line_amount', 1298.85,
      'is_invoice', 2,
      'remark', '复卷改门幅'
    )
  ),
  'detail_items', JSON_ARRAY(
    JSON_OBJECT(
      'uuid', 'demo-settle-detail-c',
      'settle_uuid', @settle_c,
      'order_uuid', @order_c,
      'order_no', 'DEMO-MES-202606-C',
      'saw_amount', 180.00,
      'rewind_amount', 1245.00,
      'extra_amount', 100.00,
      'order_amount', 1525.00,
      'remark', '未结清收款验收明细'
    )
  ),
  'print_line_items', JSON_ARRAY(
    JSON_OBJECT(
      'settle_uuid', @settle_c,
      'order_uuid', @order_c,
      'order_no', 'DEMO-MES-202606-C',
      'order_date', '2026-06-18',
      'original_uuid', 'demo-c-orig-1',
      'original_label', 'C-MW-001',
      'original_roll_no', 'C-MW-001',
      'paper_name', '白卡',
      'gram_weight', 350,
      'actual_gram_weight', 350,
      'original_width', 2500,
      'actual_width', 2500,
      'original_diameter', 1300,
      'core_diameter', 76,
      'original_weight', 3000.000,
      'process_mode', 1,
      'main_step_type', 1,
      'process_text', '锯纸',
      'process_step_summary', '标准锯纸（8刀 / 3000.000kg / 单价 22.50）',
      'finish_summary', 'Z900101、Z900102',
      'finish_detail_summary', 'Z900101（625mm / φ1300 / 720.000kg）；Z900102（625mm / φ1300 / 728.000kg）',
      'finish_count', 2,
      'finish_weight', 1448.000,
      'trim_weight', 20.000,
      'trim_summary', '40mm / 20.000kg',
      'saw_weight', 3000.000,
      'rewind_weight', 0.000,
      'saw_unit_price', 22.50,
      'saw_invoice_unit_price', 22.50,
      'rewind_unit_price', NULL,
      'rewind_invoice_unit_price', NULL,
      'saw_amount', 180.00,
      'rewind_amount', 0.00,
      'process_amount', 180.00,
      'extra_amount', 46.15,
      'extra_fee_summary', '装卸费 60.00；运费 40.00',
      'tax_amount', 0.00,
      'tax_rate', 0.00,
      'line_amount', 226.15,
      'is_invoice', 2,
      'remark', '锯纸 8刀'
    ),
    JSON_OBJECT(
      'settle_uuid', @settle_c,
      'order_uuid', @order_c,
      'order_no', 'DEMO-MES-202606-C',
      'order_date', '2026-06-18',
      'original_uuid', 'demo-c-orig-2',
      'original_label', 'C-MW-002',
      'original_roll_no', 'C-MW-002',
      'paper_name', '白卡',
      'gram_weight', 350,
      'actual_gram_weight', 351,
      'original_width', 2500,
      'actual_width', 2498,
      'original_diameter', 1300,
      'core_diameter', 76,
      'original_weight', 3500.000,
      'process_mode', 1,
      'main_step_type', 2,
      'process_text', '复卷',
      'process_step_summary', '复卷改门幅（8300.000kg / 单价 150.00）',
      'finish_summary', 'Z900103、Z900104',
      'finish_detail_summary', 'Z900103（1250mm / φ1300 / 2420.000kg）；Z900104（1250mm / φ1300 / 2452.000kg）',
      'finish_count', 2,
      'finish_weight', 4872.000,
      'trim_weight', 38.000,
      'trim_summary', '0mm / 38.000kg',
      'saw_weight', 0.000,
      'rewind_weight', 8300.000,
      'saw_unit_price', NULL,
      'saw_invoice_unit_price', NULL,
      'rewind_unit_price', 150.00,
      'rewind_invoice_unit_price', 150.00,
      'saw_amount', 0.00,
      'rewind_amount', 1245.00,
      'process_amount', 1245.00,
      'extra_amount', 53.85,
      'extra_fee_summary', '装卸费 60.00；运费 40.00',
      'tax_amount', 0.00,
      'tax_rate', 0.00,
      'line_amount', 1298.85,
      'is_invoice', 2,
      'remark', '复卷改门幅'
    )
  )
);

INSERT INTO biz_settle_order
(uuid, settle_no, customer_uuid, customer_name, settle_type, settle_date, period_start, period_end, saw_amount, rewind_amount,
 extra_amount, amount_no_tax, tax_amount, total_amount, received_amount, unreceived_amount, is_invoice, settle_status,
 snap_bill, snap_bill_time, remark)
VALUES
(@settle_c, 'DEMO-JS-202606-C', @customer_c, '演示客户C-待收款', 1, '2026-06-20', '2026-06-18', '2026-06-20',
 180.00, 1245.00, 100.00, 1525.00, 0.00, 1525.00, 0.00, 1525.00, 2, 1, @snap_c, NOW(), '演示结算C，待收款');

INSERT INTO biz_settle_detail
(uuid, settle_uuid, order_uuid, order_no, saw_amount, rewind_amount, extra_amount, order_amount, remark)
VALUES
('demo-settle-detail-c', @settle_c, @order_c, 'DEMO-MES-202606-C', 180.00, 1245.00, 100.00, 1525.00, '未结清收款验收明细');

INSERT INTO sys_operation_log (uuid, biz_type, biz_uuid, biz_no, action_type, operator, operate_time, remark)
VALUES
('demo-log-order-c', '加工单', @order_c, 'DEMO-MES-202606-C', '回录', '系统管理员', '2026-06-18 18:10:00', '演示加工单完成回录'),
('demo-log-delivery-c', '出库单', @delivery_c, 'DEMO-CK-202606-C', '出库确认', '周司机', '2026-06-19 16:10:00', '演示出库签收'),
('demo-log-settle-c', '结算单', @settle_c, 'DEMO-JS-202606-C', '结算', '系统管理员', '2026-06-20 10:00:00', '演示结算生成，待收款');
