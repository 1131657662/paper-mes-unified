import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'

export function useAiStatus(enabled: boolean) {
  return useQuery({
    ...queries.ai.status,
    enabled,
    staleTime: 60_000,
  })
}
