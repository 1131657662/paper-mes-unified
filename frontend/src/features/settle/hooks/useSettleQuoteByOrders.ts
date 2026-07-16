import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'
import type { SettleQuoteByOrdersDTO } from '../../../types/settle'

export function useSettleQuoteByOrders(data: SettleQuoteByOrdersDTO, enabled: boolean) {
  return useQuery({
    ...queries.settle.quoteByOrders(data),
    enabled,
  })
}
