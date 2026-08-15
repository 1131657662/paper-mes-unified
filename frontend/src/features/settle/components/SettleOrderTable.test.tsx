import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import type { SettleOrder } from '../../../types/settle'
import { ReceiveProgress } from './SettleOrderTable'

describe('SettleOrderTable 收款进度', () => {
  it('将累计到账显示为已收而不是已结清', () => {
    const record = {
      totalAmount: 2274,
      receivedAmount: 0,
      unreceivedAmount: 2274,
      cashReceivedAmount: 0,
      scrapOffsetAmount: 0,
      discountAmount: 0,
    } as SettleOrder

    const markup = renderToStaticMarkup(<ReceiveProgress record={record} />)

    expect(markup).toContain('已收 ¥0.00 / 未收 ¥2274.00')
    expect(markup).toContain('实际到账 ¥0.00 / 废纸 ¥0.00 / 优惠 ¥0.00')
    expect(markup).toContain('aria-label="收款进度"')
    expect(markup).not.toContain('已结清 ¥0.00')
  })
})
