import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'
import type { ReportQuery } from '../../../types/report'

export function useReportOverview(query: ReportQuery) {
  return useQuery(queries.report.overview(query))
}
