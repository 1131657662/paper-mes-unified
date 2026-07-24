import { Alert, Button, Skeleton, Statistic } from 'antd'
import { useSearchParams } from 'react-router-dom'
import ReportMetricReleaseList from '../../features/report/components/ReportMetricReleaseList'
import ReportMetricReleasePanel from '../../features/report/components/ReportMetricReleasePanel'
import { useReportMetricContext } from '../../features/report/hooks/useReportMetricContext'
import { useReportMetricReleases } from '../../features/report/hooks/useReportMetricReleases'
import '../../features/report/components/ReportMetricCatalog.css'
import './ReportManagementPage.css'

export default function ReportMetricManagementPage() {
  const [params, setParams] = useSearchParams()
  const context = useReportMetricContext()
  const releases = useReportMetricReleases(true)
  const selectedReleaseUuid = selectedRelease(params.get('release'), context.data?.releaseUuid, releases.data ?? [])
  const selectRelease = (releaseUuid: string) => {
    const next = new URLSearchParams(params); next.set('release', releaseUuid); setParams(next, { replace: true })
  }
  const retry = () => { void context.refetch(); void releases.refetch() }
  return <main className="report-management report-metric-management mes-workbench">
    <header className="report-management__header">
      <div><h1>指标口径与版本审计</h1><p>查看当前生效口径、历史发布包及每个指标的锁定版本。</p></div>
    </header>
    <section className="report-management__summary" aria-label="指标口径概况">
      <Statistic title="当前发布包" value={context.data?.releaseCode ?? '-'} />
      <Statistic title="原子指标" value={context.data?.metrics.length ?? 0} suffix="项" />
      <Statistic title="历史发布包" value={releases.data?.length ?? 0} suffix="个" />
      <Statistic title="一致性" value={context.data?.releaseChecksum ? '已校验' : '-'} />
    </section>
    {(context.isError || releases.isError) && <Alert showIcon type="error" message="指标版本资料加载失败"
      action={<Button size="small" onClick={retry}>重试</Button>} />}
    <Alert showIcon type="info" message="发布包一经生效即锁定指标版本；历史口径只读保留，用于报表结果追溯与审计。" />
    <section className="report-management__panel report-metric-management__panel">
      <div className="report-metric-catalog-layout">
        <aside className="report-metric-catalog-history">
          <div className="report-metric-catalog-history__head"><strong>发布历史</strong><span>{releases.data?.length ?? 0} 个版本包</span></div>
          <ReportMetricReleaseList activeReleaseUuid={context.data?.releaseUuid ?? ''} items={releases.data ?? []}
            loading={releases.isLoading} onSelect={selectRelease} selectedReleaseUuid={selectedReleaseUuid} />
        </aside>
        {selectedReleaseUuid ? <ReportMetricReleasePanel enabled releaseUuid={selectedReleaseUuid} /> :
          <Skeleton active paragraph={{ rows: 8 }} />}
      </div>
    </section>
  </main>
}

function selectedRelease(requested: string | null, active: string | undefined,
  releases: Array<{ releaseUuid: string }>): string {
  if (requested && releases.some((item) => item.releaseUuid === requested)) return requested
  return active ?? releases[0]?.releaseUuid ?? ''
}
