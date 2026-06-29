import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'

export function useDashboardOverview() {
  return useQuery(queries.dashboard.overview)
}
