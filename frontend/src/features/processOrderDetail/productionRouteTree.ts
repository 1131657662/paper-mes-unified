import type { FinishProductionVO, RollProductionVO, StageOutputVO } from '../../types/processOrder'
import {
  buildFinishLayers,
  buildStageOutputLayers,
  layerItemMap,
} from '../../components/processOrder/shared/layeredRewindView'
import {
  compareLayerRows,
  layerItemSortMap,
  type LayeredRewindSort,
} from '../../components/processOrder/shared/layeredRewindOrder'
import {
  activeFinishesWithFallbackTrim,
  isStageTrimOutput,
  stageOutputsWithFallbackTrim,
} from './productionRouteTrimFallback'

export interface RouteNode {
  appendable: boolean
  children: RouteNode[]
  key: string
  level: number
  layerText?: string
  meta: string
  outputKey?: string
  processLabel: string
  statusColor: string
  statusText: string
  sortInfo?: LayeredRewindSort
  title: string
  weight?: number
}

const OUTPUT_CONSUMED = 2
const OUTPUT_VOID = 4

export function buildRouteTree(
  outputs: StageOutputVO[],
  finishes: FinishProductionVO[],
  fallbackProcessLabel = '首道加工',
  production?: RollProductionVO,
): RouteNode[] {
  const activeOutputRows = stageOutputsWithFallbackTrim(outputs, production)
  if (!activeOutputRows.length) {
    const finishRows = activeFinishesWithFallbackTrim(finishes, production)
    const finishLayers = production ? buildFinishLayers(production, finishRows) : []
    const finishLayerMap = layerItemMap(finishLayers)
    const finishSortMap = layerItemSortMap(finishLayers)
    return sortNodes(finishRows.map((finish) => {
      return toFinishNode(
        finish,
        fallbackProcessLabel,
        finishLayerMap.get(finish.uuid)?.label,
        finishSortMap.get(finish.uuid),
      )
    }))
  }

  const stageLayers = production ? stageLayersFor(production, activeOutputRows) : []
  const stageLayerMap = layerItemMap(stageLayers)
  const stageSortMap = layerItemSortMap(stageLayers)
  const nodes = new Map(activeOutputRows.map((output) => {
    return [output.uuid, toStageNode(
      output,
      stageLayerMap.get(output.uuid)?.label,
      stageSortMap.get(output.uuid),
    )]
  }))
  const childrenByParent = new Map<string, RouteNode[]>()
  const roots: RouteNode[] = []

  for (const output of activeOutputRows) {
    const node = nodes.get(output.uuid)
    if (!node) continue
    const parent = output.parentOutputUuid
    if (parent && nodes.has(parent)) {
      childrenByParent.set(parent, [...(childrenByParent.get(parent) ?? []), node])
    } else {
      roots.push(node)
    }
  }

  for (const [parentUuid, children] of childrenByParent.entries()) {
    nodes.get(parentUuid)!.children = sortNodes(children)
  }
  return sortNodes(roots)
}

function toStageNode(
  output: StageOutputVO,
  layerText?: string,
  sortInfo?: LayeredRewindSort,
): RouteNode {
  const remain = isStageTrimOutput(output)
  return {
    children: [],
    key: output.uuid,
    level: output.stageLevel ?? 1,
    layerText,
    title: remain ? trimTitle(output.outputNo) : output.outputNo || `产物 ${output.outputSort ?? '-'}`,
    meta: formatSpec(output.paperName, output.gramWeight, output.finishWidth),
    weight: output.estimateWeight,
    processLabel: output.sourceSummary || stepTypeText(output.sourceStepType),
    statusColor: remain ? 'orange' : outputStatusColor(output.outputStatus),
    statusText: remain ? '修边/余料' : outputStatusText(output.outputStatus),
    sortInfo,
    outputKey: remain ? undefined : output.outputNo || output.uuid,
    appendable: !remain && isAppendableOutput(output),
  }
}

function toFinishNode(
  finish: FinishProductionVO,
  processLabel: string,
  layerText?: string,
  sortInfo?: LayeredRewindSort,
): RouteNode {
  const isRemain = finish.isRemain === 1
  return {
    children: [],
    key: finish.uuid,
    level: 1,
    layerText,
    title: isRemain ? trimTitle(finish.finishRollNo) : finish.finishRollNo || `成品 ${finish.rowSort ?? '-'}`,
    meta: formatSpec(finish.paperName, finish.gramWeight, finish.finishWidth),
    weight: finish.estimateWeight,
    processLabel,
    statusColor: isRemain ? 'orange' : 'green',
    statusText: isRemain ? '修边/余料' : '最终成品',
    sortInfo,
    outputKey: isRemain ? undefined : finish.finishRollNo || (finish.uuid ? `F:${finish.uuid}` : undefined),
    appendable: !isRemain,
  }
}

function stageLayersFor(production: RollProductionVO, outputs: StageOutputVO[]) {
  const rewindRows = outputs.filter((output) => (output.sourceStepType ?? production.mainStepType) === 2)
  return buildStageOutputLayers(production, rewindRows)
}

function isAppendableOutput(output: StageOutputVO) {
  return output.outputStatus !== OUTPUT_CONSUMED && output.outputStatus !== OUTPUT_VOID
}

function trimTitle(identifier?: string) {
  if (!identifier || identifier === '修边' || identifier === '切边') return '修边'
  return `修边 ${identifier}`
}

function sortNodes(nodes: RouteNode[]) {
  return [...nodes].sort((a, b) => {
    return compareLayerRows(a.sortInfo, b.sortInfo)
      || a.level - b.level
      || a.title.localeCompare(b.title)
  })
}

function formatSpec(name?: string, gram?: number, width?: number) {
  return `${name || '-'} / ${gram ?? '-'}g / ${width ?? '-'}mm`
}

function stepTypeText(stepType?: number) {
  return stepType === 1 ? '锯纸' : stepType === 2 ? '复卷' : '加工'
}

function outputStatusText(status?: number) {
  return status === OUTPUT_CONSUMED ? '进入下道' : status === 3 ? '最终成品' : '阶段产物'
}

function outputStatusColor(status?: number) {
  return status === OUTPUT_CONSUMED ? 'orange' : status === 3 ? 'green' : 'blue'
}
