import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'

export function useRemainAdjustments() {
  return useQuery({ ...queries.remain.adjustments, staleTime: 15_000 })
}
