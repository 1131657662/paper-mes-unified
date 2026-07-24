import { useHasPermission } from '../../stores/authStore'
import { PERMISSIONS } from '../../constants/permissions'
import type { DeliveryOrder, DeliveryQuery } from '../../types/delivery'
import type { DeliveryQueueFilter } from './deliveryListModel'
import { useDeliveryConfirmActions } from './useDeliveryConfirmActions'
import { useDeliveryExportActions } from './useDeliveryExportActions'
import { useDeliveryLifecycleActions } from './useDeliveryLifecycleActions'

interface Options {
  clearSelection: () => void
  filters: DeliveryQuery
  queue: DeliveryQueueFilter
  refetch: () => void
  selectedRows: DeliveryOrder[]
}

export function useDeliveryListActions(options: Options) {
  const canManage = useHasPermission(PERMISSIONS.deliveryManage)
  const canRelease = useHasPermission(PERMISSIONS.deliveryRelease)
  const canConfirm = canManage || canRelease
  const selected = options.selectedRows.length === 1 ? options.selectedRows[0] : undefined
  const shared = { canManage, clearSelection: options.clearSelection }
  const confirm = useDeliveryConfirmActions({ canConfirm, canRelease, clearSelection: options.clearSelection,
    selectedRows: options.selectedRows })
  const lifecycle = useDeliveryLifecycleActions({ ...shared, refetch: options.refetch, selected })
  const exports = useDeliveryExportActions({ filters: options.filters, queue: options.queue, selected })
  return {
    canManage,
    canConfirm,
    clearSelection: options.clearSelection,
    selected,
    selectedCount: options.selectedRows.length,
    ...confirm,
    ...lifecycle,
    ...exports,
  }
}

export type DeliveryListActions = ReturnType<typeof useDeliveryListActions>
