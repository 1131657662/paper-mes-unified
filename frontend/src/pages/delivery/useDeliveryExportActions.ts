import { message } from 'antd'
import { useNavigate } from 'react-router-dom'
import { useExportDeliveryList } from '../../features/delivery/hooks/useExportDeliveryList'
import { useCreateDeliveryOrderExportTask } from '../../features/exportTask/hooks/useCreateDeliveryOrderExportTask'
import type { DeliveryOrder, DeliveryQuery } from '../../types/delivery'
import { deliveryStatus, type DeliveryQueueFilter } from './deliveryListModel'

interface Options {
  filters: DeliveryQuery
  queue: DeliveryQueueFilter
  selected?: DeliveryOrder
}

export function useDeliveryExportActions(options: Options) {
  const navigate = useNavigate()
  const exportListMutation = useExportDeliveryList()
  const exportSelectedMutation = useCreateDeliveryOrderExportTask()
  const exportSelected = async () => {
    if (!options.selected) return void message.warning('请选择一张出库单导出')
    await exportSelectedMutation.mutateAsync({ uuid: options.selected.uuid })
    message.success('已加入导出任务，可在右上角下载任务中心查看')
  }
  return {
    exportingList: exportListMutation.isPending,
    exportingSelected: exportSelectedMutation.isPending,
    print: () => printSelected(options.selected, navigate),
    exportSelected,
    exportList: () => exportListMutation.mutate({
      ...options.filters,
      deliveryStatus: deliveryStatus(options.queue),
    }),
  }
}

function printSelected(selected: DeliveryOrder | undefined, navigate: ReturnType<typeof useNavigate>) {
  if (!selected) return void message.warning('请选择一张出库单打印')
  navigate(`/delivery-orders/${selected.uuid}?print=1`)
}
