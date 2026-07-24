import { Alert, Button, Descriptions, Skeleton, Tag, Typography } from 'antd'
import dayjs from 'dayjs'
import { useReportMetricRelease } from '../hooks/useReportMetricRelease'
import type { ReportMetricReleaseSummaryVO } from '../../../types/report'
import ReportMetricVersionTable from './ReportMetricVersionTable'

interface Props {
  enabled: boolean
  releaseUuid: string
}

export default function ReportMetricReleasePanel({ enabled, releaseUuid }: Props) {
  const { data: releaseDetail, isLoading: isLoadingRelease,
    isError: isReleaseError, refetch: refetchRelease } = useReportMetricRelease(releaseUuid, enabled)
  if (isLoadingRelease) return <Skeleton active paragraph={{ rows: 8 }} />
  if (isReleaseError) return <Alert type="error" showIcon message="发布包明细加载失败"
    action={<Button type="link" size="small" onClick={() => void refetchRelease()}>重试</Button>} />
  if (!releaseDetail) return null
  const release = releaseDetail.release
  return <section className="report-metric-release-panel">
    <div className="report-metric-release-panel__head">
      <div><strong>{release.releaseName}</strong><Tag>{release.releaseCode}</Tag></div>
      <span>数据截至 {formatTime(release.asOf)}</span>
    </div>
    <ReleaseMetadata release={release} />
    <ReportMetricVersionTable items={releaseDetail.metrics} />
  </section>
}

function ReleaseMetadata({ release }: { release: ReportMetricReleaseSummaryVO }) {
  return <Descriptions className="report-metric-release-metadata" column={2} size="small">
    <Descriptions.Item label="发布状态">{statusLabel(release.releaseStatus)}</Descriptions.Item>
    <Descriptions.Item label="指标数量">{release.metricCount}</Descriptions.Item>
    <Descriptions.Item label="发布时间">{formatTime(release.publishedAt)}</Descriptions.Item>
    <Descriptions.Item label="发布人">{release.publishedBy || '-'}</Descriptions.Item>
    <Descriptions.Item label="发布校验" span={2}>
      <Typography.Text copyable ellipsis={{ tooltip: release.releaseChecksum }}>
        {release.releaseChecksum || '-'}
      </Typography.Text>
    </Descriptions.Item>
  </Descriptions>
}

function statusLabel(status: number) {
  if (status === 2) return '生效中'
  if (status === 3) return '已停用'
  return '草稿'
}

function formatTime(value?: string) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-'
}
