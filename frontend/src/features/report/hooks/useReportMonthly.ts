import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'
import type { ReportQuery } from '../../../types/report'

export function useReportMonthly(query: ReportQuery) {
  return useQuery(queries.report.monthly(query))
}
