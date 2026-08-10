import { Tag } from 'antd'
import dayjs from 'dayjs'
import timezone from 'dayjs/plugin/timezone'
import utc from 'dayjs/plugin/utc'
import { DISPLAY_TERMS } from '../../../constants/displayTerms'
import type {
  ReportMetricContextVO,
  ReportQueryExecutionMetaVO,
} from '../../../types/report'
import ReportMetricCatalogButton from './ReportMetricCatalogButton'

dayjs.extend(utc)
dayjs.extend(timezone)

const MES_TIME_ZONE = 'Asia/Shanghai'

interface Props {
  compact?: boolean
  context?: ReportMetricContextVO
  execution?: ReportQueryExecutionMetaVO
  loading: boolean
}

export default function ReportMetricContextBar({
  compact = false,
  context,
  execution,
  loading,
}: Props) {
  if (loading) {
    return (
      <div className="report-metric-context report-metric-context--loading">
        正在核对{DISPLAY_TERMS.metricVersion}...
      </div>
    )
  }
  if (!context) return null

  return (
    <div
      className={`report-metric-context${compact ? ' report-metric-context--compact' : ''}`}
      aria-label={`报表${DISPLAY_TERMS.metricVersion}`}
    >
      <span className="report-metric-context__label">
        {DISPLAY_TERMS.metricVersion}
      </span>
      <strong title={context.releaseName}>{context.releaseName}</strong>
      {!compact && <Tag>{context.releaseCode}</Tag>}
      {!compact && <span>{context.metrics.length} 个原子指标</span>}
      <Tag
        color={execution?.consistencyMode === 'MATERIALIZED' ? 'green' : 'blue'}
      >
        {execution?.consistencyMode === 'MATERIALIZED'
          ? DISPLAY_TERMS.precomputedResult
          : DISPLAY_TERMS.realtimeCalculation}
      </Tag>
      <span className="report-metric-context__as-of">
        数据截至 {formatTime(execution?.dataAsOf ?? context.asOf)}
      </span>
      <ReportMetricCatalogButton context={context} />
    </div>
  )
}

function formatTime(value: string) {
  return dayjs(value).tz(MES_TIME_ZONE).format('YYYY-MM-DD HH:mm:ss')
}
