import { Form } from 'antd'
import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import WarehouseProfileForm from './WarehouseProfileForm'

function WarehouseProfileFormHarness() {
  const [form] = Form.useForm()
  return <WarehouseProfileForm editing={false} form={form} />
}

describe('仓库档案表单', () => {
  it('使用地址/说明文案，同时保留 location 字段名兼容后端', () => {
    const markup = renderToStaticMarkup(
      <Form>
        <WarehouseProfileFormHarness />
      </Form>,
    )

    expect(markup).toContain('仓库地址/说明')
    expect(markup).toContain('填写仓库地址或识别说明')
    expect(markup).not.toContain('库位范围')
  })
})
