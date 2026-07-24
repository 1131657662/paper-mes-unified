import { Alert, Card, Spin } from 'antd'
import { useSearchParams } from 'react-router-dom'
import QueryLoadErrorAlert from '../../components/feedback/QueryLoadErrorAlert'
import ReportFilterSummary from '../../features/report/components/ReportFilterSummary'
import ReportMetricContextBar from '../../features/report/components/ReportMetricContextBar'
import ReportOperationalBreakdown from '../../features/report/components/ReportOperationalBreakdown'
import ReportOperationalFilterBar from '../../features/report/components/ReportOperationalFilterBar'
import ReportOperationalMetrics from '../../features/report/components/ReportOperationalMetrics'
import ReportOperationalTrend from '../../features/report/components/ReportOperationalTrend'
import { useReportMetricContext } from '../../features/report/hooks/useReportMetricContext'
import { useReportOperationalAnalysis } from '../../features/report/hooks/useReportOperationalAnalysis'
import { useReportPapers } from '../../features/report/hooks/useReportReferenceData'
import { useReportQueryMetadata } from '../../features/report/hooks/useReportQueryMetadata'
import { useExportReport } from '../../features/report/hooks/useExportReport'
import { useCustomers } from '../../features/processOrderCreate/hooks/useReferenceData'
import ReportSubscriptionButton from '../../features/reportSubscription/components/ReportSubscriptionButton'
import type { ReportOperationalAnalysisVO, ReportOperationalTopicCode } from '../../types/reportOperational'
import { reportFiltersFromQuery, reportQueryFromFilters } from './reportFilterState'
import { findReportNavigation } from './reportNavigation'
import { resolveReportSourcePath } from './reportNavigation'
import { needsOperationalPaperCandidates, reportOperationalQuery } from './reportOperationalQuery'
import { parseReportUrlState, serializeReportUrlState } from './reportUrlState'
import '../documentModule.css'
import './ReportPage.base.css'
import './ReportPage.filters.css'
import './ReportTopicPage.css'
import './ReportOperationalPage.css'
import './ReportPage.responsive.css'

export default function ReportOperationalPage({ topic }: { topic: ReportOperationalTopicCode }) {
  const [params, setParams] = useSearchParams()
  const rawQuery = parseReportUrlState(params).query
  const initialFilters = reportFiltersFromQuery(rawQuery)
  const query = reportOperationalQuery(topic, { ...reportQueryFromFilters(initialFilters),
    metricReleaseUuid: rawQuery.metricReleaseUuid })
  const context = useReportMetricContext()
  const executable = { ...query, metricReleaseUuid: context.data?.releaseUuid }
  const ready = Boolean(executable.metricReleaseUuid)
  const metadata = useReportQueryMetadata(executable, ready)
  const analysis = useReportOperationalAnalysis(topic, executable, ready)
  const customers = useCustomers()
  const needsPapers = needsOperationalPaperCandidates(topic)
  const papers = useReportPapers(needsPapers)
  const navigation = findReportNavigation('/reports/' + topic)
  const reportPath = resolveReportSourcePath(navigation?.path)
  const exportMutation = useExportReport()
  const refresh = () => {
    void context.refetch(); void metadata.refetch(); void analysis.refetch(); void customers.refetch()
    if (needsPapers) void papers.refetch()
  }
  const submit = (values: Parameters<typeof reportQueryFromFilters>[0]) => {
    const next = reportOperationalQuery(topic, reportQueryFromFilters(values))
    setParams(serializeReportUrlState(next))
  }
  return <main className="report-workbench report-topic-page report-operational-page mes-workbench">
    <Card className="report-filter-card" title={navigation?.label}>
      <ReportOperationalFilterBar
        actions={{ export: () => exportMutation.mutate({ query: executable, reportPath }), refresh, submit }}
        data={{ customers: customers.data?.records ?? [], papers: papers.data?.records ?? [] }}
        headerActions={<ReportSubscriptionButton query={executable} reportPath={reportPath} />}
        initialValues={initialFilters}
        status={{ exporting: exportMutation.isPending, loading: customers.isLoading }} topic={topic} />
    </Card>
    <section className="report-topic-result report-operational-result" aria-label="分析结果">
      {(customers.isError || (needsPapers && papers.isError)) && <QueryLoadErrorAlert message="筛选资料加载失败"
        description="客户或纸张候选项不完整，请重试后再查询。" onRetry={refresh} />}
      {analysis.isError && <QueryLoadErrorAlert message="专题报表加载失败"
        description="当前空白不代表没有业务数据，请重试查询。" onRetry={refresh} />}
      <Spin spinning={analysis.isFetching || metadata.isFetching}>
        <div className="report-workbench__content">
          <div className="report-query-status">
            <ReportFilterSummary customers={customers.data?.records ?? []} machines={[]}
              mode={topic} papers={papers.data?.records ?? []} query={query} />
            <ReportMetricContextBar compact context={context.data} execution={metadata.data}
              loading={context.isLoading || metadata.isLoading} />
          </div>
          {analysis.data && <OperationalContent analysis={analysis.data} />}
        </div>
      </Spin>
    </section>
  </main>
}

function OperationalContent({ analysis }: { analysis: ReportOperationalAnalysisVO }) {
  return <>
    {analysis.topicCode === 'inventory' && <Alert className="report-operational-scope-note" showIcon type="info"
      message="当前库存快照"
      description="日期筛选作用于首次入库时间；趋势展示当前仍在库成品的入库批次，不代表历史月末库存余额。" />}
    <ReportOperationalMetrics analysis={analysis} />
    <div className="report-operational-grid">
      <ReportOperationalTrend analysis={analysis} />
      <ReportOperationalBreakdown analysis={analysis} />
    </div>
  </>
}
