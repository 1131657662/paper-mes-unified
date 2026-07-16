import { createQueryKeys } from '@lukemorales/query-key-factory'
import type { SettleByMonthDTO, SettleByOrdersDTO, SettleCandidateQuery, SettleQuery } from '../../../types/settle'
import { settleService } from '../services/settleService'

export const settleKeys = createQueryKeys('settle', {
  candidates: (query: SettleCandidateQuery) => ({
    queryKey: [query],
    queryFn: () => settleService.candidates(query),
  }),
  detail: (uuid: string) => ({
    queryKey: [uuid],
    queryFn: () => settleService.detail(uuid),
  }),
  list: (query: SettleQuery) => ({
    queryKey: [query],
    queryFn: () => settleService.list(query),
  }),
  summary: (query: SettleQuery) => ({
    queryKey: [query],
    queryFn: () => settleService.summary(query),
  }),
  quoteByMonth: (data: SettleByMonthDTO) => ({
    queryKey: [data],
    queryFn: () => settleService.quoteByMonth(data),
  }),
  quoteByOrders: (data: SettleByOrdersDTO) => ({
    queryKey: [data],
    queryFn: () => settleService.quoteByOrders(data),
  }),
})
