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
  createDeliveryOrder: ({ uuid, requestId, customerRevisionNo }: {
    uuid: string; requestId?: string; customerRevisionNo?: number
  }) => createDeliveryOrderExportTask(uuid, requestId, customerRevisionNo ?? 0),
  createProcessOrder: ({ uuid, requestId, customerRevisionNo }: {
    uuid: string; requestId?: string; customerRevisionNo?: number
  }) => createProcessOrderExportTask(uuid, requestId, customerRevisionNo),
  createSettle: ({ uuid, requestId }: { uuid: string; requestId: string }) => createSettleExportTask(uuid, requestId),
  download: ({ uuid, filename }: { uuid: string; filename?: string }) => downloadExportTask(uuid, filename),
}
