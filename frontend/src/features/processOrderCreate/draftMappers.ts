import type {
  DraftOrderBaseDTO,
  FinishConfigSaveDTO,
  OriginalRoll,
  OriginalRollDTO,
  ProcessPlanDTO,
} from '../../types/processOrder'
import type { RollDraft } from './types'

export function newRollDraft(defaults: Partial<RollDraft> = {}): RollDraft {
  return {
    paperName: '',
    gramWeight: defaults.gramWeight ?? 80,
    originalWidth: defaults.originalWidth ?? 1000,
    rollWeight: defaults.rollWeight ?? 0,
    pieceNum: 1,
    processMode: 1,
    mainStepType: 2,
    ...defaults,
    uuid: undefined,
    localId: crypto.randomUUID(),
  }
}

export function toRollDto(roll: RollDraft): OriginalRollDTO {
  return {
    extraNo: roll.extraNo,
    rollNo: roll.rollNo,
    paperName: roll.paperName,
    gramWeight: roll.gramWeight,
    originalWidth: roll.originalWidth,
    originalDiameter: roll.originalDiameter,
    coreDiameter: roll.coreDiameter,
    originalLength: roll.originalLength,
    rollWeight: roll.rollWeight,
    pieceNum: roll.pieceNum,
    batchNo: roll.batchNo,
    damageDesc: roll.damageDesc,
    processMode: roll.processMode,
    mainStepType: roll.processMode === 3 ? undefined : roll.mainStepType,
    machineUuid: roll.processMode === 3 ? undefined : roll.machineUuid,
    remark: roll.remark,
  }
}

export function attachSavedUuids(rolls: RollDraft[], uuids: string[]): RollDraft[] {
  return rolls.map((roll, index) => ({ ...roll, uuid: uuids[index] }))
}

export function toOriginalRoll(roll: RollDraft): OriginalRoll {
  return {
    uuid: roll.uuid ?? roll.localId,
    paperName: roll.paperName,
    gramWeight: roll.gramWeight,
    originalWidth: roll.originalWidth,
    originalDiameter: roll.originalDiameter,
    coreDiameter: roll.coreDiameter,
    rollWeight: roll.rollWeight,
    pieceNum: roll.pieceNum,
    rollNo: roll.rollNo,
    extraNo: roll.extraNo,
    batchNo: roll.batchNo,
    damageDesc: roll.damageDesc,
    processMode: roll.processMode,
    mainStepType: roll.mainStepType,
    machineUuid: roll.machineUuid,
    remark: roll.remark,
  }
}

export function rollDraftFromDto(dto: OriginalRollDTO): RollDraft {
  return newRollDraft({
    ...dto,
    processMode: dto.processMode ?? 1,
    mainStepType: dto.processMode === 3 ? undefined : dto.mainStepType ?? 2,
  })
}

export function rollDraftFromOriginal(roll: OriginalRoll): RollDraft {
  return {
    extraNo: roll.extraNo,
    rollNo: roll.rollNo,
    paperName: roll.paperName ?? '',
    gramWeight: roll.gramWeight ?? 80,
    originalWidth: roll.originalWidth ?? 1000,
    originalDiameter: roll.originalDiameter,
    coreDiameter: roll.coreDiameter,
    originalLength: roll.originalLength,
    rollWeight: roll.rollWeight ?? 0,
    pieceNum: roll.pieceNum ?? 1,
    batchNo: roll.batchNo,
    damageDesc: roll.damageDesc,
    processMode: roll.processMode ?? 1,
    mainStepType: roll.processMode === 3 ? undefined : roll.mainStepType ?? 2,
    machineUuid: roll.machineUuid,
    remark: roll.remark,
    uuid: roll.uuid,
    localId: roll.uuid,
  }
}

export function baseInfoFromOrder(order: { [key: string]: unknown }): DraftOrderBaseDTO {
  return {
    customerUuid: String(order.customerUuid ?? ''),
    orderDate: String(order.orderDate ?? ''),
    expectFinishDate: order.expectFinishDate as string | undefined,
    priority: order.priority as number | undefined,
    labelBrand: order.labelBrand as string | undefined,
    warehouseUuid: order.warehouseUuid as string | undefined,
    teamGroup: order.teamGroup as string | undefined,
    isInvoice: order.isInvoice as number | undefined,
    settleType: order.settleType as number | undefined,
    settleDay: order.settleDay as number | undefined,
    taxRate: order.taxRate as number | undefined,
    urgentFee: order.urgentFee as number | undefined,
    palletFee: order.palletFee as number | undefined,
    loadingFee: order.loadingFee as number | undefined,
    freightFee: order.freightFee as number | undefined,
    otherFee: order.otherFee as number | undefined,
    remark: order.remark as string | undefined,
    remarkLong: order.remarkLong as string | undefined,
  }
}

