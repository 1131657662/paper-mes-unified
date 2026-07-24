import { DeleteOutlined, EditOutlined, HistoryOutlined } from '@ant-design/icons'
import { Badge, Button, Empty, Popconfirm, Table, Tag, Tooltip, Typography } from 'antd'
import dayjs from 'dayjs'
import type { ColumnsType } from 'antd/es/table'
import type { ReportSubscription } from '../types'
import { periodOptions, scheduleLabel } from './reportSubscriptionLabels'

interface Props {
  deleting: boolean
  items: ReportSubscription[]
  onDelete: (item: ReportSubscription) => void
  onEdit: (item: ReportSubscription) => void
  onHistory: (item: ReportSubscription) => void
}

export default function ReportSubscriptionList({ deleting, items, onDelete, onEdit, onHistory }: Props) {
  if (items.length === 0) {
    return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无报表订阅" />
  }
  return <Table<ReportSubscription> className="report-subscription-table"
    columns={columns({ deleting, onDelete, onEdit, onHistory })} dataSource={items}
    pagination={false} rowKey="uuid" scroll={{ x: 800 }} size="small" />
}

function columns(actions: Pick<Props, 'deleting' | 'onDelete' | 'onEdit' | 'onHistory'>): ColumnsType<ReportSubscription> {
  return [
    { title: '订阅', dataIndex: 'subscriptionName', width: 190,
      render: (_, item) => <SubscriptionName item={item} /> },
    { title: '计划', width: 136,
      render: (_, item) => <><strong>{scheduleLabel(item)}</strong><br />
        <Typography.Text type="secondary">{periodLabel(item.periodPolicy)}</Typography.Text></> },
    { title: '接收人', width: 170,
      render: (_, item) => <div className="report-subscription-recipients">
        {item.recipients.map((recipient) => <Tag key={recipient.uuid}>{recipient.displayName}</Tag>)}
      </div> },
    { title: '下次执行', dataIndex: 'nextRunAt', width: 146,
      render: (value: string) => dayjs(value).format('YYYY-MM-DD HH:mm') },
    { title: '操作', fixed: 'right', width: 116, align: 'center',
      render: (_, item) => <div className="report-subscription-actions">
        <Tooltip title="运行记录"><Button aria-label="运行记录" type="text" icon={<HistoryOutlined />}
          onClick={() => actions.onHistory(item)} /></Tooltip>
        <Tooltip title="编辑"><Button aria-label="编辑订阅" type="text" icon={<EditOutlined />}
          onClick={() => actions.onEdit(item)} /></Tooltip>
        <Popconfirm title="删除此订阅？" description="已生成的下载任务不会被删除。"
          onConfirm={() => actions.onDelete(item)}>
          <Tooltip title="删除"><Button aria-label="删除订阅" danger type="text"
            loading={actions.deleting} icon={<DeleteOutlined />} /></Tooltip>
        </Popconfirm>
      </div> },
  ]
}

function SubscriptionName({ item }: { item: ReportSubscription }) {
  return <div className="report-subscription-name">
    <strong>{item.subscriptionName}</strong>
    <span><Badge status={item.isEnabled ? 'processing' : 'default'} text={item.isEnabled ? '已启用' : '已停用'} /></span>
    {item.lastErrorMessage && <Typography.Text type="danger" ellipsis={{ tooltip: item.lastErrorMessage }}>
      {item.lastErrorMessage}
    </Typography.Text>}
  </div>
}

function periodLabel(value: number) {
  return periodOptions.find((item) => item.value === value)?.label ?? '未知周期'
}
