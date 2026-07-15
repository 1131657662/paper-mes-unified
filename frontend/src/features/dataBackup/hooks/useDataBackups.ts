import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'

export function useDataBackups(taskRunning: boolean) {
  return useQuery({
    ...queries.dataBackup.list,
    refetchInterval: taskRunning ? 1000 : false,
  })
}

export function useDataBackupStatus() {
  return useQuery({
    ...queries.dataBackup.status,
    refetchInterval: (query) => query.state.data?.running ? 3000 : false,
  })
}

export function useDataBackupTasks() {
  return useQuery({
    ...queries.dataBackup.tasks,
    refetchInterval: (query) => query.state.data?.some((task) => task.taskStatus === 'RUNNING') ? 3000 : false,
  })
}
