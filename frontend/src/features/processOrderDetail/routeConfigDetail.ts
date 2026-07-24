import type {
  OriginalRoll,
  ProcessOrderDetailVO,
  ProcessPlanDTO,
  ProcessRouteOutputDTO,
  ProcessRoutePreviewDTO,
  ProcessRouteStageDTO,
  RollProductionVO,
} from '../../types/processOrder'
import type { Customer } from '../../types/customer'
import type { RollDraft } from '../processOrderCreate/types'
import {
  stageOutputRows,
} from './routeConfigFormOperations'
import { buildFirstRouteOutputs, resolveFirstRouteStepType } from './routeConfigHydration'
import {
  STEP_TYPE_REWIND,
  STEP_TYPE_SAW,
  routeStepName,
  type DetailRouteFormState,
  type DetailRouteOutputRow,
  type DetailRoutePriceDefaults,
  type DetailRouteStageForm,
} from './routeConfigModel'
import { calculateRouteOutputs } from './routeConfigOutputCalculator'

export {
  addDetailRouteStage,
  allDetailRouteOutputs,
  changeDetailRouteStageInputs,
  changeDetailRouteStageType,
  finalDetailRouteOutputs,
  inputRowsForStage,
  removeLastDetailRouteStage,
  selectedInputRowsForStage,
  stageOutputRows,
  updateDetailRouteStagePlan,
} from './routeConfigFormOperations'
export {
  STEP_TYPE_REWIND,
  STEP_TYPE_SAW,
  routeStepName,
} from './routeConfigModel'
export type {
  DetailRouteFormState,
  DetailRouteOutputRow,
  DetailRoutePriceDefaults,
  DetailRouteStageForm,
} from './routeConfigModel'

export function initialDetailRouteForm(
  roll: OriginalRoll,
  production?: RollProductionVO,
  prices: DetailRoutePriceDefaults = {},
  baseStageLevel = 1,
  useCurrentOutputs = false,
  preferredSourceOutputKey?: string,
): DetailRouteFormState {
  const firstStepType = resolveFirstRouteStepType(roll, production)
  return {
    baseStageLevel,
    firstOutputs: buildFirstRouteOutputs(roll, production, baseStageLevel, useCurrentOutputs),
    firstSawKnifeCount: production?.steps
      ?.find((step) => step.stepType === STEP_TYPE_SAW)?.knifeCount ?? 1,
    firstStepType,
    firstUnitPrice: priceForStep(firstStepType, prices),
    preferredSourceOutputKey,
    stages: [],
  }
}

export function initialDetailRouteFormForOrder(
  detail: ProcessOrderDetailVO | undefined,
  roll: OriginalRoll,
  prices: DetailRoutePriceDefaults,
  appendMode: boolean,
  initialOutputKey?: string,
): DetailRouteFormState {
  const production = detail?.rollProductions?.find((item) => item.originalUuid === roll.uuid)
  return initialDetailRouteForm(
    roll,
    production,
    prices,
    appendMode ? Math.max(1, ...(production?.steps ?? []).map((step) => step.stageLevel ?? 1)) : 1,
    appendMode,
    initialOutputKey,
  )
}

export function detailRoutePriceDefaults(
  detail: ProcessOrderDetailVO | undefined,
  originalUuid: string | undefined,
  customers: Customer[],
): DetailRoutePriceDefaults {
  const rollSteps = (detail?.steps ?? []).filter((step) => step.originalUuid === originalUuid)
  const customer = customers.find((item) => item.uuid === detail?.order.customerUuid)
  return {
    sawUnitPrice: rollSteps.find((step) => step.stepType === STEP_TYPE_SAW)?.unitPrice ?? customer?.sawPrice,
    rewindUnitPrice: rollSteps.find((step) => step.stepType === STEP_TYPE_REWIND)?.unitPrice ?? customer?.rewindPrice,
  }
}

export function sourceRollFromOutput(row: DetailRouteOutputRow): RollDraft {
  return {
    localId: row.outputKey,
    uuid: row.outputKey,
    paperName: row.paperName ?? '',
    gramWeight: row.gramWeight ?? 0,
    originalWidth: row.finishWidth,
    originalDiameter: row.finishDiameter,
    coreDiameter: row.finishCoreDiameter,
    rollWeight: row.estimateWeight,
    pieceNum: 1,
    processMode: 1,
    mainStepType: STEP_TYPE_REWIND,
  }
}

export function routeOutputRowsForPlan(
  stageLevel: number,
  sources: DetailRouteOutputRow[],
  plan: ProcessPlanDTO,
  existingRows: DetailRouteOutputRow[] = [],
) {
  return calculateRouteOutputs(stageLevel, sources, plan, existingRows)
}

export function buildDetailRouteDto(
  roll: OriginalRoll,
  form: DetailRouteFormState,
): ProcessRoutePreviewDTO {
  return {
    originalUuid: roll.uuid,
    stages: [firstStage(form), ...form.stages.map((stage) => stageDto(form, stage))],
  }
}

export function buildAppendRouteDto(
  roll: OriginalRoll,
  form: DetailRouteFormState,
): ProcessRoutePreviewDTO {
  return {
    originalUuid: roll.uuid,
    stages: form.stages.map((stage) => stageDto(form, stage)),
  }
}

function firstStage(form: DetailRouteFormState): ProcessRouteStageDTO {
  return {
    stageLevel: 1,
    stepType: form.firstStepType,
    stepName: routeStepName(form.firstStepType),
    knifeCount: form.firstStepType === STEP_TYPE_SAW ? form.firstSawKnifeCount : undefined,
    unitPrice: form.firstUnitPrice,
    outputs: form.firstOutputs.map(toOutputDto),
  }
}

function stageDto(
  form: DetailRouteFormState,
  stage: DetailRouteStageForm,
): ProcessRouteStageDTO {
  return {
    stageLevel: stage.stageLevel,
    inputOutputKeys: stage.inputOutputKeys,
    stepType: stage.stepType,
    stepName: routeStepName(stage.stepType),
    knifeCount: stage.stepType === STEP_TYPE_SAW ? stage.plan.knifeCount : undefined,
    unitPrice: stage.plan.unitPrice,
    plan: stage.plan,
    outputs: stageOutputRows(form, stage).map(toOutputDto),
  }
}

function toOutputDto(row: DetailRouteOutputRow): ProcessRouteOutputDTO {
  return {
    outputKey: row.outputKey,
    isRemain: row.isRemain,
    paperName: row.paperName,
    gramWeight: row.gramWeight,
    finishCoreDiameter: row.finishCoreDiameter,
    finishDiameter: row.finishDiameter,
    finishWidth: row.finishWidth,
    estimateWeight: row.estimateWeight,
    remark: row.isRemain === 1 ? '修边/余料' : undefined,
  }
}

function priceForStep(stepType: number, prices: DetailRoutePriceDefaults) {
  return stepType === STEP_TYPE_SAW ? prices.sawUnitPrice : prices.rewindUnitPrice
}
