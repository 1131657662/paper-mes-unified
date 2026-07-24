import { Alert, Form, Input, InputNumber, Radio, Select, Space } from 'antd'
import type { FormInstance } from 'antd'
import type { PricingBatchFormValues } from '../pricingBatchModel'

interface Props {
  form: FormInstance<PricingBatchFormValues>
  prefix: 'strip' | 'repack'
  label: string
  count: number
}

export default function ServicePricingBatchFields({ form, prefix, label, count }: Props) {
  const modeName = `${prefix}Mode` as const
  const basisName = `${prefix}Basis` as const
  const priceName = `${prefix}Price` as const
  const amountName = `${prefix}Amount` as const
  const mode = Form.useWatch(modeName, form) ?? 1
  const basis = Form.useWatch(basisName, form) ?? 'PIECE'
  const unit = basis === 'PIECE' ? '件' : '吨'
  return (
    <section className="pricing-batch-group pricing-batch-group--service">
      <div className="pricing-batch-group__title">
        <strong>{label}</strong><span>{count} 道附加工艺</span>
      </div>
      <Form.Item name={modeName} label="核价方式" className="pricing-batch-group__wide">
        <Radio.Group optionType="button" buttonStyle="solid" options={[
          { value: 1, label: '按件 / 按吨' },
          { value: 3, label: '合计固定金额' },
          { value: 4, label: '免费' },
        ]} />
      </Form.Item>
      {mode === 1 && (
        <>
          <Form.Item name={basisName} label="计费单位" rules={[{ required: true, message: '请选择计费单位' }]}>
            <Select options={[{ value: 'PIECE', label: '按件' }, { value: 'TON', label: '按吨' }]} />
          </Form.Item>
          <Form.Item label={`核定单价（元/${unit}）`} required>
            <Space.Compact block>
              <Form.Item name={priceName} noStyle rules={[{ required: true, message: '请输入核定单价' }, {
                type: 'number', min: 0.0001, message: '核定单价必须大于0',
              }]}>
                <InputNumber aria-label={`${label}核定单价`} min={0.0001} precision={4} style={{ width: '100%' }} />
              </Form.Item>
              <Input aria-label="计价单位" readOnly tabIndex={-1} value={`元/${unit}`} style={{ width: 72 }} />
            </Space.Compact>
          </Form.Item>
          <Alert className="pricing-batch-group__wide" showIcon type="info"
            message={basis === 'PIECE' ? '数量自动取各母卷件数' : '数量自动取各母卷回录实重并折算为吨'} />
        </>
      )}
      {mode === 3 && (
        <Form.Item className="pricing-batch-group__wide" name={amountName} label="所选工序合计固定金额（元）"
          extra={`这是 ${count} 道工序的合计，不会按每卷重复收取；系统会自动分摊且保持合计不变。`}
          rules={[{ required: true, message: '请输入合计固定金额' }, { type: 'number', min: 0, message: '金额不能为负数' }]}>
          <InputNumber min={0} precision={2} style={{ width: '100%' }} />
        </Form.Item>
      )}
      {mode === 4 && <Alert className="pricing-batch-group__wide" showIcon type="success"
        message={`所选 ${count} 道${label}工序将明确记为免费`} />}
    </section>
  )
}
