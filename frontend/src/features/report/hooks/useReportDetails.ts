import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'
import type { ReportDetailQuery } from '../../../types/report'

export function useReportDetails(query: ReportDetailQuery, enabled = true) {
  return useQuery({
    ...queries.report.details(query),
    enabled,
    placeholderData: keepPreviousData,
  })
}
