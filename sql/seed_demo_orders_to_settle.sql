-- 演示数据：两张加工单从加工完成、出库签收到结算收款完成。
-- 覆盖：锯纸、现场定尺、直发、普通复卷，以及复卷 5 类模式。
-- 可重复执行：先删除 DEMO-MES-202606 前缀数据，再重新插入。

SET @customer_a = 'demo-customer-a-uuid';
SET @customer_b = 'demo-customer-b-uuid';
SET @machine_saw = 'demo-machine-saw-uuid';
SET @machine_rewind = 'demo-machine-rewind-uuid';
SET @warehouse = 'demo-warehouse-uuid';

SET @order_a = 'demo-order-a-uuid';
SET @order_b = 'demo-order-b-uuid';
SET @delivery_a = 'demo-delivery-a-uuid';
SET @delivery_b = 'demo-delivery-b-uuid';
SET @settle_a = 'demo-settle-a-uuid';
SET @settle_b = 'demo-settle-b-uuid';

DELETE FROM biz_receive_record WHERE settle_uuid IN (@settle_a, @settle_b);
DELETE FROM biz_settle_detail WHERE settle_uuid IN (@settle_a, @settle_b);
DELETE FROM biz_settle_order WHERE uuid IN (@settle_a, @settle_b);
DELETE FROM biz_delivery_detail WHERE delivery_uuid IN (@delivery_a, @delivery_b);
DELETE FROM biz_delivery_order WHERE uuid IN (@delivery_a, @delivery_b);
DELETE FROM biz_finish_original_rel WHERE order_uuid IN (@order_a, @order_b);
DELETE FROM biz_finish_roll WHERE order_uuid IN (@order_a, @order_b);
DELETE FROM biz_process_param WHERE order_uuid IN (@order_a, @order_b);
DELETE FROM biz_process_step WHERE order_uuid IN (@order_a, @order_b);
DELETE FROM biz_original_roll WHERE order_uuid IN (@order_a, @order_b);
DELETE FROM biz_process_order WHERE uuid IN (@order_a, @order_b);
DELETE FROM sys_operation_log WHERE biz_uuid IN (@order_a, @order_b, @delivery_a, @delivery_b, @settle_a, @settle_b);

INSERT INTO sys_customer (uuid, customer_code, customer_name, contact, phone, settle_type, settle_day, saw_price, rewind_price, default_invoice, price_include_tax, tax_rate, remark)
VALUES
(@customer_a, 'DEMO-A', '演示客户A-华东纸业', '王经理', '13800000001', 2, 25, 8.00, 120.00, 1, 2, 13.00, '演示数据客户'),
(@customer_b, 'DEMO-B', '演示客户B-南方包装', '李经理', '13800000002', 1, NULL, 9.00, 135.00, 2, 2, 0.00, '演示数据客户')
ON DUPLICATE KEY UPDATE customer_name = VALUES(customer_name), saw_price = VALUES(saw_price), rewind_price = VALUES(rewind_price);

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
 tax_rate, loading_fee, process_amount_no_tax, process_amount_tax, extra_amount_no_tax, extra_amount_tax, total_amount_no_tax,
 total_amount_tax, total_process_amount, total_extra_amount, total_amount, total_original_weight, total_original_ton,
 total_finish_weight, total_step_count, actual_total_knife, order_status, print_status, print_count, last_print_time,
 last_print_user, back_record_time, back_record_user, is_mix_process, remark)
VALUES
(@order_a, 'DEMO-MES-202606-A', @customer_a, '演示客户A-华东纸业', '2026-06-02', 2, @warehouse, 'A班', 1, 2, 25,
 13.00, 80.00, 1140.00, 148.20, 80.00, 10.40, 1220.00, 158.60, 1140.00, 80.00, 1378.60,
 8800.000, 8.800, 8465.000, 5, 18, 5, 1, 1, '2026-06-02 09:20:00', '系统管理员',
 '2026-06-02 17:30:00', '系统管理员', 1, '演示：锯纸、现场定尺、直发、普通复卷'),
