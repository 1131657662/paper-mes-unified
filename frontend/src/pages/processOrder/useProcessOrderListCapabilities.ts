import { PERMISSIONS } from '../../constants/permissions'
import { useHasPermission } from '../../stores/authStore'

export interface ProcessOrderListCapabilities {
  canBackRecord: boolean
  canCreateOrder: boolean
  canManageDelivery: boolean
  canManageOrder: boolean
  canManageSettlement: boolean
}

export function useProcessOrderListCapabilities(): ProcessOrderListCapabilities {
  return {
    canBackRecord: useHasPermission(PERMISSIONS.orderBackRecord),
    canCreateOrder: useHasPermission(PERMISSIONS.orderCreate),
    canManageDelivery: useHasPermission(PERMISSIONS.deliveryManage),
    canManageOrder: useHasPermission(PERMISSIONS.orderManage),
    canManageSettlement: useHasPermission(PERMISSIONS.settleManage),
  }
}
