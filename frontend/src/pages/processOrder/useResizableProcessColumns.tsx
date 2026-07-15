import type { ProColumns } from '@ant-design/pro-components'
import { useResizableTableColumns } from '../../components/useResizableTableColumns'
import type { ProcessOrder } from '../../types/processOrder'

export function useResizableProcessColumns(columns: ProColumns<ProcessOrder>[]) {
  return useResizableTableColumns<ProcessOrder, ProColumns<ProcessOrder>>(columns, 'process-order-v2')
}
