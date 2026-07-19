import { Tabs } from 'antd'
import { useState } from 'react'
import type { TaskHandlers } from './DownloadTaskList'
import DownloadTaskHistory from './DownloadTaskHistory'
import DownloadTaskOperations from './DownloadTaskOperations'

interface Props {
  canViewOperations: boolean
  canViewTasks: boolean
  handlers: TaskHandlers
  open: boolean
  unacknowledgedCount: number
}

export default function DownloadTaskDrawerContent(props: Props) {
  const [activeKey, setActiveKey] = useState(props.canViewTasks ? 'tasks' : 'operations')
  const tasks = <DownloadTaskHistory enabled={props.open && activeKey === 'tasks'} handlers={props.handlers}
    unacknowledgedCount={props.unacknowledgedCount} />
  const operations = (
    <DownloadTaskOperations enabled={props.open && (!props.canViewTasks || activeKey === 'operations')} />
  )

  if (!props.canViewTasks) return operations
  if (!props.canViewOperations) return tasks
  return <Tabs activeKey={activeKey} onChange={setActiveKey} items={[
    { key: 'tasks', label: '我的任务', children: tasks },
    { key: 'operations', label: '运行状态', children: operations },
  ]} />
}
