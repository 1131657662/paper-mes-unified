import { describe, expect, it } from 'vitest'
import {
  DEFAULT_WIDTH_DIFFERENCE_POLICY,
  processModeRequiresMain,
  WIDTH_DIFFERENCE_POLICY_OPTIONS,
} from '../../constants/processOrder'
import { defaultPlanForRoll } from './draftMappers'
import type { RollDraft } from './types'

const sawRoll: RollDraft = {
  localId: 'roll-1',
  paperName: '白卡',
  gramWeight: 300,
  originalWidth: 2353,
  rollWeight: 2285,
  processMode: 1,
  mainStepType: 1,
}

describe('width difference policy defaults', () => {
  it('orders allocation first and loss last', () => {
    expect(WIDTH_DIFFERENCE_POLICY_OPTIONS.map((option) => option.value))
      .toEqual(['ALLOCATE', 'REMAINDER', 'LOSS'])
  })

  it('defaults new saw plans to allocation', () => {
    expect(DEFAULT_WIDTH_DIFFERENCE_POLICY).toBe('ALLOCATE')
    expect(defaultPlanForRoll(sawRoll).widthDifferencePolicy).toBe('ALLOCATE')
  })
})

describe('service-only process policy', () => {
  it('does not require a saw or rewind main process', () => {
    expect(processModeRequiresMain(4)).toBe(false)
    expect(defaultPlanForRoll({ ...sawRoll, processMode: 4 }).mainStepType).toBeUndefined()
  })
})
