import { message } from 'antd'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { notifyErrorOnce } from '../../../api/request'
import { queries } from '../../../queries'
import { deliveryInventoryService } from '../services/deliveryInventoryService'

export function useAssignDeliveryInventoryWarehouse() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: deliveryInventoryService.assignWarehouse,
    onSuccess: (result) => {
      message.success(`已为 ${result.repairedRollCount} 卷历史成品补录仓库`)
      void queryClient.invalidateQueries({ queryKey: queries.delivery._def })
    },
    onError: (error) => notifyErrorOnce(error, '历史库存补仓失败，请刷新后重试'),
  })
}
