import { availableFinishWeight } from '../../features/delivery/utils/deliveryFormatters'
import type { AvailableFinishVO } from '../../types/delivery'
import type { DeliveryLineEdit } from './deliverySelectionModel'

export interface DeliverySelectionReviewGroup {
  key: string
  orderDate?: string
  orderNo: string
  items: AvailableFinishVO[]
  totalWeight: number
}

export function buildDeliverySelectionReviewGroups(
  items: AvailableFinishVO[],
  edits: Record<string, DeliveryLineEdit>,
): DeliverySelectionReviewGroup[] {
  const grouped = new Map<string, AvailableFinishVO[]>()
  for (const item of items) {
    const current = grouped.get(item.orderUuid) ?? []
    current.push(item)
    grouped.set(item.orderUuid, current)
  }
  return Array.from(grouped.entries())
    .map(([orderUuid, groupItems]) => buildReviewGroup(orderUuid, groupItems, edits))
    .sort(compareReviewGroups)
}

function buildReviewGroup(
  orderUuid: string,
  items: AvailableFinishVO[],
  edits: Record<string, DeliveryLineEdit>,
): DeliverySelectionReviewGroup {
  const first = items[0]
  return {
    key: orderUuid,
    orderDate: first?.orderDate,
    orderNo: first?.orderNo ?? '-',
    items,
    totalWeight: items.reduce((total, item) => (
      total + (edits[item.finishUuid]?.outWeight ?? availableFinishWeight(item))
    ), 0),
  }
}

function compareReviewGroups(
  left: DeliverySelectionReviewGroup,
  right: DeliverySelectionReviewGroup,
): number {
  const dateOrder = (right.orderDate ?? '').localeCompare(left.orderDate ?? '')
  return dateOrder || right.orderNo.localeCompare(left.orderNo, 'zh-CN', { numeric: true })
}
