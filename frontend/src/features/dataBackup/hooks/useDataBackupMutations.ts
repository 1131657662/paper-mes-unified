import { useMutation, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { queries } from '../../../queries'
import { dataBackupService } from '../services/dataBackupService'

export function useCreateDataBackup() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: dataBackupService.create,
    onSuccess: (result) => {
      message.success(result.message)
      void queryClient.invalidateQueries({ queryKey: queries.dataBackup._def })
    },
  })
}

export function useVerifyDataBackup() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: dataBackupService.verify,
    onSuccess: (result) => {
      message.success(result.message)
      void queryClient.invalidateQueries({ queryKey: queries.dataBackup._def })
    },
  })
}

export function useUpdateDataBackupEnabled() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: dataBackupService.updateEnabled,
    onSuccess: (status) => {
      message.success(status.enabled ? '管理端备份已启用' : '管理端备份已停用')
      void queryClient.invalidateQueries({ queryKey: queries.dataBackup._def })
    },
  })
}

export function useUpdateAutomaticBackup() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ enabled, executionTime }: { enabled: boolean; executionTime: string }) => (
      dataBackupService.updateAutomatic(enabled, executionTime)
    ),
    onSuccess: (status) => {
      message.success(status.automaticEnabled ? '自动备份已启用' : '自动备份已停用')
      void queryClient.invalidateQueries({ queryKey: queries.dataBackup._def })
    },
  })
}

export function useUpdateDataBackupRetention() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: dataBackupService.updateRetention,
    onSuccess: () => {
      message.success('备份保留策略已更新')
      void queryClient.invalidateQueries({ queryKey: queries.dataBackup._def })
    },
  })
}

export function useCleanupExpiredBackups() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: dataBackupService.cleanup,
    onSuccess: (result) => {
      message.success(result.message)
      void queryClient.invalidateQueries({ queryKey: queries.dataBackup._def })
    },
  })
}

export function useDeleteDataBackup() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: dataBackupService.delete,
    onSuccess: () => {
      message.success('备份已删除')
      void queryClient.invalidateQueries({ queryKey: queries.dataBackup._def })
    },
  })
}
