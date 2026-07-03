import type { FinishProductionVO, StageOutputVO } from '../../types/processOrder'

export interface RouteNode {
  appendable: boolean
  children: RouteNode[]
  key: string
  level: number
  meta: string
  outputKey?: string
  processLabel: string
  statusColor: string
  statusText: string
  title: string
  weight?: number
}

const OUTPUT_CONSUMED = 2
const OUTPUT_VOID = 4
const ROLL_NO_VOID = 3

export function buildRouteTree(
  outputs: StageOutputVO[],
  finishes: FinishProductionVO[],
  fallbackProcessLabel = '首道加工',
): RouteNode[] {
  const activeOutputRows = outputs.filter((output) => output.outputStatus !== OUTPUT_VOID)
  if (!activeOutputRows.length) return activeFinishes(finishes).map((finish) => toFinishNode(finish, fallbackProcessLabel))

  const nodes = new Map(activeOutputRows.map((output) => [output.uuid, toStageNode(output)]))
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

function toStageNode(output: StageOutputVO): RouteNode {
  return {
    children: [],
    key: output.uuid,
    level: output.stageLevel ?? 1,
    title: output.outputNo || `产物 ${output.outputSort ?? '-'}`,
    meta: formatSpec(output.paperName, output.gramWeight, output.finishWidth),
    weight: output.estimateWeight,
    processLabel: output.sourceSummary || stepTypeText(output.sourceStepType),
    statusColor: outputStatusColor(output.outputStatus),
    statusText: outputStatusText(output.outputStatus),
    outputKey: output.outputNo || output.uuid,
    appendable: isAppendableOutput(output),
  }
}

function toFinishNode(finish: FinishProductionVO, processLabel: string): RouteNode {
  return {
    children: [],
    key: finish.uuid,
    level: 1,
    title: finish.finishRollNo || `成品 ${finish.rowSort ?? '-'}`,
    meta: formatSpec(finish.paperName, finish.gramWeight, finish.finishWidth),
    weight: finish.estimateWeight,
    processLabel,
    statusColor: 'green',
    statusText: '最终成品',
    outputKey: finish.finishRollNo || (finish.uuid ? `F:${finish.uuid}` : undefined),
    appendable: true,
  }
}

function activeFinishes(finishes: FinishProductionVO[]) {
  return finishes.filter((finish) => finish.rollNoStatus !== ROLL_NO_VOID && finish.isSpare !== 1)
}

function isAppendableOutput(output: StageOutputVO) {
  return output.outputStatus !== OUTPUT_CONSUMED && output.outputStatus !== OUTPUT_VOID
}

function sortNodes(nodes: RouteNode[]) {
  return [...nodes].sort((a, b) => a.level - b.level || a.title.localeCompare(b.title))
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
