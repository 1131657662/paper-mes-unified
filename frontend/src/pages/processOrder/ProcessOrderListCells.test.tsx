import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import type { ProcessOrder } from '../../types/processOrder'
import { OrderNoCell, OrderStatusCell } from './ProcessOrderListCells'
import { processSummaryText } from './processOrderListModel'

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

  it('显示真实工艺名称', () => {
    const record: ProcessOrder = {
      uuid: 'order-3', orderNo: 'JG003', processNames: ['锯纸', '剥损整理'],
    }

    const markup = renderToStaticMarkup(<OrderNoCell onDetail={() => undefined} record={record} />)

    expect(markup).toContain('锯纸 + 剥损整理')
    expect(markup).not.toContain('单一工艺')
  })

  it('工艺超过两类时压缩列表文案并保留完整说明', () => {
    const summary = processSummaryText({
      uuid: 'order-4', processNames: ['锯纸', '复卷', '剥损整理', '重新包装'],
    })

    expect(summary.compact).toBe('锯纸 + 复卷 +2')
    expect(summary.full).toBe('锯纸 + 复卷 + 剥损整理 + 重新包装')
  })

  it('同类型多道工序保留多段标识', () => {
    const summary = processSummaryText({
      uuid: 'order-5', processNames: ['锯纸'], isMixProcess: 1,
    })

    expect(summary.compact).toBe('锯纸 · 多段')
    expect(summary.full).toBe('锯纸 · 多段工艺')
  })

  it('区分已下发但未确认打印的加工单', () => {
    const markup = renderToStaticMarkup(
      <OrderStatusCell record={{ uuid: 'order-6', orderStatus: 2, printStatus: 0, printCount: 0 }} />,
    )

    expect(markup).toContain('已下发，未打印')
  })
})