(@order_b, 'DEMO-MES-202606-B', @customer_b, '演示客户B-南方包装', '2026-06-12', 1, @warehouse, 'B班', 2, 1, NULL,
 0.00, 0.00, 0.00, 0.00, 30.00, 0.00, 1905.00, 0.00, 1875.00, 30.00, 1905.00,
 12500.000, 12.500, 12120.000, 6, 0, 5, 1, 1, '2026-06-12 10:10:00', '系统管理员',
 '2026-06-12 18:40:00', '系统管理员', 0, '演示：复卷五类模式')
ON DUPLICATE KEY UPDATE order_status = VALUES(order_status), total_amount = VALUES(total_amount);

INSERT INTO biz_original_roll
(uuid, order_uuid, row_sort, roll_no, paper_name, gram_weight, original_width, original_diameter, core_diameter,
 roll_weight, actual_weight, piece_num, total_weight, process_mode, main_step_type, roll_status, machine_uuid,
 operator, process_amount, total_loss_weight, total_loss_ratio, customer_name, order_no, remark)
VALUES
('demo-a-orig-1', @order_a, 1, 'A-MW-001', '白卡', 300, 2520, 1300, 76, 2200.000, 2200.000, 1, 2200.000, 1, 1, 3, @machine_saw, '张师傅', 144.00, 35.000, 1.59, '演示客户A-华东纸业', 'DEMO-MES-202606-A', '标准锯纸 6刀'),
('demo-a-orig-2', @order_a, 2, 'A-MW-002', '白卡', 300, 2520, 1300, 76, 2200.000, 2200.000, 1, 2200.000, 2, 1, 3, @machine_saw, '张师傅', 96.00, 22.000, 1.00, '演示客户A-华东纸业', 'DEMO-MES-202606-A', '现场定尺锯纸 4刀'),
('demo-a-orig-3', @order_a, 3, 'A-MW-003', '白卡', 300, 2520, 1300, 76, 2200.000, 2200.000, 1, 2200.000, 3, NULL, 4, NULL, NULL, 0.00, 0.000, 0.00, '演示客户A-华东纸业', 'DEMO-MES-202606-A', '不加工直发'),
('demo-a-orig-4', @order_a, 4, 'A-MW-004', '白卡', 300, 2520, 1300, 76, 2200.000, 2200.000, 1, 2200.000, 1, 2, 3, @machine_rewind, '李师傅', 900.00, 45.000, 2.05, '演示客户A-华东纸业', 'DEMO-MES-202606-A', '普通复卷改门幅'),
('demo-b-orig-1', @order_b, 1, 'B-MW-001', '牛卡', 200, 2000, 1300, 76, 2500.000, 2500.000, 1, 2500.000, 1, 2, 3, @machine_rewind, '赵师傅', 337.50, 60.000, 2.40, '演示客户B-南方包装', 'DEMO-MES-202606-B', '模式1 改门幅'),
('demo-b-orig-2', @order_b, 2, 'B-MW-002', '牛卡', 200, 2000, 1300, 76, 2500.000, 2500.000, 1, 2500.000, 1, 2, 3, @machine_rewind, '赵师傅', 337.50, 70.000, 2.80, '演示客户B-南方包装', 'DEMO-MES-202606-B', '模式2 改直径'),
('demo-b-orig-3', @order_b, 3, 'B-MW-003', '牛卡', 200, 2000, 1300, 76, 2500.000, 2500.000, 1, 2500.000, 1, 2, 3, @machine_rewind, '赵师傅', 405.00, 75.000, 3.00, '演示客户B-南方包装', 'DEMO-MES-202606-B', '模式3 改直径+门幅'),
('demo-b-orig-4', @order_b, 4, 'B-MW-004', '牛卡', 200, 2000, 1300, 76, 2500.000, 2500.000, 1, 2500.000, 1, 2, 3, @machine_rewind, '赵师傅', 397.50, 85.000, 3.40, '演示客户B-南方包装', 'DEMO-MES-202606-B', '模式4 内外层分层'),
('demo-b-orig-5', @order_b, 5, 'B-MW-005', '牛卡', 200, 2000, 1300, 76, 2500.000, 2500.000, 1, 2500.000, 1, 2, 3, @machine_rewind, '赵师傅', 397.50, 90.000, 3.60, '演示客户B-南方包装', 'DEMO-MES-202606-B', '模式5 多母卷合并'),
('demo-b-orig-6', @order_b, 6, 'B-MW-006', '牛卡', 200, 2000, 1300, 76, 2500.000, 2500.000, 1, 2500.000, 1, 2, 3, @machine_rewind, '赵师傅', 0.00, 0.000, 0.00, '演示客户B-南方包装', 'DEMO-MES-202606-B', '模式5 多母卷合并来源');

