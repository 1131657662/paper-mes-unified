import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'
import type { SettleCandidateQuery } from '../../../types/settle'

export function useSettleCandidates(query: SettleCandidateQuery, enabled = true) {
  return useQuery({
    ...queries.settle.candidates(query),
    enabled,
    refetchOnMount: 'always',
    refetchOnWindowFocus: 'always',
    staleTime: 0,
  })
}
