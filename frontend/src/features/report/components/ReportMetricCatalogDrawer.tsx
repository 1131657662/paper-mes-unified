import { Alert, Button, Drawer } from 'antd'
import { useState } from 'react'
import type { ReportMetricContextVO } from '../../../types/report'
import { useReportMetricReleases } from '../hooks/useReportMetricReleases'
import ReportMetricReleaseList from './ReportMetricReleaseList'
import ReportMetricReleasePanel from './ReportMetricReleasePanel'
import './ReportMetricCatalog.css'

interface Props {
  context: ReportMetricContextVO
  onClose: () => void
  open: boolean
}

export default function ReportMetricCatalogDrawer({ context, onClose, open }: Props) {
  const [selectedReleaseUuid, setSelectedReleaseUuid] = useState(context.releaseUuid)
  const { data: releases = [], isLoading: isLoadingReleases,
    isError: isReleasesError, refetch: refetchReleases } = useReportMetricReleases(open)
  return <Drawer className="report-metric-catalog-drawer" open={open} onClose={onClose}
    title="指标口径审计" width="min(1120px, calc(100vw - 24px))">
    <Alert className="report-metric-catalog-hint" type="info" showIcon
      message="每个发布包固定绑定逐指标版本；历史口径只读保留，便于追溯报表计算依据。" />
    {isReleasesError && <Alert className="report-metric-catalog-error" type="error" showIcon
      message="发布历史加载失败" action={<Button type="link" size="small"
        onClick={() => void refetchReleases()}>重试</Button>} />}
    <div className="report-metric-catalog-layout">
      <aside className="report-metric-catalog-history">
        <div className="report-metric-catalog-history__head">
          <strong>发布历史</strong><span>{releases.length} 个版本包</span>
        </div>
        <ReportMetricReleaseList activeReleaseUuid={context.releaseUuid} items={releases}
          loading={isLoadingReleases} onSelect={setSelectedReleaseUuid}
          selectedReleaseUuid={selectedReleaseUuid} />
      </aside>
      <ReportMetricReleasePanel enabled={open} releaseUuid={selectedReleaseUuid} />
    </div>
  </Drawer>
}
