import type { ProcessPlanDTO } from '../../types/processOrder'
import type {
  DetailRouteFormState,
  DetailRouteOutputRow,
  DetailRoutePriceDefaults,
  DetailRouteStageForm,
} from './routeConfigModel'
import { calculateRouteOutputs } from './routeConfigOutputCalculator'
import { defaultPlanForSources } from './routeConfigPlanDefaults'
import { sourceRowsFromKeys } from './routeConfigSource'

export function addDetailRouteStage(
  form: DetailRouteFormState,
  stepType: number,
  prices: DetailRoutePriceDefaults,
): DetailRouteFormState {
  const source = nextStageDefaultSource(form)
  if (!source) return form
  const stageLevel = (source.stageLevel || 1) + 1
  return { ...form, stages: [...form.stages, newStage(stageLevel, source, stepType, prices)] }
}

export function changeDetailRouteStageInputs(
  form: DetailRouteFormState,
  stageId: string,
  inputOutputKeys: string[],
  prices: DetailRoutePriceDefaults,
): DetailRouteFormState {
  return updateStage(form, stageId, (stage) => {
    const sources = sourceRowsFromKeys(inputRowsForStage(form, stage.id), inputOutputKeys)
    if (!sources.length) return stage
    return {
      ...stage,
      inputOutputKeys: sources.map((row) => row.outputKey),
      plan: defaultPlanForSources(sources, stage.stepType, prices),
    }
  })
}

export function changeDetailRouteStageType(
  form: DetailRouteFormState,
  stageId: string,
  stepType: number,
  prices: DetailRoutePriceDefaults,
): DetailRouteFormState {
  return updateStage(form, stageId, (stage) => {
    const sources = sourceRowsFromKeys(inputRowsForStage(form, stage.id), stage.inputOutputKeys)
    if (!sources.length) return { ...stage, stepType }
    return { ...stage, stepType, plan: defaultPlanForSources(sources, stepType, prices) }
  })
}

export function updateDetailRouteStagePlan(
  form: DetailRouteFormState,
  stageId: string,
  plan: ProcessPlanDTO,
): DetailRouteFormState {
  return updateStage(form, stageId, (stage) => ({ ...stage, plan }))
}

export function removeLastDetailRouteStage(form: DetailRouteFormState) {
  return { ...form, stages: form.stages.slice(0, -1) }
}

export function inputRowsForStage(form: DetailRouteFormState, stageId: string) {
  const index = form.stages.findIndex((stage) => stage.id === stageId)
  const stageIndex = index < 0 ? form.stages.length : index
  const currentInputs = new Set(index >= 0 ? form.stages[index]?.inputOutputKeys ?? [] : [])
  const consumed = consumedInputKeysBeforeStage(form, stageIndex)
  return outputRowsBeforeStage(form, stageIndex)
    .filter((row) => row.isRemain !== 1)
    .filter((row) => !consumed.has(row.outputKey) || currentInputs.has(row.outputKey))
}

export function stageOutputRows(form: DetailRouteFormState, stage: DetailRouteStageForm) {
  const beforeRows = outputRowsBeforeStage(form, stageIndexOf(form, stage.id))
  const sources = sourceRowsFromKeys(beforeRows, stage.inputOutputKeys)
  if (!sources.length) return []
  return calculateRouteOutputs(stage.stageLevel, sources, stage.plan, beforeRows)
}

export function selectedInputRowsForStage(
  form: DetailRouteFormState,
  stage: DetailRouteStageForm,
) {
  return sourceRowsFromKeys(inputRowsForStage(form, stage.id), stage.inputOutputKeys)
}

export function finalDetailRouteOutputs(form: DetailRouteFormState) {
  const consumed = new Set(form.stages.flatMap((stage) => stage.inputOutputKeys))
  return allDetailRouteOutputs(form)
    .filter((row) => row.isRemain !== 1 && !consumed.has(row.outputKey))
}

export function allDetailRouteOutputs(form: DetailRouteFormState) {
  return outputRowsBeforeStage(form, form.stages.length)
}

function newStage(
  stageLevel: number,
  source: DetailRouteOutputRow,
  stepType: number,
  prices: DetailRoutePriceDefaults,
): DetailRouteStageForm {
  return {
    id: `stage-${stageLevel}-${source.outputKey}`,
    inputOutputKeys: [source.outputKey],
    plan: defaultPlanForSources([source], stepType, prices),
    stageLevel,
    stepType,
  }
}

function outputRowsBeforeStage(form: DetailRouteFormState, stageIndex: number) {
  return form.stages.slice(0, stageIndex).reduce((rows, stage) => {
    const sources = sourceRowsFromKeys(rows, stage.inputOutputKeys)
    return sources.length
      ? [...rows, ...calculateRouteOutputs(stage.stageLevel, sources, stage.plan, rows)]
      : rows
  }, form.firstOutputs)
}

function consumedInputKeysBeforeStage(form: DetailRouteFormState, stageIndex: number) {
  return new Set(form.stages.slice(0, stageIndex).flatMap((stage) => stage.inputOutputKeys))
}

function nextStageDefaultSource(form: DetailRouteFormState) {
  const lastStage = form.stages.at(-1)
  if (lastStage) {
    const lastOutputs = stageOutputRows(form, lastStage)
    if (lastOutputs.length) return lastOutputs[0]
  }
  const finals = finalDetailRouteOutputs(form)
  if (!form.preferredSourceOutputKey) return finals[0]
  return finals.find((row) => (
    row.outputKey === form.preferredSourceOutputKey
    || row.finishRollNo === form.preferredSourceOutputKey
  )) ?? finals[0]
}

function stageIndexOf(form: DetailRouteFormState, stageId: string) {
  const index = form.stages.findIndex((stage) => stage.id === stageId)
  return index < 0 ? form.stages.length : index
}

function updateStage(
  form: DetailRouteFormState,
  stageId: string,
  updater: (stage: DetailRouteStageForm) => DetailRouteStageForm,
) {
  return {
    ...form,
    stages: form.stages.map((stage) => (stage.id === stageId ? updater(stage) : stage)),
  }
}
