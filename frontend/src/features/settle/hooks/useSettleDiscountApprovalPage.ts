import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'
import type { SettleDiscountApprovalQuery } from '../../../types/settle'

export function useSettleDiscountApprovalPage(query: SettleDiscountApprovalQuery) {
  return useQuery({
    ...queries.settle.discountApprovalPage(query),
    refetchInterval: query.scope === 'pending' ? 30_000 : false,
  })
}
