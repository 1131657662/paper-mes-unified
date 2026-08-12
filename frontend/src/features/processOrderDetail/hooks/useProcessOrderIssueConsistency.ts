import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'

export function useProcessOrderIssueConsistency(orderUuid?: string, enabled = true) {
  return useQuery({
    ...queries.processOrderDetail.issueConsistency(orderUuid ?? ''),
    enabled: Boolean(orderUuid) && enabled,
  })
}
