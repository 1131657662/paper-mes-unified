import { Descriptions, Modal, Table, Tag } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { CLOSE_LEVEL } from '../../../constants/processOrder'
import type { BackRecordResultVO, RollCheck } from '../../../types/processOrder'
import { formatOptionalKg, formatPercent } from '../../../utils/numberFormatters'
import { worstRollCheck } from './backRecordUtils'

export function showBackRecordResult(result: BackRecordResultVO) {
  const check = worstRollCheck(result)
  Modal.info({
    title: result.orderCompleted ? '整单回录完成' : '本批回录已保存',
    width: 760,
    content: (
      <>
        <Descriptions className="back-record-result-summary" column={2} size="small">
          <Descriptions.Item label="单号">{result.orderNo}</Descriptions.Item>
          <Descriptions.Item label="状态">{result.orderCompleted ? '已完成' : '待回录（部分完成）'}</Descriptions.Item>
          <Descriptions.Item label="本批母卷">{result.recordedRollCount ?? 0} 组</Descriptions.Item>
          <Descriptions.Item label="剩余母卷">{result.remainingRollCount ?? 0} 卷</Descriptions.Item>
          <Descriptions.Item label="闭合结果">
            <Tag color={CLOSE_LEVEL[check?.level ?? 'PASS']?.color}>{CLOSE_LEVEL[check?.level ?? 'PASS']?.text}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="最大偏差率">{check?.diffRatioPct == null ? '-' : formatPercent(check.diffRatioPct)}</Descriptions.Item>
          <Descriptions.Item label="直发成品">自动生成 {result.directShipGenerated ?? 0} 条</Descriptions.Item>
          <Descriptions.Item label="备用号">自动作废 {result.voidedSpareCount ?? 0} 个</Descriptions.Item>
          {result.overToleranceReleased && (
            <Descriptions.Item label="超差放行" span={2}>
              <Tag color="error">已授权放行</Tag>
            </Descriptions.Item>
          )}
        </Descriptions>
        <Table
          rowKey={(row) => row.originalUuid ?? row.rollNo ?? 'summary'}
          size="small"
          columns={columns}
          dataSource={result.rollChecks ?? []}
          pagination={false}
        />
      </>
    ),
  })
}

const columns: ColumnsType<RollCheck> = [
  { title: '母卷', dataIndex: 'rollNo', width: 120, render: (value) => value || '-' },
  {
    title: '结果',
    dataIndex: 'level',
    width: 100,
    render: (value) => <Tag color={CLOSE_LEVEL[value]?.color}>{CLOSE_LEVEL[value]?.text ?? value}</Tag>,
  },
  { title: '复称重量', dataIndex: 'actualWeight', align: 'right', width: 110, render: (value) => formatOptionalKg(value) },
  { title: '理论合计', dataIndex: 'theoreticalWeight', align: 'right', width: 110, render: (value) => formatOptionalKg(value) },
  { title: '偏差', dataIndex: 'diffWeight', align: 'right', width: 100, render: (value) => formatOptionalKg(value) },
  { title: '偏差率', dataIndex: 'diffRatioPct', align: 'right', width: 90, render: (value) => value == null ? '-' : formatPercent(value) },
]
