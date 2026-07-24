import { Form, InputNumber, Select } from 'antd'
import type { ProcessCatalog } from '../../../types/processCatalog'

interface Props {
  catalog?: ProcessCatalog
  billingBasis?: string
}

export default function ProcessStepPricingServiceFields({ catalog, billingBasis }: Props) {
  const unit = catalog?.units.find((item) => item.code === billingBasis)
    ?? catalog?.units.find((item) => item.defaultUnit)
    ?? catalog?.units[0]
  return (
    <>
      <Form.Item name="billingBasis" label="计费单位" rules={[{ required: true, message: '请选择按件或按吨计费' }]}>
        <Select options={catalog?.units.map((item) => ({ value: item.code, label: item.name }))} />
      </Form.Item>
      <Form.Item name="billingUnitPrice" label={`核定单价（元/${unit?.name ?? '单位'}）`}
        extra={unit?.code === 'PIECE' ? '计费数量自动取母卷件数。' : '计费数量优先取回录后的母卷实重并折算为吨。'}
        rules={[{ required: true, message: '请输入核定单价' }, {
          type: 'number', min: 0.0001, message: '核定单价必须大于0',
        }]}>
        <InputNumber min={0.0001} precision={4} style={{ width: '100%' }} />
      </Form.Item>
    </>
  )
}
