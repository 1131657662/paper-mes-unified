import { DeleteOutlined, EditOutlined, PlusOutlined, StarFilled } from '@ant-design/icons'
import { Button, Card, Empty, Popconfirm, Skeleton, Table, Tag, Tooltip } from 'antd'
import dayjs from 'dayjs'
import type { ColumnsType } from 'antd/es/table'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import QueryLoadErrorAlert from '../../components/feedback/QueryLoadErrorAlert'
import { useDeleteReportSavedView } from '../../features/reportSavedView/hooks/useDeleteReportSavedView'
import { useReportSavedViews } from '../../features/reportSavedView/hooks/useReportSavedViews'
import type { ReportSavedView } from '../../features/reportSavedView/types'
import ReportSavedViewModal from '../../features/reportSavedView/components/ReportSavedViewModal'
import type { ReportQuery } from '../../types/report'
import { serializeReportUrlState } from './reportUrlState'
import './ReportSavedViewsPage.css'

export default function ReportSavedViewsPage() {
  const navigate = useNavigate()
  const [editing, setEditing] = useState<ReportSavedView | null | undefined>(undefined)
  const query = useReportSavedViews()
  const deleteMutation = useDeleteReportSavedView()
  const baseQuery = defaultQuery()
  const apply = (item: ReportSavedView) => navigate(`${item.reportPath}?${viewParams(item)}`)
  return <main className="report-saved-views mes-workbench">
    <Card title="保存视图" extra={<Button type="primary" icon={<PlusOutlined />} onClick={() => setEditing(null)}>新建视图</Button>}>
      <p className="report-saved-views__intro">把常用的筛选、分组和指标组合保存下来，下一次打开即可继续使用。</p>
      {query.isError && <QueryLoadErrorAlert message="保存视图加载失败" description="请刷新后重试。" onRetry={() => void query.refetch()} />}
      {query.isLoading ? <Skeleton active paragraph={{ rows: 6 }} /> : query.data?.length ? <Table<ReportSavedView>
        rowKey="uuid" size="small" scroll={{ x: 920 }} pagination={false} dataSource={query.data}
        columns={columns({ apply, deleting: deleteMutation.isPending, edit: setEditing, remove: deleteMutation.mutate })} />
        : <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="还没有保存视图" />}
    </Card>
    {editing !== undefined && <ReportSavedViewModal open baseQuery={baseQuery} initial={editing}
      onClose={() => setEditing(undefined)} onSaved={() => setEditing(undefined)} />}
  </main>
}

interface ViewActions { apply: (item: ReportSavedView) => void; deleting: boolean; edit: (item: ReportSavedView) => void; remove: (input: { uuid: string; version: number }) => void }

function columns(actions: ViewActions): ColumnsType<ReportSavedView> {
  return [
    { title: '视图名称', dataIndex: 'viewName', width: 220, render: (value, item) => <div className="report-saved-view-name"><strong>{value}</strong>{item.isDefault === 1 && <Tag color="gold" icon={<StarFilled />}>默认</Tag>}</div> },
    { title: '打开页面', dataIndex: 'reportPath', width: 150 },
    { title: '分组', dataIndex: 'dimensionCode', width: 120, render: (value) => value || '经营总览' },
    { title: '指标数', dataIndex: 'metricCodes', width: 90, align: 'right', render: (value: string[]) => value.length },
    { title: '最后更新', dataIndex: 'updateTime', width: 160, render: (value) => dayjs(value).format('YYYY-MM-DD HH:mm') },
    { title: '操作', fixed: 'right', width: 170, align: 'center', render: (_, item) => <div className="report-saved-view-actions">
      <Button type="link" onClick={() => actions.apply(item)}>打开</Button><Tooltip title="编辑"><Button type="text" aria-label={`编辑视图 ${item.viewName}`} icon={<EditOutlined />} onClick={() => actions.edit(item)} /></Tooltip>
      <Popconfirm title="删除此保存视图？" onConfirm={() => actions.remove({ uuid: item.uuid, version: item.version })}><Button danger type="text" loading={actions.deleting} aria-label={`删除视图 ${item.viewName}`} icon={<DeleteOutlined />} /></Popconfirm>
    </div> },
  ]
}

function viewParams(item: ReportSavedView) {
  const params = serializeReportUrlState({ ...item.reportQuery, metricReleaseUuid: undefined } as ReportQuery)
  if (item.reportPath.endsWith('/explorer')) { params.set('dimension', item.dimensionCode ?? 'customer'); params.set('metrics', item.metricCodes.join(',')) }
  return params.toString()
}

function defaultQuery(): ReportQuery { return { dateFrom: dayjs().startOf('year').format('YYYY-MM-DD'), dateTo: dayjs().format('YYYY-MM-DD') } }
