-- V3.31: publish a new immutable metric bundle for operational report topics.
SET SESSION lock_wait_timeout = 5;
SET SESSION innodb_lock_wait_timeout = 5;
SET SESSION group_concat_max_len = 65535;

SELECT GET_LOCK('paper_mes_report_metric_baseline', 10) INTO @report_metric_lock;
START TRANSACTION;

INSERT IGNORE INTO rpt_metric_definition
  (uuid, metric_code, metric_name, description, value_type, unit_code, display_scale, display_order)
VALUES
  (REPLACE(UUID(), '-', ''), 'settlement_document_count', '有效结算单数', '未作废的结算单数量', 'INTEGER', 'DOCUMENT', 0, 200),
  (REPLACE(UUID(), '-', ''), 'settlement_pending_count', '待收款结算单数', '状态为待收款的有效结算单数量', 'INTEGER', 'DOCUMENT', 0, 210),
  (REPLACE(UUID(), '-', ''), 'settlement_partial_count', '部分收款结算单数', '状态为部分收款的有效结算单数量', 'INTEGER', 'DOCUMENT', 0, 220),
  (REPLACE(UUID(), '-', ''), 'overdue_document_count', '逾期结算单数', '到期且仍有欠款的有效结算单数量', 'INTEGER', 'DOCUMENT', 0, 230),
  (REPLACE(UUID(), '-', ''), 'overdue_amount', '逾期金额', '逾期有效结算单的未收金额', 'MONEY', 'CNY', 2, 240),
  (REPLACE(UUID(), '-', ''), 'collection_record_count', '有效回款流水数', '未撤销的有效回款流水数量', 'INTEGER', 'RECORD', 0, 250),
  (REPLACE(UUID(), '-', ''), 'discount_amount', '优惠核销金额', '有效回款流水中的优惠及尾差核销金额', 'MONEY', 'CNY', 2, 260),
  (REPLACE(UUID(), '-', ''), 'scrap_weight_kg', '废纸抵扣重量', '有效回款流水中的废纸抵扣重量', 'DECIMAL', 'KG', 3, 270),
  (REPLACE(UUID(), '-', ''), 'inventory_roll_count', '当前库存卷数', '查询范围内当前在库成品卷数量', 'INTEGER', 'ROLL', 0, 280),
  (REPLACE(UUID(), '-', ''), 'inventory_available_count', '可用库存卷数', '当前在库且未被待出库单锁定的成品卷数量', 'INTEGER', 'ROLL', 0, 290),
  (REPLACE(UUID(), '-', ''), 'inventory_locked_count', '锁定库存卷数', '当前在库且被待出库单锁定的成品卷数量', 'INTEGER', 'ROLL', 0, 300),
  (REPLACE(UUID(), '-', ''), 'inventory_exception_count', '异常库存卷数', '仓库或入库时间缺失以及异常的当前库存卷数量', 'INTEGER', 'ROLL', 0, 310),
  (REPLACE(UUID(), '-', ''), 'inventory_weight_kg', '当前库存重量', '当前在库成品卷剩余可出库重量', 'DECIMAL', 'KG', 3, 320),
  (REPLACE(UUID(), '-', ''), 'inventory_locked_weight_kg', '锁定库存重量', '被待出库单锁定的当前库存重量', 'DECIMAL', 'KG', 3, 330),
  (REPLACE(UUID(), '-', ''), 'delivery_document_count', '有效出库单数', '待出库与已签收的有效出库单数量', 'INTEGER', 'DOCUMENT', 0, 340),
  (REPLACE(UUID(), '-', ''), 'delivery_pending_count', '待出库单数', '状态为待出库的有效出库单数量', 'INTEGER', 'DOCUMENT', 0, 350),
  (REPLACE(UUID(), '-', ''), 'delivery_completed_count', '已出库单数', '状态为已出库签收的有效出库单数量', 'INTEGER', 'DOCUMENT', 0, 360),
  (REPLACE(UUID(), '-', ''), 'delivery_pending_weight_kg', '待出库重量', '待出库单有效明细的出库重量', 'DECIMAL', 'KG', 3, 370),
  (REPLACE(UUID(), '-', ''), 'delivery_completed_weight_kg', '已出库重量', '已出库签收单有效明细的出库重量', 'DECIMAL', 'KG', 3, 380);

INSERT IGNORE INTO rpt_metric_version
  (uuid, metric_uuid, version_no, implementation_key, definition_json,
   definition_checksum, version_status, locked_at, locked_by)
SELECT REPLACE(UUID(), '-', ''), d.uuid, 1, CONCAT('report.sql.', d.metric_code),
       JSON_OBJECT('implementationKey', CONCAT('report.sql.', d.metric_code), 'semanticVersion', 1),
       SHA2(CONCAT(d.metric_code, '|1|report.sql.', d.metric_code), 256),
       2, CURRENT_TIMESTAMP, 'system'
FROM rpt_metric_definition d
WHERE d.is_deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM rpt_metric_version v
    WHERE v.metric_uuid = d.uuid AND v.version_no = 1
  );

INSERT IGNORE INTO rpt_metric_release
  (uuid, release_code, release_name, release_status)
VALUES (REPLACE(UUID(), '-', ''), 'REPORT-BASELINE-V2', '统计报表全域指标口径 V2', 1);

INSERT IGNORE INTO rpt_metric_release_item
  (uuid, release_uuid, metric_uuid, metric_version_uuid, display_order, create_by)
SELECT REPLACE(UUID(), '-', ''), r.uuid, d.uuid, v.uuid, d.display_order, 'system'
FROM rpt_metric_release r
JOIN rpt_metric_definition d ON d.is_deleted = 0 AND d.is_enabled = 1
JOIN rpt_metric_version v
  ON v.metric_uuid = d.uuid AND v.version_no = 1 AND v.is_deleted = 0
WHERE r.release_code = 'REPORT-BASELINE-V2';

UPDATE rpt_metric_release
SET release_status = 3, retired_at = CURRENT_TIMESTAMP, retired_by = 'system'
WHERE is_deleted = 0 AND release_status = 2
  AND release_code != 'REPORT-BASELINE-V2'
  AND EXISTS (
    SELECT 1 FROM (
      SELECT uuid FROM rpt_metric_release
      WHERE release_code = 'REPORT-BASELINE-V2' AND release_status IN (1, 2)
    ) target_release
  );

UPDATE rpt_metric_release r
SET r.release_checksum = (
      SELECT SHA2(GROUP_CONCAT(CONCAT(d.metric_code, ':', v.definition_checksum)
        ORDER BY d.metric_code SEPARATOR '|'), 256)
      FROM rpt_metric_release_item i
      JOIN rpt_metric_definition d ON d.uuid = i.metric_uuid
      JOIN rpt_metric_version v ON v.uuid = i.metric_version_uuid
      WHERE i.release_uuid = r.uuid
    ),
    r.release_status = 2,
    r.published_at = COALESCE(r.published_at, CURRENT_TIMESTAMP),
    r.published_by = COALESCE(r.published_by, 'system')
WHERE r.release_code = 'REPORT-BASELINE-V2' AND r.release_status IN (1, 2);

COMMIT;
SELECT RELEASE_LOCK('paper_mes_report_metric_baseline') INTO @report_metric_unlock;
