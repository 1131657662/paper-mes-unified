import type { ProcessOrderSettlementMode } from '../settlementSemantics'
import type { FinishConfigSaveDTO, OriginalRollDTO } from './finishConfig'
import type { OrderSettlementMode, ProcessOrder } from './order'
import type { ProcessConfigDraftVO } from './planning'
import type { OriginalRoll } from './roll'

export interface DraftOrderVO {
  order?: ProcessOrder
  currentStep?: number
  rolls?: OriginalRoll[]
  configs?: ProcessConfigDraftVO[]
}

export interface DraftSummaryVO {
  orderUuid?: string
  orderNo?: string
  customerName?: string
  orderDate?: string
  currentStep?: number
  rollCount?: number
  configuredCount?: number
  totalWeight?: number
}

/** 创建加工单入参，与后端 ProcessOrderCreateDTO 对应。 */
export interface ProcessOrderCreateDTO {
  customerUuid: string
  orderDate: string
  expectFinishDate?: string
  priority?: number
  labelBrand?: string
  warehouseUuid?: string
  isInvoice?: number
  settleType?: ProcessOrderSettlementMode
  settleDay?: number
  settleMode?: OrderSettlementMode
  customerVersion?: number
  settleOverrideReason?: string
  taxRate?: number
  urgentFee?: number
  palletFee?: number
  loadingFee?: number
  freightFee?: number
  otherFee?: number
  remark?: string
  remarkLong?: string
  originalRolls: OriginalRollDTO[]
}

export interface DraftOrderBaseDTO {
  expectedVersion?: number
  customerUuid: string
  orderDate: string
  expectFinishDate?: string
  priority?: number
  labelBrand?: string
  warehouseUuid?: string
  isInvoice?: number
  settleType?: ProcessOrderSettlementMode
  settleDay?: number
  settleMode?: OrderSettlementMode
  customerVersion?: number
  settleOverrideReason?: string
  taxRate?: number
  urgentFee?: number
  palletFee?: number
  loadingFee?: number
  freightFee?: number
  otherFee?: number
  remark?: string
  remarkLong?: string
}

export interface OriginalRollBatchSaveDTO {
  expectedVersion: number
  rolls: OriginalRollDTO[]
}

export interface DraftRollProcessDTO {
  originalUuid: string
  processMode: number
  mainStepType?: number
  machineUuid?: string
}

export interface DraftRollProcessBatchSaveDTO {
  expectedVersion: number
  rolls: DraftRollProcessDTO[]
}

export interface ProcessConfigDraftSaveDTO {
  expectedVersion: number
  config: FinishConfigSaveDTO
}

export interface DraftProgressDTO {
  expectedVersion: number
  currentStep?: number
}

export interface DraftSubmitDTO {
  expectedVersion: number
}

export interface OriginalRollImportError {
  rowNumber: number
  field?: string
  message?: string
  raw?: Record<string, string>
}

export interface OriginalRollImportPreviewVO {
  validRows?: OriginalRollDTO[]
  errors?: OriginalRollImportError[]
}

export interface ProcessOrderSubmitVO {
  orderUuid?: string
  orderNo?: string
  orderStatus?: number
  finishRollNos?: string[]
  remainRollNos?: string[]
  spareRollNos?: string[]
}
