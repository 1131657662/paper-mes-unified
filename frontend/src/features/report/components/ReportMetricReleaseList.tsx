import { Badge, Empty, Skeleton, Tag } from 'antd'
import dayjs from 'dayjs'
import type { ReportMetricReleaseStatus, ReportMetricReleaseSummaryVO } from '../../../types/report'

interface Props {
  activeReleaseUuid: string
  items: ReportMetricReleaseSummaryVO[]
  loading: boolean
  onSelect: (releaseUuid: string) => void
  selectedReleaseUuid: string
}

export default function ReportMetricReleaseList(props: Props) {
  if (props.loading) return <Skeleton active paragraph={{ rows: 5 }} />
  if (props.items.length === 0) {
    return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无发布记录" />
  }
  return <div className="report-metric-release-list">
    {props.items.map((item) => <button type="button" key={item.releaseUuid}
      aria-pressed={item.releaseUuid === props.selectedReleaseUuid}
      className={releaseClass(item.releaseUuid, props.selectedReleaseUuid)}
      onClick={() => props.onSelect(item.releaseUuid)}>
      <span className="report-metric-release-item__head">
        <strong>{item.releaseName}</strong>
        <Badge status={statusTone(item.releaseStatus)} text={statusLabel(item.releaseStatus)} />
      </span>
      <span className="report-metric-release-item__meta">
        <Tag>{item.releaseCode}</Tag>
        {item.releaseUuid === props.activeReleaseUuid && <Tag color="processing">当前使用</Tag>}
      </span>
      <span className="report-metric-release-item__foot">
        {item.metricCount} 个指标 · {formatTime(item.publishedAt ?? item.createTime)}
      </span>
    </button>)}
  </div>
}

function releaseClass(releaseUuid: string, selectedReleaseUuid: string) {
  const base = 'report-metric-release-item'
  return releaseUuid === selectedReleaseUuid ? `${base} ${base}--selected` : base
}

function statusLabel(status: ReportMetricReleaseStatus) {
  if (status === 2) return '生效中'
  if (status === 3) return '已停用'
  return '草稿'
}

function statusTone(status: ReportMetricReleaseStatus): 'processing' | 'default' | 'warning' {
  if (status === 2) return 'processing'
  if (status === 1) return 'warning'
  return 'default'
}

function formatTime(value: string) {
  return dayjs(value).format('YYYY-MM-DD HH:mm')
}
