import { Form, InputNumber, Modal, Segmented } from 'antd'

export interface BatchSpecValues {
  finishWidth: number
  finishDiameter?: number
  finishCoreDiameter?: number
  target: 'missing' | 'all'
}

interface Props {
  onApply: (values: BatchSpecValues) => void
  onCancel: () => void
  open: boolean
  maxWidth?: number
}

export default function BackRecordBatchSpecModal({ onApply, onCancel, open, maxWidth }: Props) {
  const [form] = Form.useForm<BatchSpecValues>()
  const submit = async () => {
    const values = await form.validateFields()
    onApply(values)
    form.resetFields()
  }
  return (
    <Modal open={open} title="批量填写现场规格" okText="应用" cancelText="取消" destroyOnHidden onCancel={onCancel} onOk={submit}>
      <Form form={form} layout="vertical" initialValues={{ target: 'missing' }}>
        <Form.Item name="target" label="应用范围" rules={[{ required: true }]}>
          <Segmented block options={[{ label: '仅未填写正式卷', value: 'missing' }, { label: '覆盖全部正式卷', value: 'all' }]} />
        </Form.Item>
        <div className="mes-form-grid">
        <Form.Item name="finishWidth" label="成品门幅" rules={[{ required: true, message: '请输入成品门幅' }, { type: 'number', min: 1, max: maxWidth, message: maxWidth ? `门幅范围为 1-${maxWidth}mm` : '门幅必须大于0' }]}>
            <InputNumber min={1} max={maxWidth} precision={0} suffix="mm" />
          </Form.Item>
          <Form.Item name="finishDiameter" label="直径（可选）">
            <InputNumber min={1} precision={0} suffix="英寸" />
          </Form.Item>
          <Form.Item name="finishCoreDiameter" label="纸芯（可选）">
            <InputNumber min={1} precision={0} suffix="英寸" />
          </Form.Item>
        </div>
      </Form>
    </Modal>
  )
}
