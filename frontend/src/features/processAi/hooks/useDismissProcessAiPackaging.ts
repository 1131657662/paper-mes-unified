import { useMutation, useQueryClient } from '@tanstack/react-query'
import { notifyErrorOnce } from '../../../api/request'
import { queries } from '../../../queries'
import { processAiService } from '../services/processAiService'

export function useDismissProcessAiPackaging() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: processAiService.dismissPackaging,
    onSuccess: (_result, input) => {
      queryClient.invalidateQueries({
        queryKey: queries.processAi.pendingPackaging(input).queryKey,
      })
    },
    onError: (error) => notifyErrorOnce(error, '放弃 AI 包装候选失败'),
  })
}
