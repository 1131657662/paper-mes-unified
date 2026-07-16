import { Form } from 'antd'
import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import SettleConditionFields from './SettleConditionFields'

describe('结算条件字段', () => {
  it('账期模式明确标记客户和归属日期范围为必填', () => {
    const markup = renderToStaticMarkup(
      <Form>
        <SettleConditionFields customers={[]} invoiceOptions={[]} isMonthMode loading={false} />
      </Form>,
    )

    expect(markup).toContain('按账期自动圈单')
    expect(markup.match(/ant-form-item-required/g)).toHaveLength(3)
    expect(markup).toContain('请选择客户')
    expect(markup).toContain('归属日期范围')
  })
})