INSERT INTO biz_process_step
(uuid, order_uuid, original_uuid, step_sort, step_type, step_name, is_main, knife_count, process_weight, unit_price, step_amount, loss_weight, operator, remark)
VALUES
('demo-step-a-1', @order_a, 'demo-a-orig-1', 1, 1, '标准锯纸', 1, 6, 2200.000, 24.00, 144.00, 35.000, '张师傅', '600+600+600+600+120切边'),
('demo-step-a-2', @order_a, 'demo-a-orig-2', 1, 1, '现场定尺', 1, 4, 2200.000, 24.00, 96.00, 22.000, '张师傅', '现场按客户尺寸切'),
('demo-step-a-4', @order_a, 'demo-a-orig-4', 1, 2, '普通复卷', 1, 0, 7.500, 120.00, 900.00, 45.000, '李师傅', '改门幅 840×3'),
('demo-step-b-1', @order_b, 'demo-b-orig-1', 1, 2, '模式1改门幅', 1, 0, 2.500, 135.00, 337.50, 60.000, '赵师傅', '950×2，剩余转修边'),
('demo-step-b-2', @order_b, 'demo-b-orig-2', 1, 2, '模式2改直径', 1, 0, 2.500, 135.00, 337.50, 70.000, '赵师傅', '2000mm门幅不变，φ650×2'),
('demo-step-b-3', @order_b, 'demo-b-orig-3', 1, 2, '模式3改直径+门幅', 1, 0, 3.000, 135.00, 405.00, 75.000, '赵师傅', '1000mm×2段，φ650'),
('demo-step-b-4', @order_b, 'demo-b-orig-4', 1, 2, '模式4内外层分层', 1, 0, 2.944, 135.00, 397.50, 85.000, '赵师傅', '内层900 外层1100'),
('demo-step-b-5', @order_b, 'demo-b-orig-5', 1, 2, '模式5多母卷合并', 1, 0, 2.944, 135.00, 397.50, 90.000, '赵师傅', 'B-MW-005 70% + B-MW-006 30% 接纸');

INSERT INTO biz_process_param
(uuid, order_uuid, original_uuid, step_uuid, param_mode, layer_sort, out_diameter, core_diameter, layer_width, area_ratio, split_ratio, remark)
VALUES
('demo-param-a-4', @order_a, 'demo-a-orig-4', 'demo-step-a-4', 1, 1, 1300, 76, 840, 33.33, NULL, '普通复卷改门幅'),
('demo-param-b-1', @order_b, 'demo-b-orig-1', 'demo-step-b-1', 1, 1, 1300, 76, 950, 50.00, NULL, '改门幅 950×2'),
('demo-param-b-2', @order_b, 'demo-b-orig-2', 'demo-step-b-2', 2, 1, 650, 76, 2000, 50.00, NULL, '改直径 φ650×2'),
('demo-param-b-3', @order_b, 'demo-b-orig-3', 'demo-step-b-3', 3, 1, 650, 76, 1000, 50.00, NULL, '改直径+门幅'),
('demo-param-b-4a', @order_b, 'demo-b-orig-4', 'demo-step-b-4', 4, 1, 650, 76, 900, 45.00, NULL, '内层'),
('demo-param-b-4b', @order_b, 'demo-b-orig-4', 'demo-step-b-4', 4, 2, 900, 76, 1100, 55.00, NULL, '外层'),
('demo-param-b-5a', @order_b, 'demo-b-orig-5', 'demo-step-b-5', 5, 1, 900, 76, 1000, 50.00, 70.00, '多母卷合并来源1'),
('demo-param-b-5b', @order_b, 'demo-b-orig-6', 'demo-step-b-5', 5, 1, 900, 76, 1000, 50.00, 30.00, '多母卷合并来源2');

