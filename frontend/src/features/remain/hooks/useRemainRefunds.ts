import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'

export function useRemainRefunds() {
  return useQuery({ ...queries.remain.refunds, staleTime: 15_000 })
}
