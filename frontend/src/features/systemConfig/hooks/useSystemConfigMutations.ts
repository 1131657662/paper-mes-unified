import { useMutation, useQueryClient } from '@tanstack/react-query'
import { queries } from '../../../queries'
import { systemConfigService } from '../services/systemConfigService'

export function useCreateDictItem() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: systemConfigService.createDictItem,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queries.systemConfig._def }),
  })
}

export function useUpdateDictItem() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: systemConfigService.updateDictItem,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queries.systemConfig._def }),
  })
}

export function useDeleteDictItem() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: systemConfigService.deleteDictItem,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queries.systemConfig._def }),
  })
}

export function useCreateConfigItem() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: systemConfigService.createConfigItem,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queries.systemConfig._def }),
  })
}

export function useUpdateConfigItem() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: systemConfigService.updateConfigItem,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queries.systemConfig._def }),
  })
}

export function useDeleteConfigItem() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: systemConfigService.deleteConfigItem,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queries.systemConfig._def }),
  })
}

export function useUpdateNoRule() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: systemConfigService.updateNoRule,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queries.systemConfig._def }),
  })
}
