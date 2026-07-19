import { message } from 'antd'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { notifyErrorOnce } from '../../../api/request'
import { exportTaskKeys } from '../../exportTask/queries/exportTaskKeys'
import { deliveryInventoryService } from '../services/deliveryInventoryService'

export function useExportDeliveryInventory() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: deliveryInventoryService.export,
    onSuccess: () => {
      message.success('导出任务已提交，可在下载任务中心查看')
      void queryClient.invalidateQueries({ queryKey: exportTaskKeys._def })
    },
    onError: (error) => notifyErrorOnce(error, '成品库存导出任务提交失败，请重试'),
  })
}
