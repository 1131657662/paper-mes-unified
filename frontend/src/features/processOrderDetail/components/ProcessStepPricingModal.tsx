import { CalculatorOutlined } from '@ant-design/icons'
import { Alert, Descriptions, Form, Input, InputNumber, Modal, Select, Space, Tag } from 'antd'
import type { ProcessStepPricingAdjustmentDTO } from '../../../api/processOrder'
import type { ProcessStep } from '../../../types/processOrder'
import { formatMoney, formatNumber } from '../orderDetailUtils'

type FormValues = ProcessStepPricingAdjustmentDTO

interface Props {
  open: boolean
  step: ProcessStep | null
  loading?: boolean
  onCancel: () => void
  onSubmit: (values: ProcessStepPricingAdjustmentDTO) => Promise<void>
}

const MODE_OPTIONS = [
  { value: 1, label: '标准计价' },
  { value: 2, label: '按指定数量计价' },
  { value: 3, label: '固定金额' },
  { value: 4, label: '免收' },
]

export default function ProcessStepPricingModal({ open, step, loading, onCancel, onSubmit }: Props) {
  const [form] = Form.useForm<FormValues>()
  const mode = Form.useWatch('billingMode', form)
  const billingQuantity = Form.useWatch('billingQuantity', form)
  const billingAmount = Form.useWatch('billingAmount', form)
  if (!step) return null

  const standardAmount = step.standardStepAmount ?? step.stepAmount ?? 0
  const finalAmount = mode === 3
    ? (billingAmount ?? 0)
    : mode === 4
      ? 0
      : mode === 2
        ? estimateQuantityAmount(step, billingQuantity)
        : step.stepAmount ?? standardAmount
  const adjustment = finalAmount - standardAmount

  const initialValues: FormValues = {
    billingMode: step.billingMode ?? 1,
    billingQuantity: step.billingQuantity,
    billingAmount: step.billingAmount,
    reason: step.pricingAdjustmentReason ?? '',
  }

  const handleFinish = async (values: FormValues) => {
    await onSubmit({
      billingMode: values.billingMode,
      billingQuantity: values.billingMode === 2 ? values.billingQuantity : undefined,
      billingAmount: values.billingMode === 3 ? values.billingAmount : undefined,
      reason: values.reason.trim(),
    })
    form.resetFields()
  }

  return (
    <Modal
      open={open}
      title={<Space><CalculatorOutlined />计价核定</Space>}
      okText="保存核定"
      cancelText="取消"
      confirmLoading={loading}
      destroyOnClose
      onCancel={onCancel}
      onOk={() => form.submit()}
    >
      <Alert
        type="info"
        showIcon
        message={`${step.stepName || (step.stepType === 2 ? '复卷工序' : '锯纸工序')} · ${step.billingMode && step.billingMode !== 1 ? '当前为特殊计价' : '当前为标准计价'}`}
        description="核定结果会写入加工单，并在月结客户生成结算单前冻结；加工工序金额按元四舍五入，超过系统免审额度的负向优惠需要财务或管理员授权。"
        style={{ marginBottom: 16 }}
      />
      <Descriptions size="small" column={2} bordered>
        <Descriptions.Item label="标准数量">{quantityText(step.standardQuantity, step)}</Descriptions.Item>
        <Descriptions.Item label="标准金额">{formatMoney(standardAmount)}</Descriptions.Item>
        <Descriptions.Item label="预计最终金额">
          <Space size={6}><strong>{formatMoney(finalAmount)}</strong><Tag color={adjustment < 0 ? 'gold' : adjustment > 0 ? 'blue' : 'default'}>调整 {formatMoney(adjustment)}</Tag></Space>
        </Descriptions.Item>
      </Descriptions>
      <Form form={form} layout="vertical" initialValues={initialValues} onFinish={handleFinish} style={{ marginTop: 16 }}>
        <Form.Item name="billingMode" label="计价模式" rules={[{ required: true, message: '请选择计价模式' }]}>
          <Select options={MODE_OPTIONS} />
        </Form.Item>
        {mode === 2 && <Form.Item name="billingQuantity" label={`最终计费数量（${step.stepType === 2 ? '吨' : '刀'}）`} rules={[{ required: true, message: '请输入最终计费数量' }, { type: 'number', min: 0.001, message: '数量必须大于0' }]}>
          <InputNumber min={0.001} precision={step.stepType === 2 ? 3 : 0} style={{ width: '100%' }} />
        </Form.Item>}
        {mode === 3 && <Form.Item label="最终固定金额" required>
          <Space.Compact block>
            <Form.Item name="billingAmount" noStyle rules={[{ required: true, message: '请输入固定金额' }, { type: 'number', min: 0, message: '金额不能为负数' }]}>
              <InputNumber aria-label="固定结算金额" min={0} precision={0} style={{ width: '100%' }} />
            </Form.Item>
            <Input aria-label="金额单位" readOnly tabIndex={-1} value="元" style={{ width: 56 }} />
          </Space.Compact>
        </Form.Item>}
        <Form.Item name="reason" label="调整原因" rules={[{ required: true, whitespace: true, max: 255, message: '请填写调整原因（最多255字）' }]}>
          <Input.TextArea rows={3} maxLength={255} showCount placeholder="例如：客户仅加工20米，双方确认按1吨计费" />
        </Form.Item>
      </Form>
    </Modal>
  )
}

function quantityText(quantity: number | undefined, step: ProcessStep): string {
  if (quantity == null) return '-'
  return `${formatNumber(quantity, step.stepType === 2 ? 3 : 0)} ${step.stepType === 2 ? 't' : '刀'}`
}

function estimateQuantityAmount(step: ProcessStep, quantity?: number): number {
  if (quantity == null || step.unitPrice == null) return step.stepAmount ?? 0
  return Math.round(quantity * step.unitPrice)
}
