import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'
import type { SettleCandidateQuery } from '../../../types/settle'

export function useSettleCandidates(query: SettleCandidateQuery) {
  return useQuery(queries.settle.candidates(query))
}
