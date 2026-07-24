import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'
import type { ReportQuery } from '../../../types/report'

export function useReportOverview(query: ReportQuery, enabled = true) {
  return useQuery({
    ...queries.report.overview(query),
    enabled,
    placeholderData: keepPreviousData,
  })
}
