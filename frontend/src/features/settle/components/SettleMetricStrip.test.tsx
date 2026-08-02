import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import type { SettleOrder } from '../../../types/settle'
import SettleMetricStrip from './SettleMetricStrip'

describe('settlement metric strip', () => {
  it('excludes void documents from receivable and settled counts', () => {
    const orders = [
      { settleStatus: 1, totalAmount: 100, unreceivedAmount: 100 },
      { settleStatus: 3, totalAmount: 50, unreceivedAmount: 0 },
      { settleStatus: 4, totalAmount: 80, unreceivedAmount: 80 },
    ] as SettleOrder[]

    const markup = renderToStaticMarkup(<SettleMetricStrip orders={orders} selectedCandidates={[]} />)

    expect(markup).toContain('待收款</span><strong>1 张')
    expect(markup).toContain('已结清</span><strong>1 张')
  })
})
