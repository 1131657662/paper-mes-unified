import { message } from 'antd'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { notifyErrorOnce } from '../../../api/request'
import { queries } from '../../../queries'
import { reportSavedViewService } from '../services/reportSavedViewService'

export function useDeleteReportSavedView() {
  const client = useQueryClient()
  return useMutation({ mutationFn: reportSavedViewService.delete,
    onSuccess: () => { message.success('保存视图已删除'); void client.invalidateQueries({ queryKey: queries.reportSavedView._def }) },
    onError: (error) => notifyErrorOnce(error, '保存视图删除失败，请刷新后重试'), })
}
