import { Card, Spin } from 'antd'
import QueryLoadErrorAlert from '../../components/feedback/QueryLoadErrorAlert'
import { useState } from 'react'
import { useLocation, useSearchParams } from 'react-router-dom'
import ReportFilterBar, {
  type ReportFilterValues,
} from '../../features/report/components/ReportFilterBar'
import ReportFilterSummary from '../../features/report/components/ReportFilterSummary'
import ReportInsightStrip from '../../features/report/components/ReportInsightStrip'
import ReportMetricStrip from '../../features/report/components/ReportMetricStrip'
import ReportMetricContextBar from '../../features/report/components/ReportMetricContextBar'
import ReportRankingPanel from '../../features/report/components/ReportRankingPanel'
import ReportTables from '../../features/report/components/ReportTables'
import ReportTrendPanel from '../../features/report/components/ReportTrendPanel'
import { useExportReport } from '../../features/report/hooks/useExportReport'
import { useReportDetailPagination } from '../../features/report/hooks/useReportDetailPagination'
import { useReportDetails } from '../../features/report/hooks/useReportDetails'
import { useReportDimensions } from '../../features/report/hooks/useReportDimensions'
import { useReportOverview } from '../../features/report/hooks/useReportOverview'
import { useReportQueryMetadata } from '../../features/report/hooks/useReportQueryMetadata'
import { useReportMachines, useReportPapers } from '../../features/report/hooks/useReportReferenceData'
import { useReportMetricContext } from '../../features/report/hooks/useReportMetricContext'
import { useCustomers } from '../../features/processOrderCreate/hooks/useReferenceData'
import ReportSubscriptionButton from '../../features/reportSubscription/components/ReportSubscriptionButton'
import { useReportThresholdContext } from '../../features/reportAlert/hooks/useReportThresholdContext'
import ReportAlertRuleButton from '../../features/reportAlert/components/ReportAlertRuleButton'
import ReportAlertEventButton from '../../features/reportAlert/components/ReportAlertEventButton'
import type { ReportDimension, ReportQuery } from '../../types/report'
import { parseReportUrlState, serializeReportUrlState } from './reportUrlState'
import { reportFiltersFromQuery, reportQueryFromFilters } from './reportFilterState'
import { findReportNavigation, resolveReportSourcePath } from './reportNavigation'
import '../documentModule.css'
import './ReportPage.base.css'
import './ReportPage.filters.css'
import './ReportPage.metrics.css'
import './ReportPage.visuals.css'
import './ReportPage.tables.css'
import './ReportPage.responsive.css'

