import { Tag } from 'antd'
import dayjs from 'dayjs'
import type { ReportMetricContextVO, ReportQueryExecutionMetaVO } from '../../../types/report'
import ReportMetricCatalogButton from './ReportMetricCatalogButton'

interface Props {
  compact?: boolean
  context?: ReportMetricContextVO
  execution?: ReportQueryExecutionMetaVO
  loading: boolean
}

export default function ReportMetricContextBar({ compact = false, context, execution, loading }: Props) {
  if (loading) {
    return <div className="report-metric-context report-metric-context--loading">正在核对指标口径...</div>
  }
  if (!context) return null

  return (
    <div className={`report-metric-context${compact ? ' report-metric-context--compact' : ''}`}
      aria-label="报表指标口径">
      <span className="report-metric-context__label">指标口径</span>
      <strong title={context.releaseName}>{context.releaseName}</strong>
      {!compact && <Tag>{context.releaseCode}</Tag>}
      {!compact && <span>{context.metrics.length} 个原子指标</span>}
      <Tag color={execution?.consistencyMode === 'MATERIALIZED' ? 'green' : 'blue'}>
        {execution?.consistencyMode === 'MATERIALIZED' ? '物化口径' : '实时口径'}
      </Tag>
      <span className="report-metric-context__as-of">
        数据截至 {formatTime(execution?.dataAsOf ?? context.asOf)}
      </span>
      <ReportMetricCatalogButton context={context} />
    </div>
  )
}

function formatTime(value: string) {
  return dayjs(value).format('YYYY-MM-DD HH:mm:ss')
}
