import { Form } from 'antd'
import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import BaseInfoFormSections from './BaseInfoFormSections'

describe('加工单基础信息表单', () => {
  it('新建表单不再提供班组字段', () => {
    const markup = renderToStaticMarkup(
      <Form>
        <BaseInfoFormSections
          customerSelected={false}
          onCustomerChange={() => undefined}
          onSettleModeChange={() => undefined}
          options={{ customers: [], invoices: [], priorities: [], settlements: [], warehouses: [] }}
        />
      </Form>,
    )

    expect(markup).toContain('客户与生产安排')
    expect(markup).not.toContain('name="teamGroup"')
    expect(markup).not.toContain('班组')
    expect(markup).toContain('id="remarkLong"')
    expect(markup).toContain('客户加工要求')
    expect(markup).toContain('maxLength="2000"')
  })
})
