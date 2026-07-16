import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'

export function useSettleDiscountApprovals(uuid: string, enabled: boolean) {
  return useQuery({
    ...queries.settle.discountApprovals(uuid),
    enabled: enabled && Boolean(uuid),
  })
}
