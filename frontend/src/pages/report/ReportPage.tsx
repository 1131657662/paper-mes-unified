import { Card, Spin, message } from 'antd'
import QueryLoadErrorAlert from '../../components/feedback/QueryLoadErrorAlert'
import dayjs from 'dayjs'
import { useState } from 'react'
import ReportBarList from '../../features/report/components/ReportBarList'
import ReportFilterBar, {
  type ReportFilterValues,
} from '../../features/report/components/ReportFilterBar'
import ReportFilterSummary from '../../features/report/components/ReportFilterSummary'
import ReportInsightStrip from '../../features/report/components/ReportInsightStrip'
import ReportMetricStrip from '../../features/report/components/ReportMetricStrip'
import ReportTables from '../../features/report/components/ReportTables'
import ReportTrendPanel from '../../features/report/components/ReportTrendPanel'
import { useExportReport } from '../../features/report/hooks/useExportReport'
import { useReportDetails } from '../../features/report/hooks/useReportDetails'
import { useReportDimensions } from '../../features/report/hooks/useReportDimensions'
import { useReportOverview } from '../../features/report/hooks/useReportOverview'
import { useReportMachines, useReportPapers } from '../../features/report/hooks/useReportReferenceData'
import { formatMoney, formatNumber, formatTonFromKg } from '../../features/report/utils/reportFormatters'
import { useCustomers } from '../../features/processOrderCreate/hooks/useReferenceData'
import { notifyErrorOnce } from '../../api/request'
import type { ReportDimension, ReportQuery } from '../../types/report'
import '../documentModule.css'
import './ReportPage.css'

export default function ReportPage() {
  const initialFilters = defaultFilters()
  const [query, setQuery] = useState<ReportQuery>(() => toQuery(initialFilters))
  const [dimension, setDimension] = useState<ReportDimension>(initialFilters.dimension)
  const overviewQuery = useReportOverview(query)
  const dimensionQuery = useReportDimensions({ ...query, dimension })
  const detailsQuery = useReportDetails(query)
  const monthlyQuery = useReportDimensions({ ...query, dimension: 'month' })
  const customerRankQuery = useReportDimensions({ ...query, dimension: 'customer' })
  const productRankQuery = useReportDimensions({ ...query, dimension: 'paper' })
  const customersQuery = useCustomers()
  const machinesQuery = useReportMachines()
  const papersQuery = useReportPapers()
  const exportMutation = useExportReport()
  const loading = overviewQuery.isFetching || dimensionQuery.isFetching || detailsQuery.isFetching
  const resultError = [overviewQuery, dimensionQuery, detailsQuery, monthlyQuery, customerRankQuery, productRankQuery]
    .some((item) => item.isError)
  const referenceError = customersQuery.isError || machinesQuery.isError || papersQuery.isError

  const refresh = () => {
    overviewQuery.refetch()
    dimensionQuery.refetch()
    detailsQuery.refetch()
    monthlyQuery.refetch()
    customerRankQuery.refetch()
    productRankQuery.refetch()
  }

  const submit = (values: ReportFilterValues) => {
    const nextQuery = toQuery(values)
    setQuery(nextQuery)
    setDimension(values.dimension)
  }

  const exportCurrent = () => {
    exportMutation.mutate(
      { ...query, dimension },
      {
        onError: (error) => notifyErrorOnce(error, '导出失败，请稍后重试'),
        onSuccess: (result) => message.success(`已导出 ${result.filename}（${formatFileSize(result.size)}）`),
      },
    )
  }

  return (
    <div className="report-workbench mes-workbench">
      <Card className="report-filter-card" title="统计报表">
        {referenceError && (
          <QueryLoadErrorAlert
            description="客户、纸张或机台筛选项未完整加载，当前筛选范围可能不完整。"
            message="报表筛选资料加载失败"
            onRetry={() => {
              void customersQuery.refetch()
              void machinesQuery.refetch()
              void papersQuery.refetch()
            }}
          />
        )}
        <ReportFilterBar
          customers={customersQuery.data?.records ?? []}
          exporting={exportMutation.isPending}
          initialValues={initialFilters}
          loading={customersQuery.isLoading}
          machines={machinesQuery.data?.records ?? []}
          onExport={exportCurrent}
          onRefresh={refresh}
          onSubmit={submit}
          papers={papersQuery.data?.records ?? []}
        />
      </Card>

      <Card className="report-result-card" title="分析结果">
        {resultError && (
          <QueryLoadErrorAlert
            description="部分或全部统计结果未成功加载，当前空图表不代表没有业务数据。"
            message="报表数据加载失败"
            onRetry={refresh}
          />
        )}
        <Spin spinning={loading}>
          <div className="report-workbench__content">
            <ReportFilterSummary
              customers={customersQuery.data?.records ?? []}
              machines={machinesQuery.data?.records ?? []}
              papers={papersQuery.data?.records ?? []}
              query={query}
            />
            <ReportMetricStrip overview={overviewQuery.data} />
            <ReportInsightStrip overview={overviewQuery.data} details={detailsQuery.data?.rows} />
            <div className="report-workbench__grid">
              <ReportTrendPanel monthly={monthlyQuery.data ?? []} />
              <ReportBarList
                title="客户加工应收排行"
                emptyText="当前条件暂无客户汇总"
                items={(customerRankQuery.data ?? []).slice(0, 8).map((item) => ({
                  key: item.dimensionKey,
                  label: item.dimensionName,
                  meta: `${formatNumber(item.orderCount)} 单 / ${formatTonFromKg(item.originalWeight)}`,
                  value: item.totalAmount ?? 0,
                  valueText: formatMoney(item.totalAmount),
                }))}
              />
              <ReportBarList
                title="产品工艺费贡献排行"
                emptyText="当前条件暂无产品汇总"
                items={(productRankQuery.data ?? []).slice(0, 8).map((item) => ({
                  key: item.dimensionKey,
                  label: item.dimensionName,
                  meta: `${formatNumber(item.originalRollCount)} 卷 / ${formatTonFromKg(item.originalWeight)}`,
                  value: item.processAmount || item.totalAmount || item.originalWeight || 0,
                  valueText: item.processAmount ? formatMoney(item.processAmount) : formatTonFromKg(item.originalWeight),
                }))}
              />
            </div>
            <ReportTables
              details={detailsQuery.data}
              dimension={dimension}
              dimensions={dimensionQuery.data ?? []}
              loading={loading}
              onDimensionChange={setDimension}
              onRefresh={refresh}
            />
          </div>
        </Spin>
      </Card>
    </div>
  )
}

function defaultFilters(): ReportFilterValues {
  return {
    dimension: 'customer',
    period: [dayjs().startOf('year'), dayjs()],
  }
}

function toQuery(values: ReportFilterValues): ReportQuery {
  return {
    customerUuid: values.customerUuid,
    dateFrom: values.period?.[0]?.format('YYYY-MM-DD'),
    dateTo: values.period?.[1]?.format('YYYY-MM-DD'),
    dimension: values.dimension,
    isInvoice: values.isInvoice,
    machineUuid: values.machineUuid,
    mainStepType: values.mainStepType,
    orderStatus: values.orderStatus,
    paperName: values.paperName,
    processMode: values.processMode,
    settleType: values.settleType,
  }
}

function formatFileSize(size: number) {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}
