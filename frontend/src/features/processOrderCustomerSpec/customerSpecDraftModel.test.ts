import { describe, expect, it } from 'vitest'
import {
  STANDARD_WEIGHT_FORMULA,
  applyWeightRule,
  buildFinishCustomerRevisionRequest,
  createCustomerSpecDrafts,
  prepareCustomerSpecPreviewRows,
} from './customerSpecDraftModel'

describe('客户口径版本请求', () => {
  it('公式按所选成品显式进入预览请求', () => {
    const rows = createCustomerSpecDrafts([{ finishUuid: 'f1', finishVersion: 3, calculationMode: 'KEEP', valid: true, specificationChanged: false, weightChanged: false }])
    const changed = applyWeightRule(rows, ['f1'], { mode: 'FORMULA', formula: STANDARD_WEIGHT_FORMULA, skipZero: true })
    const request = buildFinishCustomerRevisionRequest(8, '客户要求改标签', 'req-1', changed, ['f1'])
    expect(request).toMatchObject({ requestId: 'req-1', expectedOrderVersion: 8, reason: '客户要求改标签' })
    expect(request.items[0]).toMatchObject({ calculationMode: 'FORMULA', formulaExpression: STANDARD_WEIGHT_FORMULA, zeroPolicy: 'SKIP', roundingScale: 0 })
  })

  it('未选择的成品不会写入版本', () => {
    const rows = createCustomerSpecDrafts([
      { finishUuid: 'f1', finishVersion: 1, calculationMode: 'KEEP', valid: true, specificationChanged: false, weightChanged: false },
      { finishUuid: 'f2', finishVersion: 1, calculationMode: 'KEEP', valid: true, specificationChanged: false, weightChanged: false },
    ])
    expect(buildFinishCustomerRevisionRequest(1, '局部调整', 'req-2', rows, ['f2']).items.map((item) => item.finishUuid)).toEqual(['f2'])
  })

  it('预览时自动把当前公式应用到所选成品', () => {
    const rows = createCustomerSpecDrafts([
      { finishUuid: 'f1', finishVersion: 1, calculationMode: 'KEEP', valid: true, specificationChanged: false, weightChanged: false },
      { finishUuid: 'f2', finishVersion: 1, calculationMode: 'KEEP', valid: true, specificationChanged: false, weightChanged: false },
    ])

    const result = prepareCustomerSpecPreviewRows({
      rows, selected: ['f1'], applyPendingRule: true,
      rule: { mode: 'FORMULA', formula: STANDARD_WEIGHT_FORMULA, skipZero: true },
    })

    expect(result[0]).toMatchObject({ calculationMode: 'FORMULA', formulaExpression: STANDARD_WEIGHT_FORMULA })
    expect(result[1]).toBe(rows[1])
  })

  it('默认保持规则不会覆盖逐件录入的重量方式', () => {
    const rows = createCustomerSpecDrafts([
      { finishUuid: 'f1', finishVersion: 1, calculationMode: 'MANUAL', customerDisplayWeight: 1186, valid: true, specificationChanged: false, weightChanged: true },
    ])

    const result = prepareCustomerSpecPreviewRows({
      rows, selected: ['f1'], applyPendingRule: false,
      rule: { mode: 'KEEP', skipZero: true },
    })

    expect(result).toBe(rows)
    expect(result[0]).toMatchObject({ calculationMode: 'MANUAL', customerDisplayWeight: 1186 })
  })

  it('用户明确切换到保持时会更新所选成品', () => {
    const rows = createCustomerSpecDrafts([
      { finishUuid: 'f1', finishVersion: 1, calculationMode: 'FORMULA', customerDisplayWeight: 1260, valid: true, specificationChanged: false, weightChanged: true },
    ])

    const result = prepareCustomerSpecPreviewRows({
      rows, selected: ['f1'], applyPendingRule: true,
      rule: { mode: 'KEEP', skipZero: true },
    })

    expect(result[0]).toMatchObject({ calculationMode: 'KEEP', customerDisplayWeight: 1260 })
  })
})
