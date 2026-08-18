import { message } from 'antd'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { notifyErrorOnce } from '../../../api/request'
import { queries } from '../../../queries'
import { processAiService } from '../services/processAiService'
import type { ProcessAiManagedProvider } from '../types'

export interface UpdateProcessAiProviderKeyInput {
  provider: ProcessAiManagedProvider
  apiKey: string
}

export function useUpdateProcessAiProviderKey() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ provider, apiKey }: UpdateProcessAiProviderKeyInput) =>
      processAiService.updateProviderKey(provider, apiKey),
    onSuccess: (settings) => {
      const provider = settings.provider.toLowerCase() as ProcessAiManagedProvider
      queryClient.setQueryData(queries.processAi.providerSettings(provider).queryKey, settings)
      void queryClient.invalidateQueries({ queryKey: queries.processAi.status.queryKey })
      message.success(`${provider === 'zhipu' ? 'GLM' : 'DeepSeek'} API Key 已安全保存`)
    },
    onError: (error) => notifyErrorOnce(error, 'DeepSeek API Key 保存失败'),
  })
}
