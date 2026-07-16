import { message } from 'antd'
import { useBatchConfirmDelivery } from '../../features/delivery/hooks/useBatchConfirmDelivery'
import { useConfirmDelivery } from '../../features/delivery/hooks/useConfirmDelivery'
import type { DeliveryOrder } from '../../types/delivery'
import { askSignUser, confirmBatchSign } from './deliveryListDialogs'

interface Options {
  canManage: boolean
  clearSelection: () => void
  selectedRows: DeliveryOrder[]
}

export function useDeliveryConfirmActions(options: Options) {
  const confirmMutation = useConfirmDelivery()
  const batchConfirmMutation = useBatchConfirmDelivery()
  const pending = options.selectedRows.filter((record) => record.deliveryStatus === 1)
  return {
    confirmLoading: confirmMutation.isPending,
    batchConfirmLoading: batchConfirmMutation.isPending,
    selectedPendingCount: pending.length,
    confirm: (record: DeliveryOrder) => confirmOne(record, options, confirmMutation.mutateAsync),
    confirmBatch: () => confirmBatch(pending, options, batchConfirmMutation.mutateAsync),
  }
}

async function confirmOne(record: DeliveryOrder, options: Options, mutate: ReturnType<typeof useConfirmDelivery>['mutateAsync']) {
  if (!options.canManage) return
  const signUser = await askSignUser(record)
  if (signUser === null) return
  await mutate({ uuid: record.uuid, data: signUser ? { signUser } : undefined })
  message.success('出库签收完成')
  options.clearSelection()
}

async function confirmBatch(records: DeliveryOrder[], options: Options,
  mutate: ReturnType<typeof useBatchConfirmDelivery>['mutateAsync']) {
  if (!options.canManage) return
  if (records.length === 0) return void message.warning('请先选择待出库单据')
  if (!await confirmBatchSign(records.length)) return
  await mutate({ deliveryUuids: records.map((record) => record.uuid) })
  message.success(`已签收 ${records.length} 张出库单`)
  options.clearSelection()
}