INSERT INTO biz_finish_roll
(uuid, order_uuid, row_sort, finish_roll_no, roll_no_status, is_spare, paper_name, gram_weight, finish_width,
 finish_diameter, finish_core_diameter, source_type, estimate_weight, actual_weight, trim_width_share,
 trim_weight_share, finish_status, warehouse_uuid, original_roll_nos, actual_remark, remark)
VALUES
('demo-fin-a-1', @order_a, 1, 'Z900001', 2, 0, '白卡', 300, 600, 1300, 76, 1, 520.000, 515.000, 30, 8.000, 3, @warehouse, 'A-MW-001', '锯纸成品', '标准锯纸'),
('demo-fin-a-2', @order_a, 2, 'Z900002', 2, 0, '白卡', 300, 600, 1300, 76, 1, 520.000, 512.000, 30, 8.000, 3, @warehouse, 'A-MW-001', '锯纸成品', '标准锯纸'),
('demo-fin-a-3', @order_a, 3, 'Z900003', 2, 0, '白卡', 300, 630, 1300, 76, 1, 760.000, 748.000, 20, 6.000, 3, @warehouse, 'A-MW-002', '现场定尺', '现场定尺'),
('demo-fin-a-4', @order_a, 4, 'A-MW-003', 2, 0, '白卡', 300, 2520, 1300, 76, 2, 2200.000, 2200.000, 0, 0.000, 3, @warehouse, 'A-MW-003', '直发出库', '直发'),
('demo-fin-a-5', @order_a, 5, 'Z900004', 2, 0, '白卡', 300, 840, 1300, 76, 1, 900.000, 890.000, 0, 10.000, 3, @warehouse, 'A-MW-004', '普通复卷', '840mm成品'),
('demo-fin-b-1', @order_b, 1, 'Z900005', 2, 0, '牛卡', 200, 950, 1300, 76, 1, 1200.000, 1190.000, 100, 25.000, 3, @warehouse, 'B-MW-001', '模式1', '改门幅'),
('demo-fin-b-2', @order_b, 2, 'Z900006', 2, 0, '牛卡', 200, 2000, 650, 76, 1, 1150.000, 1135.000, 0, 35.000, 3, @warehouse, 'B-MW-002', '模式2', '改直径'),
('demo-fin-b-3', @order_b, 3, 'Z900007', 2, 0, '牛卡', 200, 1000, 650, 76, 1, 1320.000, 1308.000, 0, 22.000, 3, @warehouse, 'B-MW-003', '模式3', '改直径+门幅'),
('demo-fin-b-4', @order_b, 4, 'Z900008', 2, 0, '牛卡', 200, 900, 650, 76, 1, 960.000, 952.000, 0, 18.000, 3, @warehouse, 'B-MW-004', '模式4内层', '内层'),
('demo-fin-b-5', @order_b, 5, 'Z900009', 2, 0, '牛卡', 200, 1100, 900, 76, 1, 1480.000, 1460.000, 0, 24.000, 3, @warehouse, 'B-MW-004', '模式4外层', '外层'),
('demo-fin-b-6', @order_b, 6, 'Z900010', 2, 0, '牛卡', 200, 1000, 900, 76, 1, 2400.000, 2360.000, 0, 45.000, 3, @warehouse, 'B-MW-005,B-MW-006', '模式5', '多母卷合并');

