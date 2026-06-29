import { useState } from 'react'
import { Card, Spin } from 'antd'
import dayjs from 'dayjs'
import ReportBarList from '../../features/report/components/ReportBarList'
import ReportFilterBar, {
  type ReportFilterValues,
} from '../../features/report/components/ReportFilterBar'
import ReportMetricStrip from '../../features/report/components/ReportMetricStrip'
import ReportTables from '../../features/report/components/ReportTables'
import ReportTrendPanel from '../../features/report/components/ReportTrendPanel'
import { useReportCustomer } from '../../features/report/hooks/useReportCustomer'
import { useReportLoss } from '../../features/report/hooks/useReportLoss'
import { useReportMachine } from '../../features/report/hooks/useReportMachine'
import { useReportMonthly } from '../../features/report/hooks/useReportMonthly'
import { formatKg, formatMoney } from '../../features/report/utils/reportFormatters'
import { summarizeReports } from '../../features/report/utils/reportSummary'
import { useCustomers } from '../../features/processOrderCreate/hooks/useReferenceData'
import type { ReportQuery } from '../../types/report'
import './ReportPage.css'

export default function ReportPage() {
  const initialFilters: ReportFilterValues = { period: [dayjs().startOf('month'), dayjs()] }
  const [query, setQuery] = useState<ReportQuery>(() => currentMonthQuery())
  const customersQuery = useCustomers()
  const monthlyQuery = useReportMonthly(query)
  const customerQuery = useReportCustomer(query)
  const lossQuery = useReportLoss(query)
  const machineQuery = useReportMachine(query)

  const monthly = monthlyQuery.data ?? []
  const customers = customerQuery.data ?? []
  const losses = lossQuery.data ?? []
  const machines = machineQuery.data ?? []
  const summary = summarizeReports({ customers, losses, machines, monthly })
  const loading = monthlyQuery.isFetching || customerQuery.isFetching || lossQuery.isFetching || machineQuery.isFetching

  const refresh = () => {
    monthlyQuery.refetch()
    customerQuery.refetch()
    lossQuery.refetch()
    machineQuery.refetch()
  }

  return (
    <Card className="report-workbench mes-fill-card mes-workbench" title="统计报表">
      <ReportFilterBar
        customers={customersQuery.data?.records ?? []}
        initialValues={initialFilters}
        loading={customersQuery.isLoading}
        onRefresh={refresh}
        onValuesChange={(_, values) => setQuery(toQuery(values))}
      />

      <Spin spinning={loading}>
        <div className="report-workbench__content">
          <ReportMetricStrip summary={summary} />
          <div className="report-workbench__grid">
            <ReportTrendPanel monthly={monthly} />
            <ReportBarList
              title="客户金额排行"
              emptyText="当前周期暂无客户统计"
              items={customers.slice(0, 6).map((item) => ({
                key: item.customerUuid,
                label: item.customerName,
                meta: `${item.orderCount ?? 0} 单 / ${item.totalTon ?? 0}t`,
                value: item.totalAmount ?? 0,
                valueText: formatMoney(item.totalAmount),
              }))}
            />
            <ReportBarList
              title="机台产出排行"
              emptyText="当前周期暂无机台产出"
              items={machines.slice(0, 6).map((item) => ({
                key: item.machineUuid,
                label: item.machineName,
                meta: `${item.rollCount ?? 0} 卷 / ${item.totalKnife ?? 0} 刀`,
                value: item.totalOutputWeight ?? 0,
                valueText: formatKg(item.totalOutputWeight),
              }))}
            />
          </div>
          <ReportTables customers={customers} losses={losses} machines={machines} monthly={monthly} />
        </div>
      </Spin>
    </Card>
  )
}

function currentMonthQuery(): ReportQuery {
  return {
    dateFrom: dayjs().startOf('month').format('YYYY-MM-DD'),
    dateTo: dayjs().format('YYYY-MM-DD'),
  }
}

function toQuery(values: ReportFilterValues): ReportQuery {
  return {
    customerUuid: values.customerUuid,
    dateFrom: values.period?.[0]?.format('YYYY-MM-DD'),
    dateTo: values.period?.[1]?.format('YYYY-MM-DD'),
  }
}
