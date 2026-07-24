import { message } from 'antd'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { notifyErrorOnce } from '../../../api/request'
import { queries } from '../../../queries'
import { reportSubscriptionService } from '../services/reportSubscriptionService'

export function useUpdateReportSubscription() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: reportSubscriptionService.update,
    onSuccess: () => {
      message.success('报表订阅已更新')
      void queryClient.invalidateQueries({ queryKey: queries.reportSubscription._def })
    },
    onError: (error) => notifyErrorOnce(error, '报表订阅更新失败'),
  })
}
