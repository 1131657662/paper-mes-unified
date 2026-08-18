import { message } from 'antd'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { notifyErrorOnce } from '../../../api/request'
import { queries } from '../../../queries'
import { processAiService } from '../services/processAiService'
import type { ProcessAiManagedProvider } from '../types'

export interface DeleteProcessAiProviderKeyInput {
  provider: ProcessAiManagedProvider
}

export function useDeleteProcessAiProviderKey() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ provider }: DeleteProcessAiProviderKeyInput) =>
      processAiService.deleteProviderKey(provider),
    onSuccess: (settings) => {
      const provider = settings.provider.toLowerCase() as ProcessAiManagedProvider
      queryClient.setQueryData(queries.processAi.providerSettings(provider).queryKey, settings)
      void queryClient.invalidateQueries({ queryKey: queries.processAi.status.queryKey })
      message.success(settings.configured ? '已改用服务器环境变量中的密钥' : '数据库密钥已移除')
    },
    onError: (error) => notifyErrorOnce(error, '数据库密钥移除失败'),
  })
}
