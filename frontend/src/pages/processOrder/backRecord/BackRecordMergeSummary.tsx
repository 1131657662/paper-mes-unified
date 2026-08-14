import { Alert, Table, Tag, Typography } from 'antd'
import { Form } from 'antd'
import { formatKg } from '../../../features/processOrderDetail/orderDetailUtils'
import { formatOptionalKg } from '../../../utils/numberFormatters'
import type { BackRecordFormValues } from './backRecordUtils'
import type { BackRecordWorkItem } from './backRecordWorkbenchTypes'

interface Props { item: BackRecordWorkItem }

export default function BackRecordMergeSummary({ item }: Props) {
  const form = Form.useFormInstance<BackRecordFormValues>()
  const values = Form.useWatch('rolls', form) ?? {}
  if (!item.isMergeGroup) return null
  const sources = item.rollProductions.length ? item.rollProductions : item.roll ? [{
    originalUuid: item.roll.uuid, rollNo: item.roll.rollNo, rollWeight: item.roll.rollWeight,
    actualWeight: item.roll.actualWeight,
  }] : []
  const rows = sources.map((source, index) => {
    const recorded = values[source.originalUuid ?? '']?.actualWeight ?? source.actualWeight
    const estimated = source.rollWeight == null ? undefined : source.rollWeight * (source.pieceNum ?? 1)
    return {
      key: source.originalUuid ?? String(index),
      source: source.rollNo || source.originalUuid || `来源母卷 ${index + 1}`,
      estimate: estimated,
      actual: recorded,
      status: recorded != null && recorded > 0 ? '已实测' : source.weightStatus === 'ESTIMATED' ? '估算' : '待称重',
    }
  })
  const total = rows.reduce((sum, row) => sum + (row.actual ?? 0), 0)
  const missing = rows.filter((row) => row.actual == null || row.actual <= 0).length
  return (
    <section className="back-record-panel back-record-merge-summary">
      <div className="back-record-panel__head">
        <Typography.Text strong>{item.title}：逐卷重量总览</Typography.Text>
        <Typography.Text type="secondary">实测合计 {missing > 0 ? '待称重' : formatKg(total)}，已录入 {rows.length - missing}/{rows.length}</Typography.Text>
      </div>
      {missing > 0 && <Alert showIcon type="warning" message={`还有 ${missing} 卷未完成实测；合并复卷完成和正式吨位计费前必须补齐。`} />}
      <Table size="small" pagination={false} rowKey="key" dataSource={rows} columns={[
        { title: '来源母卷', dataIndex: 'source' },
        { title: '标称/估算', dataIndex: 'estimate', render: (value?: number) => formatOptionalKg(value) },
        { title: '实测重量', dataIndex: 'actual', render: (value?: number) => formatOptionalKg(value) },
        { title: '状态', dataIndex: 'status', render: (value: string) => <Tag color={value === '已实测' ? 'success' : value === '估算' ? 'default' : 'warning'}>{value}</Tag> },
      ]} />
    </section>
  )
}
