import { message } from 'antd'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { notifyErrorOnce } from '../../../api/request'
import { queries } from '../../../queries'
import { processAiService } from '../services/processAiService'

export function useDeleteProcessAiProviderKey() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: processAiService.deleteProviderKey,
    onSuccess: (settings) => {
      queryClient.setQueryData(queries.processAi.providerSettings.queryKey, settings)
      void queryClient.invalidateQueries({ queryKey: queries.processAi.status.queryKey })
      message.success(settings.configured ? '已改用服务器环境变量中的密钥' : '数据库密钥已移除')
    },
    onError: (error) => notifyErrorOnce(error, '数据库密钥移除失败'),
  })
}
