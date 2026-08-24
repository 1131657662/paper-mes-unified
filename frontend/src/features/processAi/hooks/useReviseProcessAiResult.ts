import { useMutation } from '@tanstack/react-query'
import { notifyErrorOnce } from '../../../api/request'
import { processAiService } from '../services/processAiService'

export function useReviseProcessAiResult() {
  return useMutation({
    mutationFn: processAiService.revise,
    onError: (error) => notifyErrorOnce(error, 'AI 工艺修正失败，请重新预览'),
  })
}
