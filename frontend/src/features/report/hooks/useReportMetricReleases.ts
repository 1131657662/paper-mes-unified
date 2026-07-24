import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'

export function useReportMetricReleases(enabled: boolean) {
  return useQuery({ ...queries.report.metricReleases, enabled })
}
