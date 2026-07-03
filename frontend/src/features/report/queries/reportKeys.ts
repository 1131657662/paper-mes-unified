import { createQueryKeys } from '@lukemorales/query-key-factory'
import type { ReportQuery } from '../../../types/report'
import { reportService } from '../services/reportService'

export const reportKeys = createQueryKeys('report', {
  details: (query: ReportQuery) => ({
    queryKey: [query],
    queryFn: () => reportService.details(query),
  }),
  dimensions: (query: ReportQuery) => ({
    queryKey: [query],
    queryFn: () => reportService.dimensions(query),
  }),
  machines: {
    queryKey: null,
    queryFn: () => reportService.machines(),
  },
  overview: (query: ReportQuery) => ({
    queryKey: [query],
    queryFn: () => reportService.overview(query),
  }),
  papers: {
    queryKey: null,
    queryFn: () => reportService.papers(),
  },
})
