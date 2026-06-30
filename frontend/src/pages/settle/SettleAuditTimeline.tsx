import { Empty, Tag, Timeline, Typography } from 'antd'
import type { OperationLog } from '../../types/operationLog'

interface Props {
  logs: OperationLog[]
}

export default function SettleAuditTimeline({ logs }: Props) {
  if (logs.length === 0) {
    return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无业务追踪记录" />
  }

  return (
    <Timeline
      className="settle-audit-timeline"
      items={logs.map((log) => ({
        color: toneColor(log.actionType),
        children: <AuditItem log={log} />,
      }))}
    />
  )
}

function AuditItem({ log }: { log: OperationLog }) {
  return (
    <div className="settle-audit-timeline__item">
      <div className="settle-audit-timeline__head">
        <Tag className="mes-status-tag" color={toneColor(log.actionType)}>{log.actionType || '操作'}</Tag>
        <Typography.Text strong>{log.operator || '-'}</Typography.Text>
        <span>{log.operateTime || '-'}</span>
      </div>
      {log.remark && <p>{log.remark}</p>}
      {log.fieldName && (
        <p>
          {log.fieldName}：{log.oldValue || '-'} → {log.newValue || '-'}
        </p>
      )}
    </div>
  )
}

function toneColor(actionType?: string) {
  if (actionType?.includes('收款')) return 'green'
  if (actionType?.includes('回退') || actionType?.includes('作废') || actionType?.includes('撤销')) return 'orange'
  if (actionType?.includes('结算')) return 'blue'
  return 'default'
}
