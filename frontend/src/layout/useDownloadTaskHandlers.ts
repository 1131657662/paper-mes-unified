import { message } from 'antd'
import {
  useAcknowledgeExportTask,
  useCancelExportTask,
  useDownloadExportTask,
  useRetryExportTask,
} from '../features/exportTask/hooks/useExportTaskMutations'
import type { ExportTaskAction } from '../features/exportTask/exportTaskDisplay'
import type { TaskHandlers } from './DownloadTaskList'

export function useDownloadTaskHandlers(onOpenSource: () => void): TaskHandlers {
  const download = useDownloadExportTask()
  const retry = useRetryExportTask()
  const cancel = useCancelExportTask()
  const acknowledge = useAcknowledgeExportTask()

  return {
    onDownload: (task) => download.mutate({ uuid: task.uuid, filename: task.fileName }, {
      onSuccess: (result) => message.success(`已下载 ${result.filename}`),
    }),
    onRetry: (task) => retry.mutate(task.uuid),
    onCancel: (task) => cancel.mutate(task.uuid),
    onAcknowledge: (task) => acknowledge.mutate(task.uuid),
    onOpenSource,
    busy: (taskUuid) => findBusyAction({ taskUuid, download, retry, cancel, acknowledge }),
  }
}

interface MutationStates {
  taskUuid: string
  download: ReturnType<typeof useDownloadExportTask>
  retry: ReturnType<typeof useRetryExportTask>
  cancel: ReturnType<typeof useCancelExportTask>
  acknowledge: ReturnType<typeof useAcknowledgeExportTask>
}

function findBusyAction(states: MutationStates): { uuid: string; action: ExportTaskAction } | undefined {
  const { taskUuid, download, retry, cancel, acknowledge } = states
  if (download.isPending && download.variables?.uuid === taskUuid) return busy(taskUuid, 'download')
  if (retry.isPending && retry.variables === taskUuid) return busy(taskUuid, 'retry')
  if (cancel.isPending && cancel.variables === taskUuid) return busy(taskUuid, 'cancel')
  if (acknowledge.isPending && acknowledge.variables === taskUuid) return busy(taskUuid, 'acknowledge')
  return undefined
}

function busy(uuid: string, action: ExportTaskAction) {
  return { uuid, action }
}
