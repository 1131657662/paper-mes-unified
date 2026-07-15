import { EyeOutlined, ReloadOutlined } from '@ant-design/icons'
import { Alert, Button, Descriptions, Modal, Popconfirm, Table, Tag } from 'antd'
import { formatDateTime } from '../../utils/dateTime'
import type { ColumnsType } from 'antd/es/table'
import { useState } from 'react'
import type { BackupTask } from '../../types/dataBackup'

interface BackupTaskTableProps {
  tasks: BackupTask[]
  loading: boolean
  focusedTaskId?: string
  busy: boolean
  onRetry: (task: BackupTask) => void
}

export default function BackupTaskTable({ tasks, loading, focusedTaskId, busy, onRetry }: BackupTaskTableProps) {
  const [detailTask, setDetailTask] = useState<BackupTask>()
  const rows = prioritizedTasks(tasks, focusedTaskId)
  return (
    <>
      <Table<BackupTask>
        rowKey="uuid"
        size="small"
        loading={loading}
        columns={createColumns({ busy, onDetail: setDetailTask, onRetry })}
        dataSource={rows}
        pagination={{ pageSize: 10, showSizeChanger: false }}
        rowClassName={(task) => task.uuid === focusedTaskId ? 'backup-task-row--focused' : ''}
        scroll={{ x: 900 }}
      />
      <BackupTaskDetail task={detailTask} onClose={() => setDetailTask(undefined)} />
    </>
  )
}

function prioritizedTasks(tasks: BackupTask[], focusedTaskId?: string): BackupTask[] {
  if (!focusedTaskId) return tasks
  const focused = tasks.find((task) => task.uuid === focusedTaskId)
  if (!focused) return tasks
  return [focused, ...tasks.filter((task) => task.uuid !== focusedTaskId)]
}

interface TaskActionProps {
  busy: boolean
  onDetail: (task: BackupTask) => void
  onRetry: (task: BackupTask) => void
}

function createColumns(actions: TaskActionProps): ColumnsType<BackupTask> {
  return [
    { title: '任务类型', dataIndex: 'taskType', width: 110, render: renderTaskType },
    { title: '备份编号', dataIndex: 'backupId', width: 160, render: (value) => value || '-' },
    { title: '状态', dataIndex: 'taskStatus', width: 90, render: renderStatus },
    { title: '开始时间', dataIndex: 'startedAt', width: 165, render: formatDate },
    { title: '耗时', dataIndex: 'durationMs', width: 90, render: formatDuration },
    { title: '操作者', dataIndex: 'operator', width: 110 },
    { title: '结果', dataIndex: 'message', width: 260, ellipsis: true },
    {
      title: '操作', key: 'actions', width: 160,
      render: (_, task) => <TaskActions task={task} {...actions} />,
    },
  ]
}

function TaskActions({ busy, onDetail, onRetry, task }: TaskActionProps & { task: BackupTask }) {
  const retryDisabled = busy || task.taskStatus !== 'FAILED' || (task.taskType === 'VERIFY' && !task.backupId)
  return (
    <div className="mes-table-actions">
      <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => onDetail(task)}>
        详情
      </Button>
      {task.taskStatus === 'FAILED' && (
        <Popconfirm title="确认重新执行该任务？" description={retryDescription(task)} onConfirm={() => onRetry(task)}>
          <Button type="link" size="small" icon={<ReloadOutlined />} disabled={retryDisabled}>
            重试
          </Button>
        </Popconfirm>
      )}
    </div>
  )
}

function retryDescription(task: BackupTask) {
  return task.taskType === 'VERIFY' ? '将对原备份重新进行隔离恢复演练，不会恢复生产库。' : '将重新创建一份手动备份。'
}

function BackupTaskDetail({ task, onClose }: { task?: BackupTask; onClose: () => void }) {
  return (
    <Modal title="备份任务详情" open={!!task} footer={null} onCancel={onClose} destroyOnHidden>
      {task && (
        <Descriptions bordered size="small" column={1}>
          <Descriptions.Item label="任务类型">{renderTaskType(task.taskType)}</Descriptions.Item>
          <Descriptions.Item label="任务编号">{task.uuid}</Descriptions.Item>
          <Descriptions.Item label="备份编号">{task.backupId || '-'}</Descriptions.Item>
          <Descriptions.Item label="状态">{renderStatus(task.taskStatus)}</Descriptions.Item>
          <Descriptions.Item label="开始时间">{formatDate(task.startedAt)}</Descriptions.Item>
          <Descriptions.Item label="结束时间">{formatDate(task.finishedAt)}</Descriptions.Item>
          <Descriptions.Item label="操作者">{task.operator || '-'}</Descriptions.Item>
          <Descriptions.Item label="结果">
            {task.taskStatus === 'FAILED'
              ? <Alert type="error" showIcon message="安全提示" description={task.message || '任务失败，请查看服务器日志'} />
              : task.message || '-'}
          </Descriptions.Item>
        </Descriptions>
      )}
    </Modal>
  )
}

function renderTaskType(value: BackupTask['taskType']) {
  if (value === 'BACKUP') return '手动备份'
  if (value === 'AUTO_BACKUP') return '自动备份'
  return '恢复演练'
}

function renderStatus(value: BackupTask['taskStatus']) {
  if (value === 'SUCCESS') return <Tag color="success">成功</Tag>
  if (value === 'FAILED') return <Tag color="error">失败</Tag>
  return <Tag color="processing">执行中</Tag>
}

function formatDate(value?: string) {
  return formatDateTime(value)
}

function formatDuration(value?: number) {
  if (value == null) return '-'
  return value < 1000 ? `${value} ms` : `${(value / 1000).toFixed(1)} s`
}
