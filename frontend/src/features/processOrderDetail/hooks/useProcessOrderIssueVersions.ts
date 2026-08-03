import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'

export function useProcessOrderIssueVersions(uuid?: string, enabled = true) {
  return useQuery({
    ...queries.processOrderDetail.issueVersions(uuid ?? ''),
    enabled: Boolean(uuid) && enabled,
  })
}
