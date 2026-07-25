import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import type { FinishedProductRow } from '../processOrderDetail/components/finishedProductRows'
import CustomerSpecificationComparisonTable from './CustomerSpecificationComparisonTable'
import type { FinishCustomerSpec } from './customerSpecTypes'

describe('客户口径逐件明细', () => {
  it('展示来源母卷的完整生产字段', () => {
    const markup = renderToStaticMarkup(
      <CustomerSpecificationComparisonTable rows={[row()]} specs={[spec()]} />,
    )

    expect(markup).toContain('白卡')
    expect(markup).toContain('克重 302 g')
    expect(markup).toContain('门幅 2510 mm')
    expect(markup).toContain('卷号 -')
    expect(markup).toContain('编号 NO-2')
    expect(markup).toContain('件重 1100 kg')
  })
})

function row(): FinishedProductRow {
  return {
    key: 'finish-1',
    finish: { uuid: 'finish-1', finishRollNo: 'A000001' },
    sources: [{
      originalUuid: 'roll-2',
      extraNo: 'NO-2',
      paperName: '白卡',
      gramWeight: 300,
      actualGramWeight: 302,
      originalWidth: 2520,
      actualWidth: 2510,
      rollWeight: 1100,
      pieceNum: 2,
    }],
  }
}

function spec(): FinishCustomerSpec {
  return {
    finishUuid: 'finish-1',
    finishRollNo: 'A000001',
    finishVersion: 1,
    physicalWeight: 1100,
    customerDisplayWeight: 1100,
    calculationMode: 'KEEP',
    specificationChanged: false,
    weightChanged: false,
    valid: true,
  }
}
