import { createQueryKeys } from '@lukemorales/query-key-factory'
import type { SettleCandidateQuery, SettleQuery } from '../../../types/settle'
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
})
