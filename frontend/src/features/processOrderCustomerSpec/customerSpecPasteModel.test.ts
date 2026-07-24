import { describe, expect, it } from 'vitest'
import { createCustomerSpecDrafts } from './customerSpecDraftModel'
import { applyPastedCustomerSpecs } from './customerSpecPasteModel'

describe('applyPastedCustomerSpecs', () => {
  it('按卷号匹配粘贴的客户规格并把重量标记为手工值', () => {
    const rows = createCustomerSpecDrafts([
      { finishUuid: 'f1', finishRollNo: 'A1', finishVersion: 1, calculationMode: 'KEEP', valid: true, specificationChanged: false, weightChanged: false },
      { finishUuid: 'f2', finishRollNo: 'A2', finishVersion: 1, calculationMode: 'KEEP', valid: true, specificationChanged: false, weightChanged: false },
    ])
    const result = applyPastedCustomerSpecs(rows, '卷号\t品名\t克重\t门幅\t重量\nA2\t食品卡\t75\t500\t1186')
    expect(result[0]!.calculationMode).toBe('KEEP')
    expect(result[1]).toMatchObject({ customerPaperName: '食品卡', customerGramWeight: 75, customerFinishWidth: 500, customerDisplayWeight: 1186, calculationMode: 'MANUAL' })
  })
})
