import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'
import type { ReportQuery } from '../../../types/report'
import type { ReportTopicCode } from '../services/reportService'

export function useReportTopicAnalysis(
  topic: ReportTopicCode,
  query: ReportQuery,
  enabled = true,
) {
  return useQuery({
    ...queries.report.topicAnalysis(topic, query),
    enabled,
    placeholderData: keepPreviousData,
  })
}
