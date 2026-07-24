import { CalculatorOutlined } from '@ant-design/icons'
import { Descriptions, Form, Input, InputNumber, Modal, Select, Space, Tag } from 'antd'
import type { FormInstance } from 'antd'
import type { ProcessStepPricingAdjustmentDTO } from '../../../api/processOrder'
import { findProcessCatalog } from '../../../components/processOrder/processStepCatalogModel'
import { useProcessCatalog } from '../../processCatalog/hooks/useProcessCatalog'
import type { ProcessCatalog } from '../../../types/processCatalog'
import type { OriginalRoll, ProcessStep } from '../../../types/processOrder'
import { formatMoney, formatNumber } from '../orderDetailUtils'
import { isServiceStep, pricingPreview, type PricingPreview } from '../processStepPricingModel'
import ProcessStepPricingServiceFields from './ProcessStepPricingServiceFields'

type FormValues = ProcessStepPricingAdjustmentDTO

interface Props {
  open: boolean
  step: ProcessStep | null
  originalRoll?: OriginalRoll
  loading?: boolean
  onCancel: () => void
  onSubmit: (values: ProcessStepPricingAdjustmentDTO) => Promise<void>
}

const MODE_NAMES: Record<number, string> = {
  1: '标准计价',
  2: '指定数量',
  3: '固定金额',
  4: '免收',
}

export default function ProcessStepPricingModal(props: Props) {
  const state = usePricingModalState(props)
  const { step } = props
  if (!step) return null
  return (
    <Modal
      open={props.open}
      title={<Space><CalculatorOutlined />计价核定</Space>}
      okText="保存核定"
      cancelText="取消"
      confirmLoading={props.loading}
      okButtonProps={{ disabled: state.isLoadingCatalog || !state.catalog }}
      styles={{ body: { maxHeight: 'calc(100vh - 200px)', overflowY: 'auto', paddingRight: 8 } }}
      width={640}
      centered
      destroyOnHidden
      onCancel={props.onCancel}
      onOk={() => state.form.submit()}
    >
      <PricingSummary step={step} catalog={state.catalog} preview={state.preview} />
      <PricingForm form={state.form} step={step} catalog={state.catalog} onFinish={state.finish} />
    </Modal>
  )
}

function usePricingModalState(props: Props) {
  const [form] = Form.useForm<FormValues>()
  const mode = Form.useWatch('billingMode', form)
  const billingQuantity = Form.useWatch('billingQuantity', form)
  const billingAmount = Form.useWatch('billingAmount', form)
  const billingBasis = Form.useWatch('billingBasis', form)
  const billingUnitPrice = Form.useWatch('billingUnitPrice', form)
  const { data: catalogs, isLoading: isLoadingCatalog } = useProcessCatalog()
  const catalog = findProcessCatalog(catalogs, props.step?.stepType)
  const preview = props.step
    ? pricingPreview({ step: props.step, originalRoll: props.originalRoll, mode,
        billingBasis, billingQuantity, billingAmount, billingUnitPrice })
    : { billingBasis: undefined, standardAmount: 0, finalAmount: 0, adjustment: 0 }
  const finish = async (values: FormValues) => {
    await props.onSubmit({
      billingMode: values.billingMode,
      billingQuantity: values.billingMode === 2 ? values.billingQuantity : undefined,
      billingAmount: values.billingMode === 3 ? values.billingAmount : undefined,
      billingBasis: isServiceStep(props.step) && values.billingMode === 1 ? values.billingBasis : undefined,
      billingUnitPrice: isServiceStep(props.step) && values.billingMode === 1 ? values.billingUnitPrice : undefined,
      reason: values.reason.trim(),
    })
    form.resetFields()
  }
  return { form, catalog, preview, isLoadingCatalog, finish }
}

