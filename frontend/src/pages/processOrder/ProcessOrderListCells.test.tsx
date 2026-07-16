import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import type { ProcessOrder } from '../../types/processOrder'
import { OrderNoCell } from './ProcessOrderListCells'

describe('加工单列表单号单元格', () => {
  it('保留详情链接并提供复制按钮', () => {
    const record: ProcessOrder = { uuid: 'order-1', orderNo: 'JG202607150011' }

    const markup = renderToStaticMarkup(<OrderNoCell onDetail={() => undefined} record={record} />)

    expect(markup).toContain('href="/process-orders/order-1"')
    expect(markup).toContain('aria-label="复制加工单号 JG202607150011"')
    expect(markup).toContain('process-order-list__copy-slot')
  })

  it('没有单号时不渲染复制按钮', () => {
    const record: ProcessOrder = { uuid: 'order-2' }

    const markup = renderToStaticMarkup(<OrderNoCell onDetail={() => undefined} record={record} />)

    expect(markup).not.toContain('aria-label="复制加工单号')
    expect(markup).toContain('process-order-list__copy-slot')
  })
})
