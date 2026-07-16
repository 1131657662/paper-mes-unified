import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'
import type { SettleByOrdersDTO } from '../../../types/settle'

export function useSettleQuoteByOrders(data: SettleByOrdersDTO, enabled: boolean) {
  return useQuery({
    ...queries.settle.quoteByOrders(data),
    enabled,
  })
}
