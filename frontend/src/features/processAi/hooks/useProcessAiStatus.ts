import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'

export function useProcessAiStatus(enabled: boolean) {
  return useQuery({ ...queries.processAi.status, enabled, staleTime: 30_000 })
}
