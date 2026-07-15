import { useMutation, useQueryClient } from '@tanstack/react-query'
import { queries } from '../../../queries'
import { notificationService } from '../services/notificationService'

export function useMarkNotificationRead() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: notificationService.markRead,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queries.notifications._def }),
  })
}

export function useMarkAllNotificationsRead() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: notificationService.markAllRead,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queries.notifications._def }),
  })
}
