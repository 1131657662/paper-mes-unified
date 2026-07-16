import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import SettleCandidateTable from './SettleCandidateTable'

describe('结算候选表格', () => {
  it('条件不足时显示明确引导而不是普通空数据', () => {
    const markup = renderToStaticMarkup(
      <SettleCandidateTable data={[]} emptyText="请选择客户和归属日期范围后查看候选"
        loading={false} selectedRowKeys={[]} onSelectionChange={() => undefined} />,
    )

    expect(markup).toContain('请选择客户和归属日期范围后查看候选')
    expect(markup).not.toContain('暂无数据')
  })

  it('同时区分制单日期和账期归属日期', () => {
    const markup = renderToStaticMarkup(
      <SettleCandidateTable data={[candidate()]} loading={false}
        selectedRowKeys={[]} onSelectionChange={() => undefined} />,
    )

    expect(markup).toContain('制单 2026-06-30')
    expect(markup).toContain('归属日期')
    expect(markup).toContain('2026-07-02')
  })
})

function candidate() {
  return {
    accountingDate: '2026-07-02',
    customerName: '测试客户',
    customerUuid: 'customer-1',
    orderDate: '2026-06-30',
    orderNo: 'JG202606300001',
    orderUuid: 'order-1',
  }
}
