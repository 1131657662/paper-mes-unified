import { message } from 'antd'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { notifyErrorOnce } from '../../../api/request'
import { queries } from '../../../queries'
import { reportSubscriptionService } from '../services/reportSubscriptionService'

export function useDeleteReportSubscription() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: reportSubscriptionService.delete,
    onSuccess: () => {
      message.success('报表订阅已删除')
      void queryClient.invalidateQueries({ queryKey: queries.reportSubscription._def })
    },
    onError: (error) => notifyErrorOnce(error, '报表订阅删除失败'),
  })
}
