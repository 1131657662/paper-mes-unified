import { Card, Spin } from 'antd'
import QueryLoadErrorAlert from '../../components/feedback/QueryLoadErrorAlert'
import ReportFilterBar, { type ReportFilterValues } from '../../features/report/components/ReportFilterBar'
import ReportFilterSummary from '../../features/report/components/ReportFilterSummary'
import ReportLossLeaders from '../../features/report/components/ReportLossLeaders'
import ReportMetricContextBar from '../../features/report/components/ReportMetricContextBar'
import ReportTopicBreakdown from '../../features/report/components/ReportTopicBreakdown'
import ReportTopicMetrics from '../../features/report/components/ReportTopicMetrics'
import ReportTopicTrend from '../../features/report/components/ReportTopicTrend'
import { useExportReport } from '../../features/report/hooks/useExportReport'
import { useReportMachines, useReportPapers } from '../../features/report/hooks/useReportReferenceData'
import { useReportMetricContext } from '../../features/report/hooks/useReportMetricContext'
import { useReportQueryMetadata } from '../../features/report/hooks/useReportQueryMetadata'
import { useReportTopicAnalysis } from '../../features/report/hooks/useReportTopicAnalysis'
import type { ReportTopicCode } from '../../features/report/services/reportService'
import { useCustomers } from '../../features/processOrderCreate/hooks/useReferenceData'
import ReportSubscriptionButton from '../../features/reportSubscription/components/ReportSubscriptionButton'
import type { ReportQuery, ReportTopicAnalysisVO } from '../../types/report'
import { useSearchParams } from 'react-router-dom'
import { reportFiltersFromQuery, reportQueryFromFilters } from './reportFilterState'
import { findReportNavigation, resolveReportSourcePath } from './reportNavigation'
import { parseReportUrlState, serializeReportUrlState } from './reportUrlState'
import { reportTopicQuery } from './reportTopicQuery'
import '../documentModule.css'
import './ReportPage.base.css'
import './ReportPage.filters.css'
import './ReportTopicPage.css'
import './ReportPage.responsive.css'

export default function ReportTopicPage({ topic }: { topic: ReportTopicCode }) {
  const [searchParams, setSearchParams] = useSearchParams()
  const urlQuery = parseReportUrlState(searchParams).query
  const initialFilters = reportFiltersFromQuery(urlQuery)
  const query: ReportQuery = reportTopicQuery({ ...reportQueryFromFilters(initialFilters),
    metricReleaseUuid: urlQuery.metricReleaseUuid })
  const contextQuery = useReportMetricContext()
  const executable = { ...query, metricReleaseUuid: contextQuery.data?.releaseUuid }
  const ready = Boolean(executable.metricReleaseUuid)
  const metadataQuery = useReportQueryMetadata(executable, ready)
  const analysisQuery = useReportTopicAnalysis(topic, executable, ready)
  const customersQuery = useCustomers()
  const machinesQuery = useReportMachines()
  const papersQuery = useReportPapers()
  const exportMutation = useExportReport()
  const navigation = findReportNavigation(`/reports/${topic}`)
  const reportPath = resolveReportSourcePath(navigation?.path)
  const referenceError = customersQuery.isError || machinesQuery.isError || papersQuery.isError

  const refresh = () => {
    void contextQuery.refetch()
    void metadataQuery.refetch()
    void analysisQuery.refetch()
  }
  const submit = (values: ReportFilterValues) => {
    setSearchParams(serializeReportUrlState(reportTopicQuery(reportQueryFromFilters(values))))
  }
  const exportCurrent = () => exportMutation.mutate({ query: executable, reportPath })

  return (
    <div className="report-workbench report-topic-page mes-workbench">
      <Card className="report-filter-card" title={navigation?.label}>
        {referenceError && <QueryLoadErrorAlert message="筛选资料加载失败"
          description="客户、纸张或机台候选项不完整，请重试后再查询。" onRetry={refresh} />}
        <ReportFilterBar
          key={serializeReportUrlState(query).toString()}
          actions={{ export: exportCurrent, refresh, submit }}
          data={{ customers: customersQuery.data?.records ?? [], machines: machinesQuery.data?.records ?? [],
            papers: papersQuery.data?.records ?? [] }}
          initialValues={initialFilters}
          mode={topic}
          status={{ exporting: exportMutation.isPending, loading: customersQuery.isLoading }}
          headerActions={<ReportSubscriptionButton query={executable} reportPath={reportPath} />}
        />
      </Card>
      <section className="report-topic-result" aria-label="分析结果">
        {analysisQuery.isError && <QueryLoadErrorAlert message="主题报表加载失败"
          description="当前空图表不代表没有业务数据，请重试查询。" onRetry={refresh} />}
        <Spin spinning={analysisQuery.isFetching || metadataQuery.isFetching}>
          <div className="report-workbench__content">
            <div className="report-query-status">
              <ReportFilterSummary customers={customersQuery.data?.records ?? []}
                machines={machinesQuery.data?.records ?? []} mode={topic}
                papers={papersQuery.data?.records ?? []} query={query} />
              <ReportMetricContextBar compact context={contextQuery.data} execution={metadataQuery.data}
                loading={contextQuery.isLoading || metadataQuery.isLoading} />
            </div>
            {analysisQuery.data && <TopicContent analysis={analysisQuery.data} />}
          </div>
        </Spin>
      </section>
    </div>
  )
}

function TopicContent({ analysis }: { analysis: ReportTopicAnalysisVO }) {
  return <>
    <ReportTopicMetrics analysis={analysis} />
    <ReportTopicTrend rows={analysis.monthlyTrend}
      title={analysis.topicCode === 'production' ? '月度投入与产出' : '月度损耗趋势'} />
    {analysis.topicCode === 'production'
      ? <div className="report-topic-grid"><ReportTopicBreakdown title="工艺结构"
          rows={analysis.processBreakdown} storageKey="report-production-process" />
          <ReportTopicBreakdown title="机台负荷" rows={analysis.machineBreakdown}
            storageKey="report-production-machine" /></div>
      : <><ReportTopicBreakdown title="纸种损耗结构" pageSize={6} rows={analysis.paperBreakdown}
          storageKey="report-quality-paper" /><ReportLossLeaders rows={analysis.lossLeaders} /></>}
  </>
}
