import { availableFinishWeight } from '../../features/delivery/utils/deliveryFormatters'
import type { AvailableFinishVO } from '../../types/delivery'

export interface DeliveryLineEdit {
  outWeight?: number
  remark?: string
}

export interface DeliverySelectionSummary {
  totalCount: number
  totalWeight: number
  productCount: number
  productWeight: number
  remainCount: number
  remainWeight: number
  riskCount: number
}

export interface DeliveryWeightFeedback {
  maxWeight: number
  message?: string
  status?: 'error' | 'warning'
  value: number
}

export function selectedDeliveryFinishes(
  finishes: AvailableFinishVO[],
  selectedRowKeys: React.Key[],
): AvailableFinishVO[] {
  const selectedKeys = new Set(selectedRowKeys.map(String))
  return finishes.filter((item) => selectedKeys.has(item.finishUuid))
}

export function summarizeDeliverySelection(
  items: AvailableFinishVO[],
  edits: Record<string, DeliveryLineEdit>,
): DeliverySelectionSummary {
  const products = items.filter((item) => item.isRemain !== 1)
  const remains = items.filter((item) => item.isRemain === 1)
  return {
    totalCount: items.length,
    totalWeight: selectionWeight(items, edits),
    productCount: products.length,
    productWeight: selectionWeight(products, edits),
    remainCount: remains.length,
    remainWeight: selectionWeight(remains, edits),
    riskCount: items.filter((item) => item.settlementRisk).length,
  }
}

export function deliveryWeightFeedback(
  item: AvailableFinishVO,
  edit?: DeliveryLineEdit,
): DeliveryWeightFeedback {
  const maxWeight = availableFinishWeight(item)
  const value = edit?.outWeight ?? maxWeight
  if (value <= 0) return { maxWeight, value, status: 'error', message: '重量需大于 0' }
  if (value > maxWeight) return { maxWeight, value, status: 'error', message: '超过可出库重量' }
  if (value < maxWeight) return { maxWeight, value, status: 'warning', message: '部分出库' }
  return { maxWeight, value }
}

export function deliverySelectionError(
  items: AvailableFinishVO[],
  edits: Record<string, DeliveryLineEdit>,
): string | undefined {
  for (const item of items) {
    const feedback = deliveryWeightFeedback(item, edits[item.finishUuid])
    if (feedback.status === 'error') {
      return `${item.finishRollNo}：${feedback.message}`
    }
  }
  return undefined
}

function selectionWeight(
  items: AvailableFinishVO[],
  edits: Record<string, DeliveryLineEdit>,
): number {
  return items.reduce(
    (total, item) => total + (edits[item.finishUuid]?.outWeight ?? availableFinishWeight(item)),
    0,
  )
}
