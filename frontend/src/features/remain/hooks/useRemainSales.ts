import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'

export function useRemainSales() {
  return useQuery({ ...queries.remain.sales, staleTime: 15_000 })
}
