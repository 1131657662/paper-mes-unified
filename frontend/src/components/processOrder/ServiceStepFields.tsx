import { Alert, Form, InputNumber, Radio, Select, Typography } from 'antd'
import type { ProcessCatalog } from '../../types/processCatalog'

interface Props {
  catalog: ProcessCatalog
  billingMode?: number
  billingBasis?: string
  batchMode?: boolean
  compact?: boolean
}

const BILLING_MODE_NAMES: Record<number, string> = {
  0: '暂不定价',
  1: '标准计费',
  2: '指定数量',
  3: '固定金额',
  4: '免费',
}

export default function ServiceStepFields({ catalog, billingMode, billingBasis, batchMode, compact }: Props) {
  const activeBillingMode = billingMode ?? 0
  const defaultUnit = catalog.units.find((unit) => unit.defaultUnit) ?? catalog.units[0]
  const activeBillingBasis = billingBasis ?? defaultUnit?.code
  const selectedUnit = catalog.units.find((unit) => unit.code === activeBillingBasis)
  const unitName = selectedUnit?.name ?? '单位'
  const billingOptions = [0, ...catalog.billingModes].map((mode) => ({
    label: BILLING_MODE_NAMES[mode] ?? `模式 ${mode}`,
    value: mode,
  }))
  return (
    <>
      <Form.Item className="service-step-fields__mode" label="计费方式" name="billingMode"
        rules={[{ required: true }]}>
        <Radio.Group options={billingOptions} optionType="button" buttonStyle="solid" />
      </Form.Item>
      {activeBillingMode === 0 && <PendingPricingFields catalog={catalog} compact={compact} />}
      {activeBillingMode === 1 && <StandardServiceFields catalog={catalog} unitName={unitName}
        compact={compact} />}
      {activeBillingMode === 3 && <FixedAmountFields batchMode={batchMode} compact={compact} />}
    </>
  )
}

function StandardServiceFields({ catalog, unitName, compact }: {
  catalog: ProcessCatalog
  unitName: string
  compact?: boolean
}) {
  return (
    <>
      <Form.Item className="service-step-fields__unit" label="计费单位" name="billingBasis"
        rules={[{ required: true, message: '请选择计费单位' }]}>
        <Select options={catalog.units.map((unit) => ({ label: unit.name, value: unit.code }))} />
      </Form.Item>
      <Form.Item className="service-step-fields__price" label={`服务单价（元/${unitName}）`} name="unitPrice" rules={[{ required: true, message: '请填写服务单价' }]}>
        <InputNumber min={0.01} precision={2} style={{ width: '100%' }} />
      </Form.Item>
      <AutoQuantityHint compact={compact} />
    </>
  )
}

function PendingPricingFields({ catalog, compact }: {
  catalog: ProcessCatalog
  compact?: boolean
}) {
  return (
    <>
      <Form.Item className="service-step-fields__unit" label="预计计费单位" name="billingBasis"
        rules={[{ required: true, message: '请选择后续计费单位' }]}>
        <Select options={catalog.units.map((unit) => ({ label: unit.name, value: unit.code }))} />
      </Form.Item>
      {compact ? <InlineHint>费用可稍后核定，结算前补价或设为免费。</InlineHint> : (
        <Alert showIcon type="info" message="本次只记录服务，费用稍后核定"
          description="提货完成后仍可补充单价；生成结算单前必须核价或明确选择免费。" />
      )}
      <AutoQuantityHint compact={compact} />
    </>
  )
}

function AutoQuantityHint({ compact }: { compact?: boolean }) {
  if (compact) return <InlineHint>数量自动取母卷件数或回录实重，无需填写。</InlineHint>
  return <Alert showIcon type="success" message="计费数量由系统自动计算"
    description="按件取母卷件数；按吨在回录后优先取母卷实重，无需手工填写数量。" />
}

function InlineHint({ children }: { children: string }) {
  return <Typography.Text className="process-step-pricing-fields__hint" type="secondary">
    {children}
  </Typography.Text>
}

function FixedAmountFields({ batchMode, compact }: { batchMode?: boolean; compact?: boolean }) {
  return (
    <>
      {(batchMode || compact) && (
        <Form.Item className="service-step-fields__scope" label="批量金额口径"
          name="fixedAmountScope">
          <Radio.Group optionType="button" buttonStyle="solid" options={[
            { label: '所选合计', value: 'TOTAL' },
            { label: '每卷金额', value: 'EACH' },
          ]} />
        </Form.Item>
      )}
      <Form.Item className="service-step-fields__amount"
        label={batchMode ? '固定金额（元）' : compact ? '金额（元）' : '本卷固定金额（元）'}
        name="billingAmount"
        extra={batchMode || compact
          ? '保存当前卷时始终作为本卷金额；批量应用时按上方口径分摊或逐卷复制。'
          : undefined}
        rules={[{ required: true, message: '请填写固定金额' }]}>
        <InputNumber min={0} precision={2} style={{ width: '100%' }} />
      </Form.Item>
    </>
  )
}
