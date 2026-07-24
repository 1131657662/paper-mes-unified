import { message } from 'antd'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { notifyErrorOnce } from '../../../api/request'
import { queries } from '../../../queries'
import { reportSubscriptionService } from '../services/reportSubscriptionService'

export function useRetryReportSubscriptionRun() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: reportSubscriptionService.retryRun,
    onSuccess: () => {
      message.success('订阅运行已重新派发')
      void queryClient.invalidateQueries({ queryKey: queries.reportSubscription._def })
    },
    onError: (error) => notifyErrorOnce(error, '订阅运行重试失败'),
  })
}
