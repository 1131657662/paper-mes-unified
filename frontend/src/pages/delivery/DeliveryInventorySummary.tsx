import { ClockCircleOutlined, InboxOutlined, LockOutlined, SendOutlined, TeamOutlined } from '@ant-design/icons'
import { Tooltip } from 'antd'
import type { ReactNode } from 'react'
import { formatTon } from '../../features/delivery/utils/deliveryFormatters'
import type { DeliveryInventorySummary as InventorySummary } from '../../types/deliveryInventory'
import type { DeliveryInventoryQuickFilter } from './deliveryInventoryModel'

export default function DeliveryInventorySummary({ inventoryProductLabel = '成品（含直发）', inventoryScope = 'all', summary }: { inventoryProductLabel?: string; inventoryScope?: DeliveryInventoryQuickFilter; summary?: InventorySummary }) {
  return (
    <div className="delivery-inventory-summary">
      <Metric icon={<TeamOutlined />} label="库存客户" value={summary ? `${summary.customerCount} 家` : '-'} />
      <Metric icon={<InboxOutlined />} label="实际在库" value={summary ? `${summary.totalRollCount} 卷` : '-'} sub={<InventoryBreakdown inventoryProductLabel={inventoryProductLabel} inventoryScope={inventoryScope} summary={summary} />} />
      <Metric icon={<SendOutlined />} label="可出库" value={summary ? `${summary.availableRollCount} 卷` : '-'} sub={<InventoryBreakdown available inventoryProductLabel={inventoryProductLabel} inventoryScope={inventoryScope} summary={summary} />} tone="success" />
      <Metric icon={<LockOutlined />} label="锁定卷库存" value={summary ? `${summary.lockedRollCount} 卷` : '-'} sub={formatTon(summary?.lockedWeight)} tone="warning" />
      <Metric icon={<ClockCircleOutlined />} label="计划出库" value={formatTon(summary?.plannedOutWeight)} sub={summary?.stockInTimeUnknownCount ? `${summary.stockInTimeUnknownCount} 卷缺少入库时间` : '库龄数据完整'} tone="primary" />
    </div>
  )
}

function Metric({ icon, label, sub, tone, value }: { icon: ReactNode; label: string; sub?: ReactNode; tone?: string; value: string }) {
  return (
    <div className="delivery-inventory-summary__metric" data-tone={tone}>
      <span className="delivery-inventory-summary__label">{icon}{label}</span><strong>{value}</strong>{sub && <Tooltip title={sub}><div className="delivery-inventory-summary__sub">{sub}</div></Tooltip>}
    </div>
  )
}

function InventoryBreakdown({ available = false, inventoryProductLabel, inventoryScope, summary }: { available?: boolean; inventoryProductLabel: string; inventoryScope: DeliveryInventoryQuickFilter; summary?: InventorySummary }) {
  if (!summary) return <span>-</span>
  const productCount = available ? summary.productAvailableRollCount : summary.productRollCount
  const productWeight = available ? summary.productAvailableWeight : summary.productWeight
  const remainCount = available ? summary.remainAvailableRollCount : summary.remainRollCount
  const remainWeight = available ? summary.remainAvailableWeight : summary.remainWeight
  const directCount = available ? summary.directAvailableRollCount : summary.directRollCount
  const directWeight = available ? summary.directAvailableWeight : summary.directWeight
  const showProduct = inventoryScope !== 'remain' && inventoryScope !== 'direct'
  const showRemain = inventoryScope === 'all' || inventoryScope === 'remain'
  const showDirect = inventoryScope === 'direct'
  return <span className="delivery-inventory-summary__breakdown">
    {showProduct && <BreakdownLine label={compactProductLabel(inventoryProductLabel)} title={inventoryProductLabel} count={productCount} weight={productWeight} />}
    {showRemain && <BreakdownLine label="余料" count={remainCount} weight={remainWeight} />}
    {showDirect && <BreakdownLine label="原纸直发" count={directCount} weight={directWeight} />}
    {!showDirect && directCount > 0 && <small>原纸直发 {directCount} 卷 / {formatTon(directWeight)}（已计入成品）</small>}
  </span>
}

function compactProductLabel(label: string) {
  return label === '成品（含直发）' ? '成品' : label
}

function BreakdownLine({ count, label, title, weight }: { count: number; label: string; title?: string; weight: number }) {
  return <span className="delivery-inventory-summary__breakdown-line" title={title}><span>{label}</span><strong>{count} 卷</strong><em>{formatTon(weight)}</em></span>
}
