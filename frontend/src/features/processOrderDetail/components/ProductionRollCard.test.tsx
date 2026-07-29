import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import type { DisplayRow } from '../../../components/processOrder/shared/types'
import ProductionRollCard from './ProductionRollCard'

describe('ProductionRollCard', () => {
  it('shows the saved width difference policy in the plan summary', () => {
    const step = { uuid: 'step-1', isMain: 1, widthDifferencePolicy: 'REMAINDER' as const }
    const finish = {
      uuid: 'finish-1',
      finishWidth: 900,
      estimateWeight: 900,
      isRemain: 0,
      isSpare: 0,
      rollNoStatus: 1,
    }
    const production = {
      originalUuid: 'roll-1',
      rollNo: 'ROLL-1',
      paperName: 'Test paper',
      gramWeight: 80,
      originalWidth: 1000,
      rollWeight: 1000,
      processMode: 1,
      mainStepType: 2,
      rewindParams: [{ paramMode: 1 }],
      steps: [step],
      finishes: [finish],
      stageOutputs: [],
    }
    const row: DisplayRow = {
      key: 'roll-1',
      seq: 1,
      label: 'ROLL-1',
      isMergeGroup: false,
      originalUuids: ['roll-1'],
      rollProductions: [production],
      mainProduction: production,
      sourceProductions: [],
      steps: [step],
      rewindParams: production.rewindParams,
      finishes: [finish],
      totalKnifeCount: 0,
      totalEstimateWeight: 900,
      rewindMode: 1,
      isDirectShip: false,
      hasConfig: true,
    }

    const markup = renderToStaticMarkup(<ProductionRollCard row={row} />)

    expect(markup).toContain('门幅差额：留余料')
  })
})
