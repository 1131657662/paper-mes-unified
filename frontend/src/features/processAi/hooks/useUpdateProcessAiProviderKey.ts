import { message } from 'antd'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { notifyErrorOnce } from '../../../api/request'
import { queries } from '../../../queries'
import { processAiService } from '../services/processAiService'

export function useUpdateProcessAiProviderKey() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: processAiService.updateProviderKey,
    onSuccess: (settings) => {
      queryClient.setQueryData(queries.processAi.providerSettings.queryKey, settings)
      void queryClient.invalidateQueries({ queryKey: queries.processAi.status.queryKey })
      message.success('DeepSeek API Key 已安全保存')
    },
    onError: (error) => notifyErrorOnce(error, 'DeepSeek API Key 保存失败'),
  })
}
