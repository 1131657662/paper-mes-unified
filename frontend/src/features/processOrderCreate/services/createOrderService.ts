import { pageCustomers } from '../../../api/customer'
import { pageMachines } from '../../../api/machine'
import {
  createProcessOrderDraft,
  getProcessOrderDraft,
  listProcessOrderDrafts,
  previewOriginalRollImport,
  previewProcessPlan,
  previewProcessRoute,
  replaceDraftOriginalRolls,
  saveDraftBaseInfo,
  saveDraftProgress,
  saveDraftProcessRoute,
  saveDraftProcessRouteBatch,
  saveDraftRollProcesses,
  saveProcessConfigDraft,
  saveProcessPlan,
  saveProcessPlanBatch,
  saveProcessPlanItemsBatch,
  submitProcessOrderDraft,
} from '../../../api/processOrder'
import { pageWarehouses } from '../../../api/warehouse'
import type {
  DraftOrderBaseDTO,
  DraftRollProcessBatchSaveDTO,
  FinishConfigSaveDTO,
  OriginalRollDTO,
  ProcessPlanBatchSaveDTO,
  ProcessPlanItemsBatchSaveDTO,
  ProcessPlanPreviewRequestDTO,
  ProcessRoutePreviewDTO,
  ProcessRouteBatchSaveDTO,
} from '../../../types/processOrder'

export const createOrderService = {
  customers: () => pageCustomers({ current: 1, size: 200 }),
  warehouses: () => pageWarehouses({ current: 1, size: 200 }),
  machines: () => pageMachines({ current: 1, size: 500, status: 1 }),
  drafts: () => listProcessOrderDrafts(),
  draft: (uuid: string) => getProcessOrderDraft(uuid),
  createDraft: (dto: DraftOrderBaseDTO) => createProcessOrderDraft(dto),
  saveBaseInfo: (params: { uuid: string; dto: DraftOrderBaseDTO }) =>
    saveDraftBaseInfo(params.uuid, params.dto),
  saveProgress: (params: { uuid: string; currentStep: number; expectedVersion: number }) =>
    saveDraftProgress(params.uuid, {
      currentStep: params.currentStep, expectedVersion: params.expectedVersion,
    }),
  replaceRolls: (params: { uuid: string; rolls: OriginalRollDTO[]; expectedVersion: number }) =>
    replaceDraftOriginalRolls(params.uuid, {
      rolls: params.rolls, expectedVersion: params.expectedVersion,
    }),
  importRolls: (params: { uuid: string; file: File }) => previewOriginalRollImport(params.uuid, params.file),
  saveRollProcesses: (params: { orderUuid: string; dto: DraftRollProcessBatchSaveDTO }) =>
    saveDraftRollProcesses(params.orderUuid, params.dto),
  saveConfig: (params: {
    orderUuid: string; rollUuid: string; config: FinishConfigSaveDTO; expectedVersion: number
  }) => saveProcessConfigDraft(params.orderUuid, params.rollUuid, {
    config: params.config, expectedVersion: params.expectedVersion,
  }),
  previewPlan: (params: { orderUuid: string; request: ProcessPlanPreviewRequestDTO }) =>
    previewProcessPlan(params.orderUuid, params.request),
  previewRoute: (params: { orderUuid: string; request: ProcessRoutePreviewDTO }) =>
    previewProcessRoute(params.orderUuid, params.request),
  saveRoute: (params: { orderUuid: string; request: ProcessRoutePreviewDTO }) =>
    saveDraftProcessRoute(params.orderUuid, params.request),
  saveRouteBatch: (params: { orderUuid: string; dto: ProcessRouteBatchSaveDTO }) =>
    saveDraftProcessRouteBatch(params.orderUuid, params.dto),
  savePlan: (params: { orderUuid: string; rollUuid: string; request: ProcessPlanPreviewRequestDTO }) =>
    saveProcessPlan(params.orderUuid, params.rollUuid, params.request),
  savePlanBatch: (params: { orderUuid: string; dto: ProcessPlanBatchSaveDTO }) =>
    saveProcessPlanBatch(params.orderUuid, params.dto),
  savePlanItemsBatch: (params: { orderUuid: string; dto: ProcessPlanItemsBatchSaveDTO }) =>
    saveProcessPlanItemsBatch(params.orderUuid, params.dto),
  submit: (params: { uuid: string; expectedVersion: number }) =>
    submitProcessOrderDraft(params.uuid, { expectedVersion: params.expectedVersion }),
}
