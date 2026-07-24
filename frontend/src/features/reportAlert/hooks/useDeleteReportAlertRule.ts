import { message } from 'antd'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { notifyErrorOnce } from '../../../api/request'
import { queries } from '../../../queries'
import { reportAlertService } from '../services/reportAlertService'

export function useDeleteReportAlertRule() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: reportAlertService.deleteRule,
    onSuccess: () => {
      message.success('阈值规则已删除')
      void queryClient.invalidateQueries({ queryKey: queries.reportAlert._def })
    },
    onError: (error) => notifyErrorOnce(error, '阈值规则删除失败'),
  })
}
