import type { ReportQuery } from '../../types/report'

export function parseReportUrlState(params: URLSearchParams) {
  return { query: parseQuery(params) }
}

export function serializeReportUrlState(query: ReportQuery) {
  const params = new URLSearchParams()
  const values: Record<string, string | number | undefined> = {
    metricReleaseUuid: query.metricReleaseUuid, dateFrom: query.dateFrom, dateTo: query.dateTo,
    customerUuid: query.customerUuid, paperName: query.paperName, mainStepType: query.mainStepType,
    processStepType: query.processStepType, processMode: query.processMode,
    machineUuid: query.machineUuid, settleType: query.settleType,
    isInvoice: query.isInvoice, orderStatus: query.orderStatus,
  }
  Object.entries(values).forEach(([key, value]) => {
    if (value !== undefined && value !== '') params.set(key, String(value))
  })
  return params
}

function parseQuery(params: URLSearchParams): ReportQuery {
  return {
    metricReleaseUuid: text(params.get('metricReleaseUuid')),
    dateFrom: text(params.get('dateFrom')),
    dateTo: text(params.get('dateTo')),
    customerUuid: text(params.get('customerUuid')),
    paperName: text(params.get('paperName')),
    mainStepType: number(params.get('mainStepType')),
    processStepType: number(params.get('processStepType')),
    processMode: number(params.get('processMode')),
    machineUuid: text(params.get('machineUuid')),
    settleType: number(params.get('settleType')),
    isInvoice: number(params.get('isInvoice')),
    orderStatus: number(params.get('orderStatus')),
  }
}

function text(value: string | null) {
  return value || undefined
}

function number(value: string | null) {
  if (!value) return undefined
  const parsed = Number(value)
  return Number.isInteger(parsed) ? parsed : undefined
}