function PricingSummary({ step, catalog, preview }: {
  step: ProcessStep
  catalog?: ProcessCatalog
  preview: PricingPreview
}) {
  const color = preview.adjustment < 0 ? 'gold' : preview.adjustment > 0 ? 'blue' : 'default'
  return (
    <Descriptions size="small" column={{ xs: 1, sm: 2 }} bordered>
      <Descriptions.Item label="工序">{step.stepName || catalog?.name || `工序 ${step.stepType}`}</Descriptions.Item>
      <Descriptions.Item label="计费数量">{quantityText(preview.quantity, catalog, preview.billingBasis)}</Descriptions.Item>
      <Descriptions.Item label="标准金额">{formatMoney(preview.standardAmount)}</Descriptions.Item>
      <Descriptions.Item label="核定金额">
        <Space size={6} style={{ whiteSpace: 'nowrap' }}>
          <strong>{formatMoney(preview.finalAmount)}</strong>
          <Tag color={color}>调整 {formatMoney(preview.adjustment)}</Tag>
        </Space>
      </Descriptions.Item>
    </Descriptions>
  )
}

function PricingForm({ form, step, catalog, onFinish }: {
  form: FormInstance<FormValues>
  step: ProcessStep
  catalog?: ProcessCatalog
  onFinish: (values: FormValues) => Promise<void>
}) {
  const mode = Form.useWatch('billingMode', form)
  const billingBasis = Form.useWatch('billingBasis', form)
  const unit = quantityUnit(catalog, step.billingBasis)
  const integerUnit = unit.code === 'KNIFE' || unit.code === 'PIECE'
  return (
    <Form form={form} layout="vertical" initialValues={initialValues(step)} onFinish={onFinish} style={{ marginTop: 16 }}>
      <Form.Item name="billingMode" label="计价模式" rules={[{ required: true, message: '请选择计价模式' }]}>
        <Select loading={!catalog} disabled={!catalog} options={catalog?.billingModes.map((value) => ({ value, label: MODE_NAMES[value] ?? `模式 ${value}` }))} />
      </Form.Item>
      {mode === 2 && (
        <Form.Item name="billingQuantity" label={`最终计费数量（${unit.name}）`} rules={positiveQuantityRules}>
          <InputNumber min={integerUnit ? 1 : 0.001} precision={integerUnit ? 0 : 3} style={{ width: '100%' }} />
        </Form.Item>
      )}
      {mode === 1 && isServiceStep(step) && (
        <ProcessStepPricingServiceFields catalog={catalog} billingBasis={billingBasis} />
      )}
      {mode === 3 && <FixedPricingField />}
      <Form.Item name="reason" label="调整原因" rules={[{ required: true, whitespace: true, max: 255, message: '请填写调整原因（最多255字）' }]}>
        <Input.TextArea rows={3} maxLength={255} showCount />
      </Form.Item>
    </Form>
  )
}

function FixedPricingField() {
  return (
    <Form.Item label="最终固定金额" required>
      <Space.Compact block>
        <Form.Item name="billingAmount" noStyle rules={[{ required: true, message: '请输入固定金额' }, { type: 'number', min: 0, message: '金额不能为负数' }]}>
          <InputNumber aria-label="固定结算金额" min={0} precision={2} style={{ width: '100%' }} />
        </Form.Item>
        <Input aria-label="金额单位" readOnly tabIndex={-1} value="元" style={{ width: 56 }} />
      </Space.Compact>
    </Form.Item>
  )
}

const positiveQuantityRules = [
  { required: true, message: '请输入最终计费数量' },
  { type: 'number' as const, min: 0.001, message: '数量必须大于0' },
]

function initialValues(step: ProcessStep): FormValues {
  return {
    billingMode: step.billingMode ?? 1,
    billingQuantity: step.billingQuantity,
    billingAmount: step.billingAmount,
    billingBasis: step.billingBasis as 'PIECE' | 'TON' | undefined,
    billingUnitPrice: step.billingUnitPrice ?? step.unitPrice,
    reason: step.pricingAdjustmentReason ?? '',
  }
}

function quantityText(quantity: number | undefined, catalog?: ProcessCatalog, basis?: string): string {
  if (quantity == null) return '-'
  const unit = quantityUnit(catalog, basis)
  const precision = unit.code === 'TON' ? 3 : 0
  return `${formatNumber(quantity, precision)} ${unit.name}`
}

function quantityUnit(catalog?: ProcessCatalog, basis?: string) {
  return catalog?.units.find((unit) => unit.code === basis)
    ?? catalog?.units.find((unit) => unit.defaultUnit)
    ?? catalog?.units[0]
    ?? { code: '', name: '单位', defaultUnit: true }
}
