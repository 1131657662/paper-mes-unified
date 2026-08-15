import { message } from 'antd'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { BizError, notifyErrorOnce } from '../../../api/request'
import { queries } from '../../../queries'
import { projectMemoryService } from '../services/projectMemoryService'

export function useRollbackProjectMemory() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: projectMemoryService.rollback,
    onSuccess: (snapshot) => {
      queryClient.setQueryData(queries.projectMemory.current.queryKey, snapshot)
      void queryClient.invalidateQueries({ queryKey: queries.projectMemory.versions.queryKey })
      message.success(`已回滚到项目记忆 ${snapshot.memoryVersion}`)
    },
    onError: (error) => {
      if (error instanceof BizError && error.errorCode === 'MEMORY_VERSION_CONFLICT') {
        void queryClient.invalidateQueries({ queryKey: queries.projectMemory._def })
      }
      notifyErrorOnce(error, '项目记忆回滚失败')
    },
  })
}
