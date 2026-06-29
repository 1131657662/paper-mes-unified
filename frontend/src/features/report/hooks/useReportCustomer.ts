import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'
import type { ReportQuery } from '../../../types/report'

export function useReportCustomer(query: ReportQuery) {
  return useQuery(queries.report.customer(query))
}
