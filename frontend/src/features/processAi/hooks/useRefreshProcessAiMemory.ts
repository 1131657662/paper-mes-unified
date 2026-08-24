import { useMutation, useQueryClient } from '@tanstack/react-query'
import { notifyErrorOnce } from '../../../api/request'
import { queries } from '../../../queries'
import { processAiService } from '../services/processAiService'

export function useRefreshProcessAiMemory() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: processAiService.refreshMemory,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queries.processAi.messages._def })
    },
    onError: (error) => notifyErrorOnce(error, '刷新项目记忆失败'),
  })
}
