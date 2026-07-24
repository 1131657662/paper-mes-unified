import { createQueryKeys } from '@lukemorales/query-key-factory'
import type { ReportQuery } from '../../../types/report'
import type { ReportAlertEventQuery } from '../types'
import { reportAlertService } from '../services/reportAlertService'

export const reportAlertKeys = createQueryKeys('reportAlert', {
  rules: {
    queryKey: null,
    queryFn: () => reportAlertService.rules(),
  },
  thresholdContext: (query: ReportQuery) => ({
    queryKey: [query],
    queryFn: () => reportAlertService.thresholdContext(query),
  }),
  events: (query: ReportAlertEventQuery) => ({
    queryKey: [query],
    queryFn: () => reportAlertService.events(query),
  }),
})
