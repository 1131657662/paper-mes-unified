import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'
import type { ReportDetailQuery } from '../../../types/report'

export function useReportPageAnalysis(query: ReportDetailQuery, enabled = true) {
  return useQuery({
    ...queries.report.pageAnalysis(query),
    enabled,
    placeholderData: keepPreviousData,
  })
}
