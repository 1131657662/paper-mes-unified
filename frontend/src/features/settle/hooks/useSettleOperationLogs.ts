import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'

export function useSettleOperationLogs(uuid?: string, enabled = true) {
  return useQuery({
    ...queries.settle.operationLogs(uuid ?? ''),
    enabled: Boolean(uuid) && enabled,
  })
}
