import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'

export function useReportSubscriptionCandidates(enabled: boolean) {
  return useQuery({ ...queries.reportSubscription.candidates, enabled })
}
