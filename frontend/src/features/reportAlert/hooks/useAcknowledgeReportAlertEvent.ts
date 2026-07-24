import { message } from 'antd'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { notifyErrorOnce } from '../../../api/request'
import { queries } from '../../../queries'
import { reportAlertService } from '../services/reportAlertService'

export function useAcknowledgeReportAlertEvent() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: reportAlertService.acknowledgeEvent,
    onSuccess: () => {
      message.success('预警已确认')
      void queryClient.invalidateQueries({ queryKey: queries.reportAlert.events._def })
    },
    onError: (error) => notifyErrorOnce(error, '确认预警失败'),
  })
}
