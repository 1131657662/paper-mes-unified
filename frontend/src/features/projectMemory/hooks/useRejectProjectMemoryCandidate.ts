import { message } from 'antd'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { notifyErrorOnce } from '../../../api/request'
import { queries } from '../../../queries'
import { projectMemoryService } from '../services/projectMemoryService'

export function useRejectProjectMemoryCandidate() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: projectMemoryService.rejectCandidate,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queries.projectMemory._def })
      message.success('候选知识已拒绝')
    },
    onError: (error) => notifyErrorOnce(error, '候选知识拒绝失败'),
  })
}
