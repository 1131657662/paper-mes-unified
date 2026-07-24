import { SaveOutlined } from '@ant-design/icons'
import { Button, Card, Skeleton } from 'antd'
import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import QueryLoadErrorAlert from '../../components/feedback/QueryLoadErrorAlert'
import ReportFilterBar, { type ReportFilterValues } from '../../features/report/components/ReportFilterBar'
import ReportFilterSummary from '../../features/report/components/ReportFilterSummary'
import ReportMetricContextBar from '../../features/report/components/ReportMetricContextBar'
import { useReportMachines, useReportPapers } from '../../features/report/hooks/useReportReferenceData'
import { useReportMetricContext } from '../../features/report/hooks/useReportMetricContext'
import { useReportDimensionAnalysis } from '../../features/report/hooks/useReportDimensionAnalysis'
import { useExportReport } from '../../features/report/hooks/useExportReport'
import { useCustomers } from '../../features/processOrderCreate/hooks/useReferenceData'
import ReportSubscriptionButton from '../../features/reportSubscription/components/ReportSubscriptionButton'
import type { ReportDimension, ReportQuery } from '../../types/report'
import { reportFiltersFromQuery, reportQueryFromFilters } from './reportFilterState'
import { parseReportUrlState, serializeReportUrlState } from './reportUrlState'
import ReportExplorerControls from './ReportExplorerControls'
import ReportExplorerTable from './ReportExplorerTable'
import ReportSavedViewModal from '../../features/reportSavedView/components/ReportSavedViewModal'
import { drillQuery, explorerMetricDefaults, selectedMetricItems } from './reportExplorerModel'
import '../documentModule.css'
import './ReportPage.base.css'
import './ReportPage.filters.css'
import './ReportExplorerPage.css'
import './ReportPage.responsive.css'

export default function ReportExplorerPage() {
  const [params, setParams] = useSearchParams()
  const baseQuery = parseReportUrlState(params).query
  const context = useReportMetricContext()
  const query: ReportQuery = { ...reportQueryFromFilters(reportFiltersFromQuery(baseQuery)),
    processStepType: baseQuery.processStepType, metricReleaseUuid: context.data?.releaseUuid }
  const [dimension, setDimension] = useState<ReportDimension>(readDimension(params))
  const [metricCodes, setMetricCodes] = useState<string[]>(readMetricCodes(params))
  const [saveModalOpen, setSaveModalOpen] = useState(false)
  const analysis = useReportDimensionAnalysis({ ...query, dimension }, Boolean(query.metricReleaseUuid))
  const customers = useCustomers()
  const machines = useReportMachines()
  const papers = useReportPapers()
  const exportMutation = useExportReport()
  const refresh = () => { void context.refetch(); void analysis.refetch() }
  const submit = (values: ReportFilterValues) => setParams(withExplorerState(reportQueryFromFilters(values), dimension, metricCodes))
  const updateExplorer = (nextDimension: ReportDimension, nextMetrics: string[]) => {
    setDimension(nextDimension); setMetricCodes(nextMetrics)
    const next = new URLSearchParams(params)
    next.set('dimension', nextDimension); next.set('metrics', nextMetrics.join(',')); setParams(next)
  }
  const metricItems = selectedMetricItems(context.data?.metrics ?? [], metricCodes)
  const effectiveMetricItems = metricItems.length ? metricItems : selectedMetricItems(context.data?.metrics ?? [], explorerMetricDefaults)
  const executable = { ...query, dimension }
  const drill = (key: string) => {
    const next = drillQuery(dimension, key, executable)
    if (next) setParams(withExplorerState(next, dimension, metricCodes))
  }
  return <main className="report-workbench report-explorer-page mes-workbench">
    <Card className="report-filter-card" title="多维分析">
      {(customers.isError || machines.isError || papers.isError) && <QueryLoadErrorAlert message="筛选资料加载失败"
        description="客户、纸张或机台候选不完整，请刷新后重试。" onRetry={refresh} />}
      <ReportFilterBar actions={{ export: () => exportMutation.mutate({
        query: executable, reportPath: '/reports/explorer',
      }), refresh, submit }}
        data={{ customers: customers.data?.records ?? [], machines: machines.data?.records ?? [], papers: papers.data?.records ?? [] }}
        initialValues={reportFiltersFromQuery(baseQuery)} mode="explorer"
        headerActions={<><ReportSubscriptionButton query={executable} reportPath="/reports/explorer" />
          <Button icon={<SaveOutlined />} onClick={() => setSaveModalOpen(true)}>保存视图</Button></>}
        status={{ exporting: exportMutation.isPending, loading: customers.isLoading }} />
    </Card>
    <section className="report-explorer-result" aria-label="组合结果">
      <div className="report-query-status">
        <ReportFilterSummary customers={customers.data?.records ?? []} machines={machines.data?.records ?? []}
          mode="explorer" papers={papers.data?.records ?? []} query={query} />
        <ReportMetricContextBar compact context={context.data} execution={analysis.data?.execution}
          loading={context.isLoading || analysis.isLoading} />
      </div>
      {!context.isError && context.data && <ReportExplorerControls dimension={dimension}
        metricCodes={effectiveMetricItems.map((item) => item.metricCode)} metrics={context.data.metrics} onChange={updateExplorer} />}
      {analysis.isError && <QueryLoadErrorAlert message="多维结果加载失败"
        description="请刷新后重试，当前筛选条件不会丢失。" onRetry={refresh} />}
      {analysis.isLoading ? <Skeleton active paragraph={{ rows: 8 }} /> : <ReportExplorerTable dimension={dimension}
        metrics={effectiveMetricItems} rows={analysis.data?.rows ?? []} onDrill={drill} />}
    </section>
    <ReportSavedViewModal open={saveModalOpen} baseQuery={query}
      defaults={{ reportPath: '/reports/explorer', dimensionCode: dimension,
        metricCodes: effectiveMetricItems.map((item) => item.metricCode) }}
      onClose={() => setSaveModalOpen(false)} onSaved={() => setSaveModalOpen(false)} />
  </main>
}

function readDimension(params: URLSearchParams): ReportDimension {
  const value = params.get('dimension') as ReportDimension | null
  return value && ['month', 'customer', 'paper', 'process', 'machine', 'invoice', 'settleType', 'status'].includes(value) ? value : 'customer'
}

function readMetricCodes(params: URLSearchParams) { return params.get('metrics')?.split(',').filter(Boolean) ?? explorerMetricDefaults }
function withExplorerState(query: ReportQuery, dimension: ReportDimension, metricCodes: string[]) {
  const next = serializeReportUrlState(query)
  next.set('dimension', dimension)
  next.set('metrics', metricCodes.join(','))
  return next
}
