import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import SettleQuoteSummary from './SettleQuoteSummary'

describe('结算试算摘要', () => {
  it('条件未完成时显示调用方提供的操作引导', () => {
    const markup = renderToStaticMarkup(
      <SettleQuoteSummary emptyText="选择客户和归属日期范围后显示准确试算"
        loading={false} onRetry={() => undefined} />,
    )

    expect(markup).toContain('选择客户和归属日期范围后显示准确试算')
  })

  it('试算失败时提供重新试算入口', () => {
    const markup = renderToStaticMarkup(
      <SettleQuoteSummary emptyText="选择加工单后显示准确试算" error
        loading={false} onRetry={() => undefined} />,
    )

    expect(markup).toContain('试算失败')
    expect(markup).toContain('重新试算')
  })
})
