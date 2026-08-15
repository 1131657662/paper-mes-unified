import { message } from 'antd'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { notifyErrorOnce } from '../../../api/request'
import { queries } from '../../../queries'
import { projectMemoryService } from '../services/projectMemoryService'

export function useReloadProjectMemory() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: projectMemoryService.reload,
    onSuccess: (snapshot) => {
      queryClient.setQueryData(queries.projectMemory.current.queryKey, snapshot)
      message.success('项目记忆已重新加载')
    },
    onError: (error) => notifyErrorOnce(error, '项目记忆重载失败'),
  })
}
