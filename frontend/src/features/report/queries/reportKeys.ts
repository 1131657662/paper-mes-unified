import { createQueryKeys } from '@lukemorales/query-key-factory'
import type { ReportQuery } from '../../../types/report'
import { reportService } from '../services/reportService'

export const reportKeys = createQueryKeys('report', {
  customer: (query: ReportQuery) => ({
    queryKey: [query],
    queryFn: () => reportService.customer(query),
  }),
  details: (query: ReportQuery) => ({
    queryKey: [query],
    queryFn: () => reportService.details(query),
  }),
  dimensions: (query: ReportQuery) => ({
    queryKey: [query],
    queryFn: () => reportService.dimensions(query),
  }),
  loss: (query: ReportQuery) => ({
    queryKey: [query],
    queryFn: () => reportService.loss(query),
  }),
  machine: (query: ReportQuery) => ({
    queryKey: [query],
    queryFn: () => reportService.machine(query),
  }),
  machines: {
    queryKey: null,
    queryFn: () => reportService.machines(),
  },
  monthly: (query: ReportQuery) => ({
    queryKey: [query],
    queryFn: () => reportService.monthly(query),
  }),
  overview: (query: ReportQuery) => ({
    queryKey: [query],
    queryFn: () => reportService.overview(query),
  }),
  papers: {
    queryKey: null,
    queryFn: () => reportService.papers(),
  },
})