export default function ReportPage() {
  const location = useLocation()
  const [searchParams, setSearchParams] = useSearchParams()
  const urlState = parseReportUrlState(searchParams)
  const initialFilters = reportFiltersFromQuery(urlState.query)
  const query: ReportQuery = {
    ...reportQueryFromFilters(initialFilters), metricReleaseUuid: urlState.query.metricReleaseUuid,
  }
  const reportPage = findReportNavigation(location.pathname)
  const reportPath = resolveReportSourcePath(reportPage?.path)
  const filterFormKey = serializeReportUrlState(query).toString()
  const [dimension, setDimension] = useState<ReportDimension>('customer')
  const detailPagination = useReportDetailPagination()
  const metricContextQuery = useReportMetricContext()
  const metricReady = Boolean(metricContextQuery.data?.releaseUuid)
  const executableQuery = { ...query, metricReleaseUuid: metricContextQuery.data?.releaseUuid }
  const queryMetadataQuery = useReportQueryMetadata(executableQuery, metricReady)
  const overviewQuery = useReportOverview(executableQuery, metricReady)
  const thresholdContextQuery = useReportThresholdContext(executableQuery, metricReady)
  const dimensionQuery = useReportDimensions({ ...executableQuery, dimension }, metricReady)
  const detailsQuery = useReportDetails({ ...executableQuery, ...detailPagination.pagination }, metricReady)
  const monthlyQuery = useReportDimensions({ ...executableQuery, dimension: 'month' }, metricReady)
  const customerRankQuery = useReportDimensions({ ...executableQuery, dimension: 'customer' }, metricReady)
  const productRankQuery = useReportDimensions({ ...executableQuery, dimension: 'paper' }, metricReady)
  const customersQuery = useCustomers()
  const machinesQuery = useReportMachines()
  const papersQuery = useReportPapers()
  const exportMutation = useExportReport()
  const resultQueries = [queryMetadataQuery, overviewQuery, dimensionQuery, detailsQuery, monthlyQuery,
    customerRankQuery, productRankQuery, thresholdContextQuery]
  const loading = resultQueries.some((item) => item.isFetching)
  const resultError = [...resultQueries, metricContextQuery]
    .some((item) => item.isError)
  const referenceError = customersQuery.isError || machinesQuery.isError || papersQuery.isError

  const refresh = () => {
    overviewQuery.refetch()
    dimensionQuery.refetch()
    detailsQuery.refetch()
    monthlyQuery.refetch()
    customerRankQuery.refetch()
    productRankQuery.refetch()
    metricContextQuery.refetch()
    queryMetadataQuery.refetch()
    thresholdContextQuery.refetch()
  }

  const submit = (values: ReportFilterValues) => {
    const nextQuery = { ...reportQueryFromFilters(values), metricReleaseUuid: query.metricReleaseUuid }
    detailPagination.resetPage()
    setSearchParams(serializeReportUrlState(nextQuery))
  }

  const exportCurrent = () => {
    exportMutation.mutate({
      reportPath,
      query: { ...query, dimension, metricReleaseUuid: metricContextQuery.data?.releaseUuid },
    })
  }

  return (
    <div className="report-workbench mes-workbench">
      <Card className="report-filter-card" title={reportPage?.label ?? '统计报表'}>
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
          key={filterFormKey}
          actions={{ export: exportCurrent, refresh, submit }}
          data={{
            customers: customersQuery.data?.records ?? [],
            machines: machinesQuery.data?.records ?? [],
            papers: papersQuery.data?.records ?? [],
          }}
          initialValues={initialFilters}
          status={{ exporting: exportMutation.isPending, loading: customersQuery.isLoading }}
          headerActions={<>
            <ReportAlertEventButton />
            <ReportAlertRuleButton
              customers={customersQuery.data?.records ?? []}
              papers={papersQuery.data?.records ?? []}
            />
            <ReportSubscriptionButton query={{ ...executableQuery, dimension }} reportPath={reportPath} />
          </>}
        />
      </Card>

      <section className="report-overview-result" aria-label="分析结果">
        {resultError && (
          <QueryLoadErrorAlert
            description="部分或全部统计结果未成功加载，当前空图表不代表没有业务数据。"
            message="报表数据加载失败"
            onRetry={refresh}
          />
        )}
        <Spin spinning={loading}>
          <div className="report-workbench__content">
            <div className="report-query-status">
              <ReportFilterSummary
                customers={customersQuery.data?.records ?? []}
                machines={machinesQuery.data?.records ?? []}
                papers={papersQuery.data?.records ?? []}
                query={query}
              />
              <ReportMetricContextBar
                compact
                context={metricContextQuery.data}
                execution={queryMetadataQuery.data}
                loading={metricContextQuery.isLoading || queryMetadataQuery.isLoading}
              />
            </div>
            <ReportMetricStrip overview={overviewQuery.data} />
            <ReportInsightStrip overview={overviewQuery.data} thresholds={thresholdContextQuery.data} />
            <div className="report-workbench__grid">
              <ReportTrendPanel dateFrom={query.dateFrom} dateTo={query.dateTo}
                monthly={monthlyQuery.data ?? []} />
              <ReportRankingPanel
                customers={customerRankQuery.data ?? []}
                products={productRankQuery.data ?? []}
              />
            </div>
            <ReportTables
              details={detailsQuery.data}
              dimension={dimension}
              dimensions={dimensionQuery.data ?? []}
              loading={loading}
              onDetailPageChange={detailPagination.changePage}
              onDimensionChange={setDimension}
              onRefresh={refresh}
            />
          </div>
        </Spin>
      </section>
    </div>
  )
}
