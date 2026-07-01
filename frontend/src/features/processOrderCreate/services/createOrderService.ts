import { pageCustomers } from '../../../api/customer'
import { pageMachines } from '../../../api/machine'
import {
  createProcessOrderDraft,
  getProcessOrderDraft,
  listProcessOrderDrafts,
  previewOriginalRollImport,
  previewProcessPlan,
  replaceDraftOriginalRolls,
  saveDraftBaseInfo,
  saveDraftProgress,
  saveProcessConfigDraft,
  saveProcessPlan,
  saveProcessPlanBatch,
  submitProcessOrderDraft,
  updateOriginalRoll,
} from '../../../api/processOrder'
import { pageWarehouses } from '../../../api/warehouse'
import type {
  DraftOrderBaseDTO,
  FinishConfigSaveDTO,
  OriginalRollDTO,
  ProcessPlanBatchSaveDTO,
  ProcessPlanPreviewRequestDTO,
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
  saveProgress: (params: { uuid: string; currentStep: number }) =>
    saveDraftProgress(params.uuid, { currentStep: params.currentStep }),
  replaceRolls: (params: { uuid: string; rolls: OriginalRollDTO[] }) =>
    replaceDraftOriginalRolls(params.uuid, { rolls: params.rolls }),
  importRolls: (params: { uuid: string; file: File }) => previewOriginalRollImport(params.uuid, params.file),
  updateRoll: (params: { rollUuid: string; dto: OriginalRollDTO }) =>
    updateOriginalRoll(params.rollUuid, params.dto),
  saveConfig: (params: { orderUuid: string; rollUuid: string; config: FinishConfigSaveDTO }) =>
    saveProcessConfigDraft(params.orderUuid, params.rollUuid, { config: params.config }),
  previewPlan: (params: { orderUuid: string; request: ProcessPlanPreviewRequestDTO }) =>
    previewProcessPlan(params.orderUuid, params.request),
  savePlan: (params: { orderUuid: string; rollUuid: string; request: ProcessPlanPreviewRequestDTO }) =>
    saveProcessPlan(params.orderUuid, params.rollUuid, params.request),
  savePlanBatch: (params: { orderUuid: string; dto: ProcessPlanBatchSaveDTO }) =>
    saveProcessPlanBatch(params.orderUuid, params.dto),
  submit: (uuid: string) => submitProcessOrderDraft(uuid),
}