INSERT INTO biz_finish_original_rel (uuid, finish_uuid, original_uuid, order_uuid, share_ratio, share_weight, remark)
VALUES
('demo-rel-a-1', 'demo-fin-a-1', 'demo-a-orig-1', @order_a, 100.00, 515.000, '锯纸来源'),
('demo-rel-a-2', 'demo-fin-a-2', 'demo-a-orig-1', @order_a, 100.00, 512.000, '锯纸来源'),
('demo-rel-a-3', 'demo-fin-a-3', 'demo-a-orig-2', @order_a, 100.00, 748.000, '现场定尺来源'),
('demo-rel-a-4', 'demo-fin-a-4', 'demo-a-orig-3', @order_a, 100.00, 2200.000, '直发来源'),
('demo-rel-a-5', 'demo-fin-a-5', 'demo-a-orig-4', @order_a, 100.00, 890.000, '复卷来源'),
('demo-rel-b-1', 'demo-fin-b-1', 'demo-b-orig-1', @order_b, 100.00, 1190.000, '模式1来源'),
('demo-rel-b-2', 'demo-fin-b-2', 'demo-b-orig-2', @order_b, 100.00, 1135.000, '模式2来源'),
('demo-rel-b-3', 'demo-fin-b-3', 'demo-b-orig-3', @order_b, 100.00, 1308.000, '模式3来源'),
('demo-rel-b-4', 'demo-fin-b-4', 'demo-b-orig-4', @order_b, 100.00, 952.000, '模式4内层'),
('demo-rel-b-5', 'demo-fin-b-5', 'demo-b-orig-4', @order_b, 100.00, 1460.000, '模式4外层'),
('demo-rel-b-6a', 'demo-fin-b-6', 'demo-b-orig-5', @order_b, 70.00, 1652.000, '模式5来源1'),
('demo-rel-b-6b', 'demo-fin-b-6', 'demo-b-orig-6', @order_b, 30.00, 708.000, '模式5来源2');

INSERT INTO biz_delivery_order
(uuid, delivery_no, customer_uuid, customer_name, delivery_date, total_count, total_weight, picker_name, car_no, container_no,
 sign_user, sign_time, settle_block_action, delivery_status, remark)
VALUES
(@delivery_a, 'DEMO-CK-202606-A', @customer_a, '演示客户A-华东纸业', '2026-06-03', 5, 4865.000, '王司机', '沪A-D001', '柜A01', '王司机', '2026-06-03 15:30:00', 0, 2, '演示出库A'),
(@delivery_b, 'DEMO-CK-202606-B', @customer_b, '演示客户B-南方包装', '2026-06-13', 6, 8405.000, '李司机', '粤B-D002', '柜B02', '李司机', '2026-06-13 16:20:00', 0, 2, '演示出库B');

INSERT INTO biz_delivery_detail (uuid, delivery_uuid, finish_uuid, order_uuid, finish_roll_no, paper_name, out_weight, remark)
VALUES
('demo-del-a-1', @delivery_a, 'demo-fin-a-1', @order_a, 'Z900001', '白卡', 515.000, '演示出库'),
('demo-del-a-2', @delivery_a, 'demo-fin-a-2', @order_a, 'Z900002', '白卡', 512.000, '演示出库'),
('demo-del-a-3', @delivery_a, 'demo-fin-a-3', @order_a, 'Z900003', '白卡', 748.000, '演示出库'),
('demo-del-a-4', @delivery_a, 'demo-fin-a-4', @order_a, 'A-MW-003', '白卡', 2200.000, '演示直发'),
('demo-del-a-5', @delivery_a, 'demo-fin-a-5', @order_a, 'Z900004', '白卡', 890.000, '演示出库'),
('demo-del-b-1', @delivery_b, 'demo-fin-b-1', @order_b, 'Z900005', '牛卡', 1190.000, '演示出库'),
('demo-del-b-2', @delivery_b, 'demo-fin-b-2', @order_b, 'Z900006', '牛卡', 1135.000, '演示出库'),
('demo-del-b-3', @delivery_b, 'demo-fin-b-3', @order_b, 'Z900007', '牛卡', 1308.000, '演示出库'),
('demo-del-b-4', @delivery_b, 'demo-fin-b-4', @order_b, 'Z900008', '牛卡', 952.000, '演示出库'),
('demo-del-b-5', @delivery_b, 'demo-fin-b-5', @order_b, 'Z900009', '牛卡', 1460.000, '演示出库'),
('demo-del-b-6', @delivery_b, 'demo-fin-b-6', @order_b, 'Z900010', '牛卡', 2360.000, '演示出库');

