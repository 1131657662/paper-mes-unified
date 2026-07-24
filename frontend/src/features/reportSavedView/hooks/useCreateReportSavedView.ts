import { message } from 'antd'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { notifyErrorOnce } from '../../../api/request'
import { queries } from '../../../queries'
import { reportSavedViewService } from '../services/reportSavedViewService'

export function useCreateReportSavedView() {
  const client = useQueryClient()
  return useMutation({ mutationFn: reportSavedViewService.create,
    onSuccess: () => { message.success('保存视图已创建'); void client.invalidateQueries({ queryKey: queries.reportSavedView._def }) },
    onError: (error) => notifyErrorOnce(error, '保存视图失败，请检查名称和默认设置'), })
}
