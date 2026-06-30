import { useMutation, useQueryClient } from '@tanstack/react-query'
import { queries } from '../../../queries'
import { userService } from '../services/userService'

export function useCreateUser() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: userService.create,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queries.user._def }),
  })
}

export function useUpdateUser() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: userService.update,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queries.user._def }),
  })
}

export function useUpdateUserStatus() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: userService.updateStatus,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queries.user._def }),
  })
}

export function useResetUserPassword() {
  return useMutation({
    mutationFn: userService.resetPassword,
  })
}
