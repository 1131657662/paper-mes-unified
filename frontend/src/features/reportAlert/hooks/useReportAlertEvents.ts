import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'
import type { ReportAlertEventQuery } from '../types'

export function useReportAlertEvents(query: ReportAlertEventQuery, enabled: boolean) {
  return useQuery({ ...queries.reportAlert.events(query), enabled,
    staleTime: 30_000, refetchInterval: enabled ? 60_000 : false })
}
