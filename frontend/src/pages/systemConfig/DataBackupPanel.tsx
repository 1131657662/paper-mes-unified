import { Tabs } from 'antd'
import {
  useCleanupExpiredBackups, useCreateDataBackup, useDeleteDataBackup,
  useUpdateAutomaticBackup, useUpdateDataBackupEnabled,
  useUpdateDataBackupRetention, useVerifyDataBackup,
} from '../../features/dataBackup/hooks/useDataBackupMutations'
import {
  useDataBackups, useDataBackupStatus, useDataBackupTasks,
} from '../../features/dataBackup/hooks/useDataBackups'
import BackupOverview from './BackupOverview'
import BackupRecordTable from './BackupRecordTable'
import BackupTaskTable from './BackupTaskTable'

interface Props {
  activeView: 'records' | 'tasks'
  focusedTaskId?: string
  onViewChange: (view: 'records' | 'tasks') => void
}

export default function DataBackupPanel({ activeView, focusedTaskId, onViewChange }: Props) {
  const status = useDataBackupStatus()
  const backups = useDataBackups(Boolean(status.data?.running))
  const tasks = useDataBackupTasks()
  const createBackup = useCreateDataBackup()
  const verifyBackup = useVerifyDataBackup()
  const updateEnabled = useUpdateDataBackupEnabled()
  const updateRetention = useUpdateDataBackupRetention()
  const updateAutomatic = useUpdateAutomaticBackup()
  const cleanup = useCleanupExpiredBackups()
  const deleteBackup = useDeleteDataBackup()
  const mutating = [createBackup, verifyBackup, updateEnabled, updateRetention,
    updateAutomatic, cleanup, deleteBackup]
    .some((mutation) => mutation.isPending)
  const busy = Boolean(status.data?.running || mutating)
  const unavailable = status.isLoading || !status.data?.enabled || !status.data?.configured

  function refresh() {
    void Promise.all([backups.refetch(), status.refetch(), tasks.refetch()])
  }

  return (
    <div className="data-backup-panel">
      <BackupOverview
        status={status.data}
        busy={busy}
        loading={status.isLoading}
        unavailable={unavailable}
        actions={{
          backup: () => createBackup.mutate(),
          cleanup: () => cleanup.mutate(),
          refresh,
          saveAutomatic: (enabled, executionTime) => updateAutomatic.mutate({ enabled, executionTime }),
          saveRetention: (days) => updateRetention.mutate(days),
          toggle: (enabled) => updateEnabled.mutate(enabled),
        }}
      />
      <Tabs
        activeKey={activeView}
        className="data-backup-content-tabs"
        onChange={(key) => onViewChange(key === 'tasks' ? 'tasks' : 'records')}
        items={[
          {
            key: 'records', label: `备份记录 (${backups.data?.length ?? 0})`,
            children: <BackupRecordTable records={backups.data ?? []} busy={busy} loading={backups.isLoading} onDelete={(id) => deleteBackup.mutate(id)} onVerify={(id) => verifyBackup.mutate(id)} />,
          },
          {
            key: 'tasks', label: '任务历史',
            children: (
              <BackupTaskTable
                tasks={tasks.data ?? []}
                loading={tasks.isLoading}
                focusedTaskId={focusedTaskId}
                busy={busy}
                onRetry={(task) => {
                  if (task.taskType === 'VERIFY' && task.backupId) verifyBackup.mutate(task.backupId)
                  else createBackup.mutate()
                }}
              />
            ),
          },
        ]}
      />
    </div>
  )
}
