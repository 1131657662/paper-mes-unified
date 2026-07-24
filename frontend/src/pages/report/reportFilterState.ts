import dayjs from 'dayjs'
import type { ReportFilterValues } from '../../features/report/components/ReportFilterBar'
import type { ReportQuery } from '../../types/report'

export function reportFiltersFromQuery(query: ReportQuery): ReportFilterValues {
  const dateFrom = query.dateFrom ? dayjs(query.dateFrom) : dayjs().startOf('year')
  const dateTo = query.dateTo ? dayjs(query.dateTo) : dayjs()
  return {
    period: [dateFrom, dateTo], customerUuid: query.customerUuid, paperName: query.paperName,
    mainStepType: query.mainStepType, processMode: query.processMode, machineUuid: query.machineUuid,
    settleType: query.settleType, isInvoice: query.isInvoice, orderStatus: query.orderStatus,
  }
}

export function reportQueryFromFilters(values: ReportFilterValues): ReportQuery {
  return {
    customerUuid: values.customerUuid,
    dateFrom: values.period?.[0]?.format('YYYY-MM-DD'),
    dateTo: values.period?.[1]?.format('YYYY-MM-DD'),
    isInvoice: values.isInvoice,
    machineUuid: values.machineUuid,
    mainStepType: values.mainStepType,
    orderStatus: values.orderStatus,
    paperName: values.paperName,
    processMode: values.processMode,
    settleType: values.settleType,
  }
}
