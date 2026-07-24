import type { ReportQuery } from '../../types/report'

export function reportTopicQuery(query: ReportQuery): ReportQuery {
  return {
    customerUuid: query.customerUuid,
    dateFrom: query.dateFrom,
    dateTo: query.dateTo,
    machineUuid: query.machineUuid,
    mainStepType: query.mainStepType,
    metricReleaseUuid: query.metricReleaseUuid,
    orderStatus: query.orderStatus,
    paperName: query.paperName,
    processMode: query.processMode,
  }
}
