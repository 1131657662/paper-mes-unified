import { describe, expect, it } from 'vitest'
import type { ProcessOrderDetailVO } from '../../../types/processOrder'
import { buildBackRecordDTO, initialBackRecordValues } from './backRecordUtils'

describe('back-record step submission', () => {
  it('omits a zero planned knife count from the request JSON', () => {
    const detail = stepDetail(0)

    const dto = buildBackRecordDTO(detail, initialBackRecordValues(detail))

    expect(JSON.stringify(dto)).not.toContain('knifeCount')
  })

  it('keeps a positive actual knife count in the request', () => {
    const detail = stepDetail(0)
    const values = initialBackRecordValues(detail)
    values.steps = { 'step-1': { knifeCount: 3 } }

    const dto = buildBackRecordDTO(detail, values)

    expect(dto.steps?.[0]?.knifeCount).toBe(3)
  })
})

function stepDetail(knifeCount: number): ProcessOrderDetailVO {
  return {
    order: { uuid: 'order-1' },
    originalRolls: [],
    rolls: [],
    finishRolls: [],
    steps: [{ uuid: 'step-1', knifeCount }],
  }
}
