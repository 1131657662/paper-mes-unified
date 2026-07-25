import { Form, Input, InputNumber, Modal, Select } from 'antd'
import type { NoRule, NoRuleSaveDTO } from '../../types/systemConfig'
import { statusOptions } from './systemConfigDisplay'

interface NoRuleModalProps {
  item?: NoRule
  open: boolean
  submitting: boolean
  onCancel: () => void
  onDirtyChange?: (dirty: boolean) => void
  onSubmit: (values: NoRuleSaveDTO) => Promise<void>
}

export default function NoRuleModal(props: NoRuleModalProps) {
  const [form] = Form.useForm<NoRuleSaveDTO>()
  const coreRule = CORE_NO_RULE_TYPES.has(props.item?.bizType ?? '')
  return (
    <Modal
      title="编辑单号规则"
      open={props.open}
      width={720}
      destroyOnHidden
      confirmLoading={props.submitting}
      onCancel={props.onCancel}
      onOk={() => form.submit()}
    >
      <Form className="mes-modal-form" form={form} initialValues={props.item ? toValues(props.item) : undefined} layout="vertical" onFieldsChange={() => props.onDirtyChange?.(form.isFieldsTouched())} onFinish={props.onSubmit}>
        <div className="mes-form-grid">
          <Form.Item name="bizType" label="业务类型" rules={[{ required: true, message: '业务类型不能为空' }]}>
            <Input disabled />
          </Form.Item>
          <Form.Item name="ruleName" label="规则名称" rules={[{ required: true, message: '请输入规则名称' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="prefix" label="前缀" rules={[{ required: true, message: '请输入前缀' }]}>
            <Input placeholder="如 JG、CK、JS、A" />
          </Form.Item>
          <Form.Item name="patternType" label="格式" rules={[{ required: true, message: '请选择格式' }]}>
            <Select options={patternOptions} />
          </Form.Item>
          <Form.Item name="datePattern" label="日期格式">
            <Select options={datePatternOptions} />
          </Form.Item>
          <Form.Item name="serialLength" label="流水位数" rules={[{ required: true, message: '请输入流水位数' }]}>
            <InputNumber min={3} max={10} />
          </Form.Item>
          <Form.Item name="resetCycle" label="重置周期" rules={[{ required: true, message: '请选择重置周期' }]}>
            <Select options={resetOptions} />
          </Form.Item>
          <Form.Item name="status" label="状态" rules={[{ required: true, message: '请选择状态' }]}>
            <Select disabled={coreRule} options={statusOptions} />
          </Form.Item>
          <Form.Item className="mes-form-grid__full" name="remark" label="备注">
            <Input.TextArea rows={3} placeholder="说明该单号规则适用的业务场景" />
          </Form.Item>
        </div>
      </Form>
    </Modal>
  )
}

function toValues(item: NoRule): NoRuleSaveDTO {
  return {
    bizType: item.bizType,
    datePattern: item.datePattern || 'yyyyMMdd',
    patternType: item.patternType,
    prefix: item.prefix,
    remark: item.remark,
    resetCycle: item.resetCycle,
    ruleName: item.ruleName,
    serialLength: item.serialLength,
    status: item.status,
  }
}

const CORE_NO_RULE_TYPES = new Set([
  'process_order',
  'delivery_order',
  'settle_order',
  'finish_roll',
  'customer',
  'paper',
  'machine',
  'warehouse',
])

const patternOptions = [
  { label: '前缀 + 日期 + 序号', value: 1 },
  { label: '前缀 + 序号', value: 2 },
]

const datePatternOptions = [
  { label: 'yyyyMMdd', value: 'yyyyMMdd' },
  { label: 'yyyyMM', value: 'yyyyMM' },
  { label: 'yyyy', value: 'yyyy' },
]

const resetOptions = [
  { label: '不重置', value: 0 },
  { label: '按日', value: 1 },
  { label: '按月', value: 2 },
  { label: '按年', value: 3 },
]
