import { createQueryKeys } from '@lukemorales/query-key-factory'
import type { ReportQuery } from '../../../types/report'
import { reportService } from '../services/reportService'

export const reportKeys = createQueryKeys('report', {
  customer: (query: ReportQuery) => ({
    queryKey: [query],
    queryFn: () => reportService.customer(query),
  }),
  loss: (query: ReportQuery) => ({
    queryKey: [query],
    queryFn: () => reportService.loss(query),
  }),
  machine: (query: ReportQuery) => ({
    queryKey: [query],
    queryFn: () => reportService.machine(query),
  }),
  monthly: (query: ReportQuery) => ({
    queryKey: [query],
    queryFn: () => reportService.monthly(query),
  }),
})
