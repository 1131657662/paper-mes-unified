import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'

export function useReportMetricRelease(releaseUuid: string, enabled: boolean) {
  return useQuery({ ...queries.report.metricRelease(releaseUuid), enabled })
}
