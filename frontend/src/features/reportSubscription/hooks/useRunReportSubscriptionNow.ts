import { message } from 'antd'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { notifyErrorOnce } from '../../../api/request'
import { queries } from '../../../queries'
import { reportSubscriptionService } from '../services/reportSubscriptionService'

export function useRunReportSubscriptionNow() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: reportSubscriptionService.runNow,
    onSuccess: () => {
      message.success('订阅试跑已派发，请在下载任务中心查看文件生成进度')
      void queryClient.invalidateQueries({ queryKey: queries.reportSubscription._def })
    },
    onError: (error) => notifyErrorOnce(error, '订阅试跑失败'),
  })
}
