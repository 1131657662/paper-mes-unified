import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'
import type { ReportQuery } from '../../../types/report'
import type { ReportOperationalTopicCode } from '../../../types/reportOperational'

export function useReportOperationalAnalysis(
  topic: ReportOperationalTopicCode,
  query: ReportQuery,
  enabled = true,
) {
  return useQuery({
    ...queries.report.operationalAnalysis(topic, query),
    enabled,
    placeholderData: keepPreviousData,
  })
}
