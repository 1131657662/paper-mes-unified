import type {
  DraftOrderBaseDTO,
  FinishConfigSaveDTO,
  PlanPreviewVO,
  ProcessPlanDTO,
  OriginalRoll,
  OriginalRollDTO,
} from '../../types/processOrder'

export interface RollDraft extends OriginalRollDTO {
  localId: string
  uuid?: string
}

export interface CreateOrderState {
  orderUuid?: string
  baseInfo?: DraftOrderBaseDTO
  rolls: RollDraft[]
  configs: Record<string, FinishConfigSaveDTO>
  plans: Record<string, ProcessPlanDTO>
  previews: Record<string, PlanPreviewVO>
  selectedRollId?: string
}

export interface ReferenceOption {
  label: string
  value: string
  defaultInvoice?: number
  settleDay?: number
  settleType?: number
  taxRate?: number
}

export function rollKey(roll: RollDraft | OriginalRoll) {
  return roll.uuid ?? ('localId' in roll ? roll.localId : '')
}
