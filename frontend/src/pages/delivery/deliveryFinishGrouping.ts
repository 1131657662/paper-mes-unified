import { availableFinishWeight } from '../../features/delivery/utils/deliveryFormatters'
import type { AvailableFinishVO } from '../../types/delivery'
import type { DeliveryFinishScope } from './deliveryFinishScope'
import type { DeliveryLineEdit } from './deliverySelectionModel'

export interface DeliveryFinishRow extends AvailableFinishVO {
  key: string
  rowType: 'finish'
}

export interface DeliveryOrderGroupRow {
  key: string
  rowType: 'group'
  orderUuid: string
  orderNo: string
  orderDate?: string
  scope: DeliveryFinishScope
  totalCount: number
  totalWeight: number
  selectedCount: number
  selectedWeight: number
  riskCount: number
  children: DeliveryFinishRow[]
}

export type DeliverySelectionTableRow = DeliveryFinishRow | DeliveryOrderGroupRow

export function buildDeliveryOrderGroups(
  visibleFinishes: AvailableFinishVO[],
  scope: DeliveryFinishScope,
  selectedRowKeys: React.Key[],
  edits: Record<string, DeliveryLineEdit>,
): DeliveryOrderGroupRow[] {
  const grouped = new Map<string, AvailableFinishVO[]>()
  for (const finish of visibleFinishes) {
    const current = grouped.get(finish.orderUuid) ?? []
    current.push(finish)
    grouped.set(finish.orderUuid, current)
  }
  const selectedKeys = new Set(selectedRowKeys.map(String))
  return Array.from(grouped.entries())
    .map(([orderUuid, items]) => buildGroup(
      orderUuid,
      items,
      scope,
      selectedKeys,
      edits,
    ))
    .sort(compareGroups)
}

export function isDeliveryGroupRow(
  row: DeliverySelectionTableRow,
): row is DeliveryOrderGroupRow {
  return row.rowType === 'group'
}

function buildGroup(
  orderUuid: string,
  items: AvailableFinishVO[],
  scope: DeliveryFinishScope,
  selectedKeys: Set<string>,
  edits: Record<string, DeliveryLineEdit>,
): DeliveryOrderGroupRow {
  const first = items[0]
  const selectedItems = items.filter((item) => selectedKeys.has(item.finishUuid))
  const children = items.map<DeliveryFinishRow>((item) => ({
    ...item,
    key: item.finishUuid,
    rowType: 'finish',
  }))
  return {
    key: `delivery-order-group:${orderUuid}`,
    rowType: 'group',
    orderUuid,
    orderNo: first?.orderNo ?? '-',
    orderDate: first?.orderDate,
    scope,
    totalCount: items.length,
    totalWeight: availableWeight(items),
    selectedCount: selectedItems.length,
    selectedWeight: editedWeight(selectedItems, edits),
    riskCount: items.filter((item) => item.settlementRisk).length,
    children,
  }
}

function availableWeight(items: AvailableFinishVO[]) {
  return items.reduce((total, item) => total + availableFinishWeight(item), 0)
}

function editedWeight(items: AvailableFinishVO[], edits: Record<string, DeliveryLineEdit>) {
  return items.reduce(
    (total, item) => total + (edits[item.finishUuid]?.outWeight ?? availableFinishWeight(item)),
    0,
  )
}

function compareGroups(left: DeliveryOrderGroupRow, right: DeliveryOrderGroupRow): number {
  const dateOrder = (right.orderDate ?? '').localeCompare(left.orderDate ?? '')
  return dateOrder || right.orderNo.localeCompare(left.orderNo, 'zh-CN', { numeric: true })
}
