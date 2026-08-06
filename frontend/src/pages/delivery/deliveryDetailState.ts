import type { DeliveryOrder } from '../../types/delivery'

export type DeliverySignState = 'PENDING' | 'SIGNED' | 'NOT_REQUIRED' | 'UNKNOWN'
export type DeliveryStockState = 'LOCKED' | 'DEDUCTED' | 'RELEASED' | 'UNKNOWN'

export interface DeliveryOverviewState {
  signState: DeliverySignState
  stockState: DeliveryStockState
  canSign: boolean
  canEdit: boolean
}

export function resolveDeliveryOverview(order: DeliveryOrder): DeliveryOverviewState {
  if (order.signState && order.stockState && order.canSign !== undefined && order.canEdit !== undefined) {
    return { signState: order.signState, stockState: order.stockState, canSign: order.canSign, canEdit: order.canEdit }
  }
  if (order.deliveryStatus === 1) return { signState: 'PENDING', stockState: 'LOCKED', canSign: true, canEdit: true }
  if (order.deliveryStatus === 2) return { signState: 'SIGNED', stockState: 'DEDUCTED', canSign: false, canEdit: false }
  if (order.deliveryStatus === 3) return { signState: 'NOT_REQUIRED', stockState: 'RELEASED', canSign: false, canEdit: false }
  return { signState: 'UNKNOWN', stockState: 'UNKNOWN', canSign: false, canEdit: false }
}

export function deliverySignLabel(state: DeliverySignState): string {
  if (state === 'PENDING') return '待签收'
  if (state === 'SIGNED') return '已签收'
  if (state === 'NOT_REQUIRED') return '无需签收'
  return '状态未知'
}

export function deliveryStockLabel(state: DeliveryStockState): string {
  if (state === 'LOCKED') return '实物已锁定'
  if (state === 'DEDUCTED') return '库存已扣减'
  if (state === 'RELEASED') return '库存已释放'
  return '状态未知'
}
