import { message } from 'antd'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { notifyErrorOnce } from '../../../api/request'
import { exportTaskKeys } from '../../exportTask/queries/exportTaskKeys'
import type { ReportQuery } from '../../../types/report'
import { reportService } from '../services/reportService'

export function useExportReport() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (query: ReportQuery) => reportService.export(query),
    onSuccess: () => {
      message.success('统计报表导出任务已提交，可在下载任务中心查看')
      void queryClient.invalidateQueries({ queryKey: exportTaskKeys._def })
    },
    onError: (error) => notifyErrorOnce(error, '统计报表导出任务提交失败，请重试'),
  })
}
