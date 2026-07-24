import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import type { FinishCustomerSpec } from '../../processOrderCustomerSpec/customerSpecTypes'
import FinishedProductsTable from './FinishedProductsTable'
import type { FinishedProductRow } from './finishedProductRows'

describe('加工成品客户口径', () => {
  it('默认同时展示规格汇总和逐件明细', () => {
    const markup = renderToStaticMarkup(<FinishedProductsTable rows={[row()]} specs={[spec()]} />)

    expect(markup).toContain('规格汇总')
    expect(markup).toContain('逐件明细')
    expect(markup).toContain('客户口径合计')
    expect(markup).toContain('A000568')
    expect(markup).not.toContain('规格对照')
  })

  it('客户口径加载失败时只展示实物数据', () => {
    const markup = renderToStaticMarkup(
      <FinishedProductsTable customerSpecsError rows={[row()]} />,
    )

    expect(markup).toContain('实物合计')
    expect(markup).not.toContain('客户口径合计')
    expect(markup).toMatch(/ant-segmented-item-disabled[\s\S]*客户口径/)
  })
})

function row(): FinishedProductRow {
  return {
    key: 'finish-1', sources: [{ rollNo: 'M001' }],
    finish: { uuid: 'finish-1', finishRollNo: 'A000568', paperName: '白卡', gramWeight: 250, finishWidth: 1200, actualWeight: 1176 },
  }
}

function spec(): FinishCustomerSpec {
  return {
    finishUuid: 'finish-1', finishRollNo: 'A000568', finishVersion: 1,
    physicalPaperName: '白卡', physicalGramWeight: 250, physicalFinishWidth: 1200,
    physicalWeight: 1176, customerPaperName: '食品卡', customerGramWeight: 275,
    customerFinishWidth: 1195, customerDisplayWeight: 1288, calculationMode: 'FORMULA',
    specificationChanged: true, weightChanged: true, valid: true,
  }
}
