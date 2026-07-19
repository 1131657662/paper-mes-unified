import { Empty, Tag, Timeline, Typography } from 'antd'
import dayjs from 'dayjs'
import type { SettleCollectionReminder } from '../../types/settle'
import QueryLoadErrorAlert from '../../components/feedback/QueryLoadErrorAlert'
import './SettleCollectionReminderHistory.css'

interface Props {
  error?: boolean
  items: SettleCollectionReminder[]
  loading: boolean
  onRetry: () => void
}

const CHANNEL_LABELS: Record<number, string> = { 1: '电话', 2: '微信', 3: '短信', 4: '上门', 5: '其他' }
const RESULT_LABELS: Record<number, string> = {
  1: '已联系', 2: '未接通', 3: '承诺付款', 4: '金额或业务有异议', 5: '其他',
}

export default function SettleCollectionReminderHistory({ error, items, loading, onRetry }: Props) {
  if (error) {
    return <QueryLoadErrorAlert message="催收记录加载失败" description="通用业务追踪仍可查看，可重试加载催收历史。" onRetry={onRetry} />
  }
  if (loading) return <div className="settle-reminder-history__loading">正在加载催收记录...</div>
  if (items.length === 0) return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无催收记录" />

  return <Timeline className="settle-reminder-history" items={items.map((item) => ({
    color: reminderTone(item.reminderResult),
    children: <ReminderItem item={item} />,
  }))} />
}

function ReminderItem({ item }: { item: SettleCollectionReminder }) {
  const nextFollowUp = item.nextFollowUpDate ? `下次跟进 ${item.nextFollowUpDate}` : '未设置下次跟进'
  return (
    <div className="settle-reminder-history__item">
      <div className="settle-reminder-history__head">
        <Tag color={reminderTone(item.reminderResult)}>{CHANNEL_LABELS[item.reminderChannel] ?? '其他'}</Tag>
        <Typography.Text strong>{RESULT_LABELS[item.reminderResult] ?? '其他'}</Typography.Text>
        <Typography.Text type="secondary">{item.operatorName || '-'}</Typography.Text>
        <Typography.Text type="secondary">{formatDateTime(item.reminderTime)}</Typography.Text>
      </div>
      <div className="settle-reminder-history__meta">
        {item.contactName && <span>联系人：{item.contactName}</span>}
        <span>{nextFollowUp}</span>
      </div>
      <Typography.Paragraph className="settle-reminder-history__remark" ellipsis={{ rows: 3, expandable: true }}>
        {item.remark}
      </Typography.Paragraph>
    </div>
  )
}

function formatDateTime(value?: string) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-'
}

function reminderTone(result?: number): 'green' | 'orange' | 'blue' {
  if (result === 3) return 'green'
  if (result === 4) return 'orange'
  return 'blue'
}
