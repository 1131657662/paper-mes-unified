import { createQueryKeys } from '@lukemorales/query-key-factory'
import { reportSubscriptionService } from '../services/reportSubscriptionService'
import type { ReportSubscriptionRunQuery } from '../types'

export const reportSubscriptionKeys = createQueryKeys('reportSubscription', {
  candidates: {
    queryKey: null,
    queryFn: reportSubscriptionService.candidates,
  },
  list: {
    queryKey: null,
    queryFn: reportSubscriptionService.list,
  },
  runs: (uuid: string, query: ReportSubscriptionRunQuery) => ({
    queryKey: [uuid, query],
    queryFn: () => reportSubscriptionService.runs(uuid, query),
  }),
})
