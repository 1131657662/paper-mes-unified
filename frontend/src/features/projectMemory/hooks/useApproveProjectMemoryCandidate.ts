import { message } from 'antd'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { BizError, notifyErrorOnce } from '../../../api/request'
import { queries } from '../../../queries'
import { projectMemoryService } from '../services/projectMemoryService'

export function useApproveProjectMemoryCandidate() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: projectMemoryService.approveCandidate,
    onSuccess: (snapshot) => {
      queryClient.setQueryData(queries.projectMemory.current.queryKey, snapshot)
      void queryClient.invalidateQueries({ queryKey: queries.projectMemory.versions.queryKey })
      void queryClient.invalidateQueries({ queryKey: queries.projectMemory._def })
      message.success(`候选知识已批准，项目记忆更新至 ${snapshot.memoryVersion}`)
    },
    onError: (error) => {
      if (error instanceof BizError && error.errorCode === 'MEMORY_VERSION_CONFLICT') {
        void queryClient.invalidateQueries({ queryKey: queries.projectMemory._def })
      }
      notifyErrorOnce(error, '候选知识批准失败')
    },
  })
}
