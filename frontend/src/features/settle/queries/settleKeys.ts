import { createQueryKeys } from '@lukemorales/query-key-factory'
import type { SettleCandidateQuery, SettleQuery, SettleQuoteByMonthDTO, SettleQuoteByOrdersDTO } from '../../../types/settle'
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
  detailHeader: (uuid: string) => ({
    queryKey: [uuid, 'header'],
    queryFn: () => settleService.detailHeader(uuid),
  }),
  details: (uuid: string) => ({
    queryKey: [uuid, 'details'],
    queryFn: () => settleService.details(uuid),
  }),
  receives: (uuid: string) => ({
    queryKey: [uuid, 'receives'],
    queryFn: () => settleService.receives(uuid),
  }),
  printLines: (uuid: string) => ({
    queryKey: [uuid, 'print-lines'],
    queryFn: () => settleService.printLines(uuid),
  }),
  operationLogs: (uuid: string) => ({
    queryKey: [uuid, 'operation-logs'],
    queryFn: () => settleService.operationLogs(uuid),
  }),
  discountApprovals: (uuid: string) => ({
    queryKey: [uuid],
    queryFn: () => settleService.discountApprovals(uuid),
  }),
  list: (query: SettleQuery) => ({
    queryKey: [query],
    queryFn: () => settleService.list(query),
  }),
  summary: (query: SettleQuery) => ({
    queryKey: [query],
    queryFn: () => settleService.summary(query),
  }),
  collectionSummary: (query: SettleQuery) => ({
    queryKey: [query],
    queryFn: () => settleService.collectionSummary(query),
  }),
  collectionReminders: (uuid: string) => ({
    queryKey: [uuid],
    queryFn: () => settleService.collectionReminders(uuid),
  }),
  quoteByMonth: (data: SettleQuoteByMonthDTO) => ({
    queryKey: [data],
    queryFn: () => settleService.quoteByMonth(data),
  }),
  quoteByOrders: (data: SettleQuoteByOrdersDTO) => ({
    queryKey: [data],
    queryFn: () => settleService.quoteByOrders(data),
  }),
})
