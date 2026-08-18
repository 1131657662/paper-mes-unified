import { useMutation } from '@tanstack/react-query'
import { notifyErrorOnce } from '../../../api/request'
import { processAiService } from '../services/processAiService'

export function useConfirmProcessAiResult() {
  return useMutation({
    mutationFn: processAiService.confirm,
    onError: (error) => notifyErrorOnce(error, 'AI 候选方案确认失败'),
  })
}
