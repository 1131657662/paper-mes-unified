import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'
import type { ReportSubscriptionRunQuery } from '../types'

export function useReportSubscriptionRuns(
  uuid: string,
  query: ReportSubscriptionRunQuery,
  enabled: boolean,
) {
  return useQuery({ ...queries.reportSubscription.runs(uuid, query), enabled,
    placeholderData: keepPreviousData, refetchInterval: enabled ? 30_000 : false })
}
