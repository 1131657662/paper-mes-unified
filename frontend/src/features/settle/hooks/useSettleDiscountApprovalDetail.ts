import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'

export function useSettleDiscountApprovalDetail(uuid: string) {
  return useQuery({
    ...queries.settle.discountApprovalDetail(uuid),
    enabled: Boolean(uuid),
  })
}
