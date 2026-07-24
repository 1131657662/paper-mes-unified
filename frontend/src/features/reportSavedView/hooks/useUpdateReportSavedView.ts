import { message } from 'antd'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { notifyErrorOnce } from '../../../api/request'
import { queries } from '../../../queries'
import { reportSavedViewService } from '../services/reportSavedViewService'

export function useUpdateReportSavedView() {
  const client = useQueryClient()
  return useMutation({ mutationFn: reportSavedViewService.update,
    onSuccess: () => { message.success('保存视图已更新'); void client.invalidateQueries({ queryKey: queries.reportSavedView._def }) },
    onError: (error) => notifyErrorOnce(error, '保存视图更新失败，可能已被其他人修改'), })
}
