import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'
import type { ReportQuery } from '../../../types/report'

export function useReportDimensionAnalysis(query: ReportQuery, enabled = true) {
  return useQuery({
    ...queries.report.dimensionAnalysis(query),
    enabled,
    placeholderData: keepPreviousData,
  })
}
