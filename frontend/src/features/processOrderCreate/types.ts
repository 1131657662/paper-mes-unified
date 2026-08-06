import type {
  DraftOrderBaseDTO,
  FinishConfigSaveDTO,
  PlanPreviewVO,
  ProcessPlanDTO,
  OriginalRoll,
  OriginalRollDTO,
} from '../../types/processOrder'
import type { ProcessOrderSettlementMode } from '../../types/settlementSemantics'

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
  version?: number
  defaultInvoice?: number
  settleDay?: number
  settleType?: ProcessOrderSettlementMode
  priceIncludeTax?: number
  rewindPrice?: number
  sawPrice?: number
  taxRate?: number
}

export function rollKey(roll: RollDraft | OriginalRoll) {
  return roll.uuid ?? ('localId' in roll ? roll.localId : '')
}
