import { message } from 'antd'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { notifyErrorOnce } from '../../../api/request'
import { queries } from '../../../queries'
import { reportAlertService } from '../services/reportAlertService'

export function useCreateReportAlertRule() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: reportAlertService.createRule,
    onSuccess: () => {
      message.success('阈值规则已创建')
      void queryClient.invalidateQueries({ queryKey: queries.reportAlert._def })
    },
    onError: (error) => notifyErrorOnce(error, '阈值规则创建失败'),
  })
}
