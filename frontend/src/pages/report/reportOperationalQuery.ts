import type { ReportQuery } from '../../types/report'
import type { ReportOperationalTopicCode } from '../../types/reportOperational'

export function reportOperationalQuery(
  topic: ReportOperationalTopicCode,
  query: ReportQuery,
): ReportQuery {
  const common = {
    customerUuid: query.customerUuid,
    dateFrom: query.dateFrom,
    dateTo: query.dateTo,
    metricReleaseUuid: query.metricReleaseUuid,
  }
  if (topic === 'settlement' || topic === 'collection') {
    return { ...common, isInvoice: query.isInvoice, settleType: query.settleType }
  }
  if (topic === 'inventory') return { ...common, paperName: query.paperName }
  return common
}

export function needsOperationalPaperCandidates(topic: ReportOperationalTopicCode): boolean {
  return topic === 'inventory'
}
