import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'

export function useReportSubscriptions(enabled: boolean) {
  return useQuery({ ...queries.reportSubscription.list, enabled })
}
