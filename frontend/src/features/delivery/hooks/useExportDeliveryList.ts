import { message } from 'antd'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { notifyErrorOnce } from '../../../api/request'
import { createDeliveryReconciliationExportTask } from '../../../api/exportTask'
import { exportTaskKeys } from '../../exportTask/queries/exportTaskKeys'
import type { DeliveryQuery } from '../../../types/delivery'

export function useExportDeliveryList() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (query: DeliveryQuery) => createDeliveryReconciliationExportTask(query),
    onSuccess: () => {
      message.success('出库对账导出任务已提交，可在下载任务中心查看')
      void queryClient.invalidateQueries({ queryKey: exportTaskKeys._def })
    },
    onError: (error) => notifyErrorOnce(error, '出库对账导出任务提交失败，请重试'),
  })
}
