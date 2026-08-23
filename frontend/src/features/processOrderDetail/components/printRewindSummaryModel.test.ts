import { describe, expect, it } from 'vitest'
import { buildRewindGroupDetail } from './printRewindSummaryModel'

describe('printRewindSummaryModel', () => {
  it('prints estimated group weights as whole kilograms', () => {
    const detail = buildRewindGroupDetail([
      { key: 'a', name: '余料', spec: '余料', weight: '621 kg', weightValue: 621, width: 10, status: 'trim' },
      { key: 'b', name: '余料', spec: '余料', weight: '621 kg', weightValue: 621, width: 10, status: 'trim' },
      { key: 'c', name: '余料', spec: '余料', weight: '620 kg', weightValue: 620, width: 10, status: 'trim' },
    ])

    expect(detail).toContain('1,862 kg')
    expect(detail).not.toContain('1862.000 kg')
  })
})
