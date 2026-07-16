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
  const selected = options.selectedRows.length === 1 ? options.selectedRows[0] : undefined
  const shared = { canManage, clearSelection: options.clearSelection }
  const confirm = useDeliveryConfirmActions({ ...shared, selectedRows: options.selectedRows })
  const lifecycle = useDeliveryLifecycleActions({ ...shared, refetch: options.refetch, selected })
  const exports = useDeliveryExportActions({ filters: options.filters, queue: options.queue, selected })
  return {
    canManage,
    clearSelection: options.clearSelection,
    selected,
    selectedCount: options.selectedRows.length,
    ...confirm,
    ...lifecycle,
    ...exports,
  }
}

export type DeliveryListActions = ReturnType<typeof useDeliveryListActions>
