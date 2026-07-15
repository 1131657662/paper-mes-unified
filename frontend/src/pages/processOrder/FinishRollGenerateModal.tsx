import { useState } from 'react'
import { Form, Input, InputNumber, Modal, Select } from 'antd'
import type { FinishRollBatchDTO } from '../../types/processOrder'

interface Props {
  onCancel: () => void
  onSubmit: (values: FinishRollBatchDTO) => Promise<void>
  open: boolean
  sourceOptions: Array<{ label: string; value: string }>
}

export default function FinishRollGenerateModal({ onCancel, onSubmit, open, sourceOptions }: Props) {
  const [form] = Form.useForm<FinishRollBatchDTO>()
  const [submitting, setSubmitting] = useState(false)
  const submit = async () => {
    const values = await form.validateFields()
    setSubmitting(true)
    try {
      await onSubmit(values)
      form.resetFields()
    } finally {
      setSubmitting(false)
    }
  }
  return (
    <Modal cancelText="取消" confirmLoading={submitting} destroyOnHidden okText="生成" open={open} title="批量生成正式卷号" onCancel={onCancel} onOk={submit}>
      <Form className="mes-modal-form" form={form} layout="vertical" initialValues={sourceOptions.length === 1 ? { originalUuid: sourceOptions[0]?.value } : undefined}>
        <div className="mes-form-grid">
          <Form.Item className="mes-form-grid__full" name="originalUuid" label="来源母卷" rules={[{ required: true, message: '请选择来源母卷' }]}>
            <Select options={sourceOptions} placeholder="请选择实际来源母卷" />
          </Form.Item>
          <Form.Item name="count" label="生成数量" rules={[{ required: true, message: '请输入生成数量' }, { type: 'number', min: 1, max: 500, message: '数量范围为 1-500' }]}>
            <InputNumber min={1} max={500} />
          </Form.Item>
          <Form.Item name="paperName" label="成品品名"><Input placeholder="可留空，回录时补齐" /></Form.Item>
          <Form.Item name="gramWeight" label="克重 (g)"><InputNumber min={1} placeholder="可留空" /></Form.Item>
          <Form.Item name="finishWidth" label="门幅 (mm)"><InputNumber min={1} placeholder="可留空" /></Form.Item>
          <Form.Item name="finishDiameter" label="直径 (英寸)"><InputNumber min={1} /></Form.Item>
          <Form.Item name="finishCoreDiameter" label="纸芯直径 (英寸)"><InputNumber min={1} /></Form.Item>
          <Form.Item className="mes-form-grid__full" name="remark" label="备注"><Input.TextArea rows={2} /></Form.Item>
        </div>
      </Form>
    </Modal>
  )
}
