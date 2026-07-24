import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import PhysicalSpecificationDetailView from './PhysicalSpecificationDetailView'
import type { FinishedProductRow } from './finishedProductRows'

describe('实物成品详情', () => {
  it('同时展示规格汇总和逐件明细', () => {
    const markup = renderToStaticMarkup(<PhysicalSpecificationDetailView rows={[row()]} />)

    expect(markup).toContain('规格汇总')
    expect(markup).toContain('实物合计')
    expect(markup).toContain('逐件明细')
    expect(markup).toContain('A000568')
  })
})

function row(): FinishedProductRow {
  return {
    finish: {
      uuid: 'finish-1', finishRollNo: 'A000568', paperName: '白卡',
      gramWeight: 250, finishWidth: 1200, estimateWeight: 1176, actualWeight: 1176,
    },
    key: 'finish-1',
    sources: [{ rollNo: 'M001' }],
  }
}
