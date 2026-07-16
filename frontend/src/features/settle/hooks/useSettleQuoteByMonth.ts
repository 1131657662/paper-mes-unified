import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'
import type { SettleQuoteByMonthDTO } from '../../../types/settle'

export function useSettleQuoteByMonth(data: SettleQuoteByMonthDTO, enabled: boolean) {
  return useQuery({
    ...queries.settle.quoteByMonth(data),
    enabled,
  })
}
