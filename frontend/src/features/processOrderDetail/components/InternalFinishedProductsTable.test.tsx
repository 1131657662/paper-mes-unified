import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import InternalFinishedProductsTable from './InternalFinishedProductsTable'

describe('InternalFinishedProductsTable', () => {
  it('distinguishes planned allocation from measured source weight', () => {
    const markup = renderToStaticMarkup(<InternalFinishedProductsTable rows={[{
      key: 'finish-1',
      finish: {
        uuid: 'finish-1',
        finishRollNo: 'A001',
        paperName: '测试纸',
        estimateWeight: 1,
        actualWeight: 666.6,
      },
      sources: [{
        originalUuid: 'roll-1',
        paperName: '测试纸',
        shareRatio: 33.33,
        shareWeight: 1,
        actualWeight: 666.6,
        weightStatus: 'MEASURED',
      }],
    }]} />)

    expect(markup).toContain('计划分摊 33.33% / 1 kg')
    expect(markup).toContain('来源实测 666.6 kg')
  })
})
