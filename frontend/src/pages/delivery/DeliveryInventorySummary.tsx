import { ClockCircleOutlined, InboxOutlined, LockOutlined, SendOutlined, TeamOutlined } from '@ant-design/icons'
import { Tooltip } from 'antd'
import { formatTon } from '../../features/delivery/utils/deliveryFormatters'
import type { DeliveryInventorySummary as InventorySummary } from '../../types/deliveryInventory'

export default function DeliveryInventorySummary({ summary }: { summary?: InventorySummary }) {
  return (
    <div className="delivery-inventory-summary">
      <Metric icon={<TeamOutlined />} label="库存客户" value={summary ? `${summary.customerCount} 家` : '-'} />
      <Metric icon={<InboxOutlined />} label="实际在库" value={summary ? `${summary.totalRollCount} 卷` : '-'} sub={inventoryComposition(summary)} />
      <Metric icon={<SendOutlined />} label="可出库" value={summary ? `${summary.availableRollCount} 卷` : '-'} sub={formatTon(summary?.availableWeight)} tone="success" />
      <Metric icon={<LockOutlined />} label="锁定卷库存" value={summary ? `${summary.lockedRollCount} 卷` : '-'} sub={formatTon(summary?.lockedWeight)} tone="warning" />
      <Metric icon={<ClockCircleOutlined />} label="计划出库" value={formatTon(summary?.plannedOutWeight)} sub={summary?.stockInTimeUnknownCount ? `${summary.stockInTimeUnknownCount} 卷缺少入库时间` : '库龄数据完整'} tone="primary" />
    </div>
  )
}

function Metric({ icon, label, sub, tone, value }: { icon: React.ReactNode; label: string; sub?: string; tone?: string; value: string }) {
  return (
    <div className="delivery-inventory-summary__metric" data-tone={tone}>
      <span className="delivery-inventory-summary__label">{icon}{label}</span><strong>{value}</strong>{sub && <Tooltip title={sub}><em>{sub}</em></Tooltip>}
    </div>
  )
}

function inventoryComposition(summary?: InventorySummary) {
  if (!summary) return '-'
  const weight = formatTon(summary.totalWeight)
  return `${weight} · 成品 ${summary.productRollCount} / 余料 ${summary.remainRollCount} / 直发 ${summary.directRollCount}`
}
