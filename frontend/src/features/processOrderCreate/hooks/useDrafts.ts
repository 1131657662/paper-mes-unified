import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'

export function useDrafts() {
  return useQuery(queries.createOrder.drafts)
}
