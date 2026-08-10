import type { FinishConfigSaveDTO, FinishPreviewVO } from '../../types/processOrder'
import { toFinishSpecs, toPreviewDto, type SegmentForm } from './rewindingConfigModel'

export interface RewindingConfigValues {
  nextPreview: FinishPreviewVO | null
  nextRewindMode: number
  nextUnitPrice?: number
  nextSpareCount: number
  nextSegments: SegmentForm[]
  processMode: number
}

export function toRewindingConfig(values: RewindingConfigValues): FinishConfigSaveDTO {
  return {
    processMode: values.processMode,
    mainStepType: 2,
    rewindMode: values.nextRewindMode,
    unitPrice: values.nextUnitPrice,
    spareCount: values.nextSpareCount,
    finishSpecs: toFinishSpecs(values.nextPreview, values.nextSegments),
    rewindSegments: toPreviewDto(
      values.nextRewindMode,
      values.nextSpareCount,
      values.nextSegments,
    ).segments,
  }
}

interface OnSiteConfigValues {
  count: number
  processMode: number
  rewindMode: number
  spareCount: number
  unitPrice?: number
}

export function toOnSiteRewindingConfig(values: OnSiteConfigValues): FinishConfigSaveDTO {
  return {
    processMode: values.processMode,
    mainStepType: 2,
    rewindMode: values.rewindMode,
    unitPrice: values.unitPrice,
    spareCount: values.spareCount,
    finishSpecs: [{
      count: values.count,
      finishWidth: 0,
      finishDiameter: 0,
      finishCoreDiameter: 0,
      estimateWeight: 0,
    }],
  }
}
