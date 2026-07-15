import { Tag, Typography } from 'antd'
import type { ProcessOrder } from '../../types/processOrder'
import { ORDER_STATUS } from '../../constants/processOrder'
import { formatMoney, formatTonFromKg } from '../../utils/numberFormatters'

export function OrderNoCell({ onDetail, record }: { onDetail: (uuid: string) => void; record: ProcessOrder }) {
  return (
    <div className="process-order-list__order">
      <Typography.Link
        href={`/process-orders/${record.uuid}`}
        strong
        onClick={(event) => {
          event.preventDefault()
          onDetail(record.uuid)
        }}
      >
        {record.orderNo || '-'}
      </Typography.Link>
      <div className="process-order-list__order-meta">
        <Typography.Text type="secondary">{record.isMixProcess === 1 ? '混合工艺' : '单一工艺'}</Typography.Text>
        {(record.priority ?? 1) > 1 && <PriorityPill value={record.priority} />}
      </div>
    </div>
  )
}

export function ProductionSummary({ record }: { record: ProcessOrder }) {
  const originalWeight = record.originalRollWeight ?? record.totalOriginalWeight
  const estimateWeight = record.estimateFinishWeight ?? estimateFallback(record)
  const actualWeight = record.actualFinishWeight ?? record.totalFinishWeight

  return (
    <div className="process-order-list__summary">
      <span>母卷 {record.originalRollCount ?? 0} 卷 / {formatTon(originalWeight)}</span>
      <span>成品 {record.finishRollCount ?? 0} 件 / 预估 {formatTon(estimateWeight)}</span>
      {hasWeight(actualWeight) && <span>实际 {formatTon(actualWeight)}</span>}
      {record.actualTotalKnife != null && record.actualTotalKnife > 0 && <span>锯纸 {record.actualTotalKnife} 刀</span>}
      {record.spareRollCount != null && record.spareRollCount > 0 && <span>备用 {record.spareRollCount} 件</span>}
    </div>
  )
}

export function OrderDateScheduleCell({ record }: { record: ProcessOrder }) {
  return (
    <div className="process-order-list__schedule">
      <span>制单 {record.orderDate || '-'}</span>
      {record.expectFinishDate && <span>交期 {record.expectFinishDate}</span>}
      {record.teamGroup && <span>班组 {record.teamGroup}</span>}
    </div>
  )
}

export function BillingCell({ record }: { record: ProcessOrder }) {
  const invoice = invoiceConfig(record.isInvoice)
  return (
    <div className="process-order-list__billing">
      <div className="process-order-list__billing-meta">
        <Tag className="process-order-list__billing-tag">{settleText(record.settleType, record.settleDay)}</Tag>
        <Tag color={invoice.color}>{invoice.text}</Tag>
      </div>
      <strong>{record.totalAmount ? formatMoney(record.totalAmount) : '-'}</strong>
    </div>
  )
}

export function OrderStatusCell({ record }: { record: ProcessOrder }) {
  const status = record.orderStatus == null ? undefined : ORDER_STATUS[record.orderStatus]
  const printText = record.printStatus === 1 ? `已下发 ${record.printCount ?? 1} 次` : '未下发'
  return (
    <div className="process-order-list__status">
      <Tag color={status?.color}>{status?.text ?? '-'}</Tag>
      <span>{printText}</span>
    </div>
  )
}

export function PriorityPill({ value }: { value?: number }) {
  const priority = priorityConfig[value ?? 1] ?? { text: '普通', className: 'normal' }
  return <span className={`process-order-priority process-order-priority--${priority.className}`}>{priority.text}</span>
}

function formatTon(value?: number) {
  return formatTonFromKg(value)
}

function estimateFallback(record: ProcessOrder) {
  if (record.actualFinishWeight || record.totalFinishWeight) return undefined
  return record.finishRollWeight
}

function hasWeight(value?: number) {
  return value != null && value > 0
}

function settleText(settleType?: number, settleDay?: number) {
  if (settleType === 2) return settleDay ? `月结 ${settleDay}日` : '月结'
  if (settleType === 1) return '次结'
  return '未设置'
}

function invoiceConfig(isInvoice?: number) {
  if (isInvoice === 1) return { text: '开票', color: 'blue' }
  if (isInvoice === 2) return { text: '不开票', color: 'default' }
  return { text: '未设置', color: 'default' }
}

const priorityConfig: Record<number, { text: string; className: string }> = {
  1: { text: '普通', className: 'normal' },
  2: { text: '加急', className: 'urgent' },
  3: { text: '特急', className: 'critical' },
}
