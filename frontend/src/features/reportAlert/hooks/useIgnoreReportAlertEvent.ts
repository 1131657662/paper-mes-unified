import { message } from 'antd'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { notifyErrorOnce } from '../../../api/request'
import { queries } from '../../../queries'
import { reportAlertService } from '../services/reportAlertService'

export function useIgnoreReportAlertEvent() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: reportAlertService.ignoreEvent,
    onSuccess: () => {
      message.success('预警已忽略')
      void queryClient.invalidateQueries({ queryKey: queries.reportAlert.events._def })
    },
    onError: (error) => notifyErrorOnce(error, '忽略预警失败'),
  })
}
