import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'

export function useLatestSettleDiscountApproval(uuid: string, enabled: boolean) {
  return useQuery({
    ...queries.settle.latestDiscountApproval(uuid),
    enabled: enabled && Boolean(uuid),
    refetchInterval: enabled ? 10_000 : false,
  })
}
