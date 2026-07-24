import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'

export function useSnapshotDiff(uuid?: string, enabled = true) {
  return useQuery({
    ...queries.processOrderDetail.snapshotDiff(uuid ?? ''),
    enabled: Boolean(uuid) && enabled,
  })
}
