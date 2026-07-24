import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'
import type { ReportQuery } from '../../../types/report'

export function useReportThresholdContext(query: ReportQuery, enabled = true) {
  return useQuery({
    ...queries.reportAlert.thresholdContext(query),
    enabled,
    placeholderData: keepPreviousData,
  })
}
