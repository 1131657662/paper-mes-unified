import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'

export function useReportMetricContext() {
  return useQuery({
    ...queries.report.metricContext,
    staleTime: 5 * 60 * 1000,
  })
}
