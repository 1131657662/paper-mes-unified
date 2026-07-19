import {
  acknowledgeExportTask,
  acknowledgeExportTasks,
  cancelExportTask,
  createDeliveryOrderExportTask,
  createProcessOrderExportTask,
  createSettleExportTask,
  downloadExportTask,
  getExportTaskHistory,
  getExportTaskOperations,
  getExportTaskOperationsIssues,
  getExportTaskSummary,
  retryExportTask,
} from '../../../api/exportTask'

export const exportTaskService = {
  summary: getExportTaskSummary,
  operations: getExportTaskOperations,
  operationsIssues: getExportTaskOperationsIssues,
  history: getExportTaskHistory,
  acknowledge: acknowledgeExportTasks,
  acknowledgeOne: acknowledgeExportTask,
  retry: retryExportTask,
  cancel: cancelExportTask,
  createDeliveryOrder: ({ uuid, requestId }: { uuid: string; requestId?: string }) =>
    createDeliveryOrderExportTask(uuid, requestId),
  createProcessOrder: ({ uuid, requestId }: { uuid: string; requestId?: string }) =>
    createProcessOrderExportTask(uuid, requestId),
  createSettle: ({ uuid, requestId }: { uuid: string; requestId: string }) => createSettleExportTask(uuid, requestId),
  download: ({ uuid, filename }: { uuid: string; filename?: string }) => downloadExportTask(uuid, filename),
}
