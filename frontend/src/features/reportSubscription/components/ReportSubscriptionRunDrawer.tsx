import { PlayCircleOutlined, ReloadOutlined } from '@ant-design/icons'
import { Alert, Button, Drawer, Empty, Pagination, Table, Tag, Tooltip, Typography } from 'antd'
import dayjs from 'dayjs'
import { useState } from 'react'
import type { ColumnsType } from 'antd/es/table'
import { useReportSubscriptionRuns } from '../hooks/useReportSubscriptionRuns'
import { useRetryReportSubscriptionRun } from '../hooks/useRetryReportSubscriptionRun'
import { useRunReportSubscriptionNow } from '../hooks/useRunReportSubscriptionNow'
import type { ReportSubscription, ReportSubscriptionRun } from '../types'
import './ReportSubscriptionRun.css'

interface Props { onClose: () => void; open: boolean; subscription: ReportSubscription }

export default function ReportSubscriptionRunDrawer({ onClose, open, subscription }: Props) {
  const [current, setCurrent] = useState(1)
  const query = { current, size: 8 }
  const { data: runPage, isLoading: isLoadingRuns, isError: isRunsError,
    refetch: refetchRuns } = useReportSubscriptionRuns(subscription.uuid, query, open)
  const { mutate: runNow, isPending: isRunningNow } = useRunReportSubscriptionNow()
  const { mutate: retryRun, isPending: isRetryingRun } = useRetryReportSubscriptionRun()
  return <Drawer className="report-subscription-run-drawer" width="min(760px, calc(100vw - 24px))"
    title={`运行记录 · ${subscription.subscriptionName}`} open={open} onClose={onClose}
    extra={<Button type="primary" icon={<PlayCircleOutlined />} loading={isRunningNow}
      onClick={() => runNow(subscription.uuid)}>立即试跑</Button>}>
    <Alert className="report-subscription-run-hint" type="info" showIcon
      message="已派发表示导出任务已进入接收人的下载任务中心，文件生成结果请在任务中心查看。" />
    {isRunsError && <Alert className="report-subscription-run-error" type="error" showIcon
      message="运行记录加载失败" action={<Button size="small" icon={<ReloadOutlined />}
        onClick={() => void refetchRuns()}>重试</Button>} />}
    <Table<ReportSubscriptionRun> size="small" rowKey="uuid" loading={isLoadingRuns}
      columns={runColumns(subscription.uuid, retryRun, isRetryingRun)} dataSource={runPage?.records ?? []} pagination={false}
      locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无运行记录" /> }}
      scroll={{ x: 700 }} />
    {!!runPage?.total && <Pagination className="report-subscription-run-pagination"
      current={runPage.current} pageSize={runPage.size} total={runPage.total}
      showSizeChanger={false} onChange={setCurrent} />}
  </Drawer>
}

function runColumns(subscriptionUuid: string, retryRun: (input: {
  subscriptionUuid: string; runUuid: string
}) => void, retrying: boolean): ColumnsType<ReportSubscriptionRun> {
  return [
  { title: '计划时间', dataIndex: 'scheduledFor', width: 142,
    render: (value: string) => dayjs(value).format('YYYY-MM-DD HH:mm') },
  { title: '派发状态', dataIndex: 'runStatus', width: 104,
    render: (value) => <RunStatusTag status={value} /> },
  { title: '接收人', width: 128, render: (_, item) =>
    `${item.dispatchedCount}/${item.plannedCount} 已派发${item.failedCount ? `，${item.failedCount} 失败` : ''}` },
  { title: '指标版本', dataIndex: 'metricReleaseUuid', width: 124,
    render: (value: string) => <Tooltip title={value}><Typography.Text code>{value.slice(0, 8)}</Typography.Text></Tooltip> },
  { title: '完成时间', dataIndex: 'completedAt', width: 142,
    render: (value?: string) => value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-' },
  { title: '结果', dataIndex: 'errorMessage', width: 160, ellipsis: { showTitle: false },
    render: (value?: string) => value
      ? <Typography.Text type="danger" ellipsis={{ tooltip: value }}>{value}</Typography.Text>
      : <Typography.Text type="secondary">无异常</Typography.Text> },
  { title: '操作', fixed: 'right', width: 72, align: 'center', render: (_, item) =>
    item.runStatus === 3 || item.runStatus === 4
      ? <Tooltip title="按原时间槽幂等重试"><Button type="link" size="small" loading={retrying}
        onClick={() => retryRun({ subscriptionUuid, runUuid: item.uuid })}>重试</Button></Tooltip>
      : '-' },
  ]
}

function RunStatusTag({ status }: { status: ReportSubscriptionRun['runStatus'] }) {
  const config = {
    1: { color: 'processing', text: '调度中' },
    2: { color: 'success', text: '已派发' },
    3: { color: 'warning', text: '部分派发' },
    4: { color: 'error', text: '派发失败' },
  }[status]
  return <Tag color={config.color}>{config.text}</Tag>
}
