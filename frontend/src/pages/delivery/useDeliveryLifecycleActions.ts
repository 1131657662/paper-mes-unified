import { useState } from 'react'
import { message } from 'antd'
import { rollbackDeliveryOrder } from '../../api/delivery'
import { useCancelPendingDelivery } from '../../features/delivery/hooks/useCancelPendingDelivery'
import type { DeliveryOrder } from '../../types/delivery'
import { askDeliveryCancelReason } from './deliveryDetailDialogs'
import { askRollbackReason } from './deliveryListDialogs'

interface Options {
  canManage: boolean
  clearSelection: () => void
  refetch: () => void
  selected?: DeliveryOrder
}

export function useDeliveryLifecycleActions(options: Options) {
  const cancelMutation = useCancelPendingDelivery()
  const [rollingBack, setRollingBack] = useState(false)
  return {
    cancelLoading: cancelMutation.isPending,
    rollingBack,
    cancel: () => cancelSelected(options, cancelMutation.mutateAsync),
    rollback: () => rollbackSelected(options, setRollingBack),
  }
}

async function cancelSelected(options: Options,
  mutate: ReturnType<typeof useCancelPendingDelivery>['mutateAsync']) {
  const selected = options.selected
  if (!options.canManage || !selected || selected.deliveryStatus !== 1) return
  const reason = await askDeliveryCancelReason(selected.deliveryNo)
  if (!reason) return
  await mutate({ uuid: selected.uuid, data: { reason } })
  message.success('待出库单已作废，成品库存已释放')
  options.clearSelection()
}

async function rollbackSelected(options: Options, setLoading: (value: boolean) => void) {
  const selected = options.selected
  if (!options.canManage || !selected || selected.deliveryStatus !== 2) return
  const reason = await askRollbackReason(selected.deliveryNo)
  if (!reason) return
  setLoading(true)
  try {
    await rollbackDeliveryOrder(selected.uuid, { reason })
    message.success('已回退为待出库，可继续改单')
    options.clearSelection()
    options.refetch()
  } finally {
    setLoading(false)
  }
}