export function totalWeight(rolls: RollDraft[]) {
  return rolls.reduce((sum, roll) => sum + Number(roll.rollWeight ?? 0) * (roll.pieceNum ?? 1), 0)
}

export function normalizeBaseInfo(values: DraftOrderBaseDTO): DraftOrderBaseDTO {
  return {
    ...values,
    priority: values.priority ?? 1,
    isInvoice: values.isInvoice ?? 2,
    settleType: values.settleType ?? 2,
  }
}

export function defaultPlanForRoll(roll: RollDraft, options: DefaultPlanOptions = {}): ProcessPlanDTO {
  const spareCount = Math.max(0, options.spareCount ?? 0)
  if (roll.processMode === 3) {
    return { processMode: 3, spareCount: 0, finishSpecs: [] }
  }
  if (roll.processMode === 2) {
    const mainStepType = roll.mainStepType ?? 2
    return {
      processMode: 2,
      mainStepType,
      machineUuid: roll.machineUuid,
      rewindMode: mainStepType === 2 ? 2 : undefined,
      knifeCount: mainStepType === 1 ? 0 : undefined,
      unitPrice: defaultUnitPrice(mainStepType, options),
      spareCount,
      finishSpecs: [{ itemType: 'FINISH', count: 1, finishWidth: 0, estimateWeight: 0 }],
      segments: [],
    }
  }
  if (roll.mainStepType === 1) {
    return {
      processMode: roll.processMode ?? 1,
      mainStepType: 1,
      machineUuid: roll.machineUuid,
      knifeCount: 0,
      unitPrice: defaultUnitPrice(1, options),
      spareCount,
      finishSpecs: [{ itemType: 'FINISH', count: 1, finishWidth: Math.max(1, roll.originalWidth - 100), estimateWeight: 0 }],
    }
  }
  return {
    processMode: roll.processMode ?? 1,
    mainStepType: 2,
    machineUuid: roll.machineUuid,
    rewindMode: 2,
    unitPrice: defaultUnitPrice(2, options),
    spareCount,
    finishSpecs: [],
    segments: [
      {
        segmentSort: 1,
        segmentRatio: 1,
        targetDiameter: roll.originalDiameter,
        finishCoreDiameter: roll.coreDiameter ?? 3,
        repeatCount: 1,
        sources: roll.uuid ? [{ originalUuid: roll.uuid, shareRatio: 100, consumeRatio: 100, sourceSort: 1 }] : [],
        layoutItems: [{ width: roll.originalWidth, quantity: 1, itemType: 'FINISH' }],
      },
    ],
  }
}

export function defaultConfigForRoll(roll: RollDraft, options: DefaultPlanOptions = {}): FinishConfigSaveDTO {
  const plan = defaultPlanForRoll(roll, options)
  if (roll.processMode === 3) {
    return { processMode: 3, spareCount: 0, finishSpecs: [] }
  }
  if (roll.mainStepType === 1) {
    return {
      ...plan,
      rewindSegments: undefined,
    }
  }
  return {
    processMode: plan.processMode,
    mainStepType: plan.mainStepType,
    rewindMode: plan.rewindMode,
    unitPrice: plan.unitPrice,
    spareCount: plan.spareCount,
    finishSpecs: [],
    rewindSegments: plan.segments?.map((segment) => ({
      ...segment,
        sources: segment.sources?.map((source) => ({
          originalUuid: source.originalUuid,
          shareRatio: source.shareRatio,
          consumeRatio: source.consumeRatio,
        })),
    })),
  }
}

export interface DefaultPlanOptions {
  spareCount?: number
  sawPrice?: number
  rewindPrice?: number
}

const LEGACY_SAW_UNIT_PRICE = 1.5
const LEGACY_REWIND_UNIT_PRICE = 200

export function applyLegacyPlanPriceDefaults(
  plan: ProcessPlanDTO,
  options: DefaultPlanOptions = {},
): ProcessPlanDTO {
  const unitPrice = defaultUnitPrice(plan.mainStepType, options)
  if (unitPrice == null || !isLegacyDefaultUnitPrice(plan.mainStepType, plan.unitPrice)) {
    return plan
  }
  return { ...plan, unitPrice }
}

function defaultUnitPrice(mainStepType: number | undefined, options: DefaultPlanOptions) {
  if (mainStepType === 1) return options.sawPrice
  if (mainStepType === 2) return options.rewindPrice
  return undefined
}

function isLegacyDefaultUnitPrice(mainStepType: number | undefined, unitPrice: number | undefined) {
  if (unitPrice == null) return true
  if (mainStepType === 1) return unitPrice === LEGACY_SAW_UNIT_PRICE
  if (mainStepType === 2) return unitPrice === LEGACY_REWIND_UNIT_PRICE
  return false
}
