import { ClockCircleOutlined, InfoCircleOutlined } from '@ant-design/icons'
import { Descriptions, Tag, Typography } from 'antd'
import dayjs from 'dayjs'
import type { ReportAlertEvent } from '../types'
import { eventStatusColors, eventStatusLabels, operatorLabel, signalLabel } from './reportAlertEventLabels'
import ReportAlertEventActions from './ReportAlertEventActions'

interface Props {
  canManage: boolean
  event: ReportAlertEvent
  busy: boolean
  onAcknowledge: (uuid: string) => void
  onIgnore: (uuid: string, reason: string) => void
}

export default function ReportAlertEventCard({ canManage, event, busy, onAcknowledge, onIgnore }: Props) {
  const status = event.eventStatus
  return <article className={`report-alert-event report-alert-event--${status}`}>
    <header className="report-alert-event__header">
      <div className="report-alert-event__title">
        <Tag color={eventStatusColors[status]}>{eventStatusLabels[status]}</Tag>
        <Typography.Text strong ellipsis={{ tooltip: event.ruleName }}>{event.ruleName}</Typography.Text>
      </div>
      {canManage && status === 1 && <ReportAlertEventActions acknowledged={Boolean(event.acknowledgedAt)}
        busy={busy} onAcknowledge={() => onAcknowledge(event.uuid)}
        onIgnore={(reason) => onIgnore(event.uuid, reason)} />}
    </header>
    <div className="report-alert-event__metric">
      <strong>{formatValue(event.metricValue)}%</strong>
      <span>{signalLabel(event.signalCode)} {operatorLabel(event.comparisonOperator)} {formatValue(event.thresholdValue)}%</span>
      <Tag color={event.severity === 2 ? 'error' : 'warning'}>{event.severity === 2 ? '严重' : '预警'}</Tag>
    </div>
    <Descriptions className="report-alert-event__details" size="small" column={2}>
      <Descriptions.Item label="作用范围">{event.scopeLabel}</Descriptions.Item>
      <Descriptions.Item label="指标版本"><Typography.Text code ellipsis={{ tooltip: event.metricReleaseUuid }}>
        {shortRelease(event.metricReleaseUuid)}
      </Typography.Text></Descriptions.Item>
      <Descriptions.Item label="统计周期">{event.periodStart} 至 {event.periodEnd}</Descriptions.Item>
      <Descriptions.Item label="发生次数">{event.occurrenceCount} 次</Descriptions.Item>
      <Descriptions.Item label="最近发现"><ClockCircleOutlined /> {formatTime(event.lastDetectedAt)}</Descriptions.Item>
    </Descriptions>
    {event.ignoredAt && <p className="report-alert-event__audit"><InfoCircleOutlined /> {event.ignoredBy ?? '用户'} 于 {formatTime(event.ignoredAt)} 忽略：{event.ignoreReason}</p>}
    {event.acknowledgedAt && !event.ignoredAt && <p className="report-alert-event__audit"><InfoCircleOutlined /> {event.acknowledgedBy ?? '用户'} 于 {formatTime(event.acknowledgedAt)} 已确认</p>}
  </article>
}

function formatValue(value: number) {
  return value.toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

function formatTime(value: string) {
  return dayjs(value).format('YYYY-MM-DD HH:mm')
}

function shortRelease(value: string) {
  return value.slice(0, 8)
}
