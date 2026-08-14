import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import type { SettlePrintLine } from '../../types/settle'
import { SettleFinishResultCell } from './SettleCustomerLineCells'

describe('结算成品结果单元格', () => {
  it('共享合并成品的后续母卷显示引用说明', () => {
    const markup = renderToStaticMarkup(
      <SettleFinishResultCell line={line({ sharedFinishResult: true })} />,
    )

    expect(markup).toContain('同一合并成品')
    expect(markup).toContain('见首条关联母卷')
    expect(markup).not.toContain('1 卷 / 3 kg')
  })
})

function line(overrides: Partial<SettlePrintLine>): SettlePrintLine {
  return {
    settleUuid: 'settle-1',
    orderUuid: 'order-1',
    orderNo: 'JG001',
    originalUuid: 'roll-2',
    originalLabel: '母卷2',
    finishCount: 1,
    finishWeight: 3,
    ...overrides,
  }
}
