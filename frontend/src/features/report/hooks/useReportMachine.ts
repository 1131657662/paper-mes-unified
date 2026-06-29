import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'
import type { ReportQuery } from '../../../types/report'

export function useReportMachine(query: ReportQuery) {
  return useQuery(queries.report.machine(query))
}
