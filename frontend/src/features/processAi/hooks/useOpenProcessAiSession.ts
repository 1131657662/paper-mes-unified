import { useMutation } from '@tanstack/react-query'
import { notifyErrorOnce } from '../../../api/request'
import { processAiService } from '../services/processAiService'

export function useOpenProcessAiSession() {
  return useMutation({
    mutationFn: processAiService.openSession,
    onError: (error) => notifyErrorOnce(error, 'AI 工艺助手会话打开失败'),
  })
}