INSERT INTO biz_settle_order
(uuid, settle_no, customer_uuid, customer_name, settle_type, settle_date, period_start, period_end, saw_amount, rewind_amount,
 extra_amount, amount_no_tax, tax_amount, total_amount, received_amount, unreceived_amount, is_invoice, settle_status, remark)
VALUES
(@settle_a, 'DEMO-JS-202606-A', @customer_a, '演示客户A-华东纸业', 2, '2026-06-25', '2026-06-01', '2026-06-25', 240.00, 900.00, 80.00, 1220.00, 158.60, 1378.60, 1378.60, 0.00, 1, 3, '演示结算A，已收清'),
(@settle_b, 'DEMO-JS-202606-B', @customer_b, '演示客户B-南方包装', 1, '2026-06-13', '2026-06-12', '2026-06-13', 0.00, 1875.00, 30.00, 1905.00, 0.00, 1905.00, 1905.00, 0.00, 2, 3, '演示结算B，已收清');

INSERT INTO biz_settle_detail
(uuid, settle_uuid, order_uuid, order_no, saw_amount, rewind_amount, extra_amount, order_amount, remark)
VALUES
('demo-settle-detail-a', @settle_a, @order_a, 'DEMO-MES-202606-A', 240.00, 900.00, 80.00, 1378.60, '演示结算明细A'),
('demo-settle-detail-b', @settle_b, @order_b, 'DEMO-MES-202606-B', 0.00, 1875.00, 30.00, 1905.00, '演示结算明细B');

INSERT INTO biz_receive_record
(uuid, settle_uuid, receive_date, receive_amount, pay_method, pay_no, operator, remark)
VALUES
('demo-receive-a', @settle_a, '2026-06-26 10:00:00', 1378.60, 2, 'DEMO-PAY-A', '系统管理员', '演示收款A'),
('demo-receive-b', @settle_b, '2026-06-14 11:30:00', 1905.00, 2, 'DEMO-PAY-B', '系统管理员', '演示收款B');

INSERT INTO sys_operation_log (uuid, biz_type, biz_uuid, biz_no, action_type, operator, operate_time, remark)
VALUES
('demo-log-order-a', '加工单', @order_a, 'DEMO-MES-202606-A', '回录', '系统管理员', '2026-06-02 17:30:00', '演示加工单完成回录'),
('demo-log-order-b', '加工单', @order_b, 'DEMO-MES-202606-B', '回录', '系统管理员', '2026-06-12 18:40:00', '演示加工单完成回录'),
('demo-log-delivery-a', '出库单', @delivery_a, 'DEMO-CK-202606-A', '出库确认', '王司机', '2026-06-03 15:30:00', '演示出库签收'),
('demo-log-delivery-b', '出库单', @delivery_b, 'DEMO-CK-202606-B', '出库确认', '李司机', '2026-06-13 16:20:00', '演示出库签收'),
('demo-log-settle-a', '结算单', @settle_a, 'DEMO-JS-202606-A', '收款', '系统管理员', '2026-06-26 10:00:00', '演示结算收款完成'),
('demo-log-settle-b', '结算单', @settle_b, 'DEMO-JS-202606-B', '收款', '系统管理员', '2026-06-14 11:30:00', '演示结算收款完成');
