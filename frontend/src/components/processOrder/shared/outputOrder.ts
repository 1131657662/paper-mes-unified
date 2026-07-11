import type { FinishProductionVO, RollProductionVO, StageOutputVO } from '../../../types/processOrder'
import {
  buildFinishLayers,
  buildStageOutputLayers,
  type LayeredRewindLayer,
} from './layeredRewindView'
import {
  compareLayerRows,
  layerItemSortMap,
  type LayeredRewindSort,
} from './layeredRewindOrder'

interface OrderedOutput {
  finishWidth?: number
  isRemain?: number
  outputNo?: string
  outputSort?: number
  paperName?: string
  remark?: string
  rowSort?: number
  uuid: string
}

export function sortFinishOutputs(
  production: RollProductionVO,
  outputs: FinishProductionVO[],
): FinishProductionVO[] {
  const layers = buildFinishLayers(production, outputs)
  return sortOutputs(outputs, layers, finishSortValue)
}

export function sortStageOutputs(
  production: RollProductionVO,
  outputs: StageOutputVO[],
): StageOutputVO[] {
  const layers = buildStageOutputLayers(production, outputs)
  return sortOutputs(outputs, layers, stageSortValue)
}

function sortOutputs<T extends OrderedOutput>(
  outputs: T[],
  layers: Array<LayeredRewindLayer<T>>,
  sortValue: (output: T, index: number) => number,
): T[] {
  const layerSortMap = layerItemSortMap(layers)
  return outputs.map((output, index) => ({ output, index }))
    .sort((a, b) => compareOutputs(a, b, layerSortMap, sortValue))
    .map((entry) => entry.output)
}

function compareOutputs<T extends OrderedOutput>(
  a: { output: T; index: number },
  b: { output: T; index: number },
  layerSortMap: Map<string, LayeredRewindSort>,
  sortValue: (output: T, index: number) => number,
): number {
  return compareLayerRows(layerSortMap.get(a.output.uuid), layerSortMap.get(b.output.uuid))
    || trimRank(a.output) - trimRank(b.output)
    || sortValue(a.output, a.index) - sortValue(b.output, b.index)
}

function finishSortValue(output: OrderedOutput, index: number): number {
  return output.rowSort ?? index
}

function stageSortValue(output: OrderedOutput, index: number): number {
  return output.outputSort ?? index
}

export function trimRank(output: OrderedOutput): number {
  return isTrimOutput(output) ? 1 : 0
}

function isTrimOutput(output: OrderedOutput): boolean {
  return output.isRemain === 1
    || output.outputNo === '切边'
    || output.outputNo === '修边'
    || output.paperName === '切边'
    || output.paperName === '修边'
    || output.paperName === '修边/余料'
    || output.remark === '修边/余料'
}
