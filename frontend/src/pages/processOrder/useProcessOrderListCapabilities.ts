import { useMemo } from 'react'
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
  const canBackRecord = useHasPermission(PERMISSIONS.orderBackRecord)
  const canCreateOrder = useHasPermission(PERMISSIONS.orderCreate)
  const canManageDelivery = useHasPermission(PERMISSIONS.deliveryManage)
  const canManageOrder = useHasPermission(PERMISSIONS.orderManage)
  const canManageSettlement = useHasPermission(PERMISSIONS.settleManage)

  return useMemo(() => ({
    canBackRecord,
    canCreateOrder,
    canManageDelivery,
    canManageOrder,
    canManageSettlement,
  }), [canBackRecord, canCreateOrder, canManageDelivery, canManageOrder, canManageSettlement])
}
