import { Form, InputNumber, Radio } from 'antd'
import type { ProcessCatalog } from '../../types/processCatalog'
import ServiceStepFields from './ServiceStepFields'

interface Props {
  catalog?: ProcessCatalog
  extraOnly: boolean
  billingMode?: number
  billingBasis?: string
  batchMode?: boolean
  compact?: boolean
}

export default function ProcessStepPricingFields(props: Props) {
  const { catalog, extraOnly, billingMode, billingBasis } = props
  if (!catalog) return null
  const fields = (
    <>
      {!extraOnly && catalog.allowsMainProcess && <MainProcessField />}
      {catalog.pricingStrategy === 'SAW_KNIFE' && <SawPricingFields />}
      {catalog.pricingStrategy === 'REWIND_WEIGHT' && <RewindPricingFields />}
      {catalog.pricingStrategy === 'SERVICE_QUANTITY' && (
        <ServiceStepFields
          key={catalog.uuid}
          catalog={catalog}
          billingMode={billingMode}
          billingBasis={billingBasis}
          batchMode={props.batchMode}
          compact={props.compact}
        />
      )}
    </>
  )
  if (!props.compact) return fields
  return <div className="process-step-pricing-fields process-step-pricing-fields--compact">{fields}</div>
}

function MainProcessField() {
  return (
    <Form.Item label="工序标识" name="isMain" rules={[{ required: true, message: '请选择工序标识' }]}>
      <Radio.Group>
        <Radio value={1}>主工艺</Radio>
        <Radio value={0}>追加工序</Radio>
      </Radio.Group>
    </Form.Item>
  )
}

function SawPricingFields() {
  return (
    <>
      <Form.Item label="锯纸刀数" name="knifeCount">
        <InputNumber placeholder="实际加工刀数" min={0} precision={0} style={{ width: '100%' }} />
      </Form.Item>
      <Form.Item label="锯纸单价（元/刀）" name="unitPrice">
        <InputNumber placeholder="本工序单价" min={0} precision={2} style={{ width: '100%' }} />
      </Form.Item>
    </>
  )
}

function RewindPricingFields() {
  return (
    <>
      <Form.Item label="加工吨位（吨）" name="processWeight">
        <InputNumber placeholder="复卷加工吨位" min={0} precision={3} style={{ width: '100%' }} />
      </Form.Item>
      <Form.Item label="复卷单价（元/吨）" name="unitPrice">
        <InputNumber placeholder="本工序单价" min={0} precision={2} style={{ width: '100%' }} />
      </Form.Item>
    </>
  )
}
