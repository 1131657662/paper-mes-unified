import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'
import type { SettleByMonthDTO } from '../../../types/settle'

export function useSettleQuoteByMonth(data: SettleByMonthDTO, enabled: boolean) {
  return useQuery({
    ...queries.settle.quoteByMonth(data),
    enabled,
  })
}
