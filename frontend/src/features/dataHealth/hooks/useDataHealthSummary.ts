import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'

export function useDataHealthSummary() {
  return useQuery(queries.dataHealth.summary)
}
