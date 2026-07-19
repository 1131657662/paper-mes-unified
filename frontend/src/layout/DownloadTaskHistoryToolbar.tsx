import { CheckOutlined } from '@ant-design/icons'
import { Button, Checkbox, Input, Popconfirm, Select, message } from 'antd'
import { useState } from 'react'
import { exportTaskModuleOptions, exportTaskStatusOptions } from '../features/exportTask/exportTaskHistoryFilters'
import { useAcknowledgeExportTasks } from '../features/exportTask/hooks/useExportTaskMutations'
import type { ExportTaskHistoryQuery } from '../types/exportTask'

interface Props {
  onAttentionChange: (checked: boolean) => void
  onKeywordChange: (value?: string) => void
  onModuleChange: (value?: string) => void
  onStatusChange: (value?: number) => void
  query: ExportTaskHistoryQuery
  snapshot?: AcknowledgeSnapshot
}

interface AcknowledgeSnapshot {
  asOf: string
  current: boolean
  unacknowledgedCount: number
}

export default function DownloadTaskHistoryToolbar(props: Props) {
  const [keyword, setKeyword] = useState(props.query.keyword ?? '')

  return <div className="download-task-history__filters">
    <Input.Search allowClear className="download-task-history__search" value={keyword} maxLength={80}
      placeholder="搜索任务名或文件名" onSearch={(value) => {
        const normalized = value.trim()
        setKeyword(normalized)
        props.onKeywordChange(normalized || undefined)
      }}
      onChange={(event) => {
        setKeyword(event.target.value)
        if (!event.target.value) props.onKeywordChange(undefined)
      }} />
    <Select allowClear placeholder="全部状态" value={props.query.taskStatus} options={exportTaskStatusOptions}
      onChange={props.onStatusChange} />
    <Select allowClear placeholder="全部模块" value={props.query.moduleCode} options={exportTaskModuleOptions}
      onChange={props.onModuleChange} />
    <div className="download-task-history__attention">
      <Checkbox checked={props.query.attentionOnly} onChange={(event) => props.onAttentionChange(event.target.checked)}>
        仅看待处理
      </Checkbox>
      <AcknowledgeFilteredTasks query={props.query} snapshot={props.snapshot} />
    </div>
  </div>
}

function AcknowledgeFilteredTasks({ query, snapshot }: {
  query: ExportTaskHistoryQuery
  snapshot?: AcknowledgeSnapshot
}) {
  const { mutate: acknowledgeTasks, isPending: isAcknowledging } = useAcknowledgeExportTasks()
  const terminalStatus = query.taskStatus === undefined || [3, 4, 6].includes(query.taskStatus)
  if (!snapshot || snapshot.unacknowledgedCount === 0 || !terminalStatus) return null
  const filtered = Boolean(query.taskStatus !== undefined || query.moduleCode || query.keyword)
  const label = filtered ? '标记筛选结果' : '全部标记已处理'
  const scope = filtered ? '当前筛选范围' : '全部提醒'
  const acknowledge = () => {
    if (!snapshot.current) return
    acknowledgeTasks({
      asOf: snapshot.asOf, taskStatus: query.taskStatus, moduleCode: query.moduleCode, keyword: query.keyword,
    }, {
      onSuccess: (count) => count > 0
        ? message.success(`已将 ${count} 条提醒标记为已处理`)
        : message.info('当前筛选范围没有待处理提醒'),
    })
  }
  return <Popconfirm title={`将${scope}标记为已处理？`}
    description="仅影响当前账号，不会删除文件，也不影响下载和重试。"
    disabled={!snapshot.current} onConfirm={acknowledge}>
    <Button type="link" size="small" icon={<CheckOutlined />} loading={isAcknowledging}
      disabled={!snapshot.current}>{label}</Button>
  </Popconfirm>
}
