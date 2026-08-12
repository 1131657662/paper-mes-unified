import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import { createCustomerSpecDrafts } from './customerSpecDraftModel'
import CustomerSpecEditTable from './CustomerSpecEditTable'
import type { FinishCustomerSpec } from './customerSpecTypes'

describe('客户规格编辑权限', () => {
  it('冻结生产规格时仅保留客户显示重量可编辑', () => {
    const markup = renderToStaticMarkup(
      <CustomerSpecEditTable allowPrintedSpecificationEdit={false}
        rows={createCustomerSpecDrafts([spec()])} selected={['finish-1']}
        onSelect={() => undefined} onUpdate={() => undefined} />,
    )

    expect(markup).toMatch(/aria-label="客户品名 A000001"[^>]*disabled/)
    expect(markup.match(/ant-input-number-disabled/g)).toHaveLength(2)
    expect(markup).toContain('客户重量')
  })
})

function spec(): FinishCustomerSpec {
  return {
    finishUuid: 'finish-1', finishRollNo: 'A000001', finishVersion: 1,
    physicalPaperName: '白卡', physicalGramWeight: 80, physicalFinishWidth: 900,
    physicalWeight: 900, customerPaperName: '客户白卡', customerGramWeight: 85,
    customerFinishWidth: 880, customerDisplayWeight: 910, calculationMode: 'MANUAL',
    specificationChanged: true, weightChanged: true, valid: true,
  }
}
