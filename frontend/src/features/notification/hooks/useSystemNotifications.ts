import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'

export function useSystemNotifications(enabled: boolean) {
  return useQuery({
    ...queries.notifications.summary,
    enabled,
    refetchInterval: 60_000,
  })
}
