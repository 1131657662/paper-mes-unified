import { useState } from 'react'
import { message } from 'antd'
import { useNavigate } from 'react-router-dom'
import { exportDeliveryOrder, exportDeliveryOrderList } from '../../api/delivery'
import type { DeliveryOrder, DeliveryQuery } from '../../types/delivery'
import { deliveryStatus, type DeliveryQueueFilter } from './deliveryListModel'

interface Options {
  filters: DeliveryQuery
  queue: DeliveryQueueFilter
  selected?: DeliveryOrder
}

export function useDeliveryExportActions(options: Options) {
  const navigate = useNavigate()
  const [exportingList, setExportingList] = useState(false)
  return {
    exportingList,
    print: () => printSelected(options.selected, navigate),
    exportSelected: () => exportSelected(options.selected),
    exportList: () => exportList(options, setExportingList),
  }
}

function printSelected(selected: DeliveryOrder | undefined, navigate: ReturnType<typeof useNavigate>) {
  if (!selected) return void message.warning('请选择一张出库单打印')
  navigate(`/delivery-orders/${selected.uuid}?print=1`)
}

async function exportSelected(selected?: DeliveryOrder) {
  if (!selected) return void message.warning('请选择一张出库单导出')
  await exportDeliveryOrder({ documentNo: selected.deliveryNo, uuid: selected.uuid })
}

async function exportList(options: Options, setLoading: (value: boolean) => void) {
  setLoading(true)
  try {
    await exportDeliveryOrderList({ ...options.filters, deliveryStatus: deliveryStatus(options.queue) })
    message.success('出库对账已导出')
  } finally {
    setLoading(false)
  }
}
