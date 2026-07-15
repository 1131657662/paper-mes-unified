import { useState } from 'react'
import { Form, InputNumber, Modal, Select } from 'antd'
import type { SpareRollAppendDTO } from '../../types/processOrder'

interface Props {
  onCancel: () => void
  onSubmit: (values: SpareRollAppendDTO) => Promise<void>
  open: boolean
  sourceOptions: Array<{ label: string; value: string }>
}

export default function FinishRollSpareModal({ onCancel, onSubmit, open, sourceOptions }: Props) {
  const [form] = Form.useForm<SpareRollAppendDTO>()
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
    <Modal cancelText="取消" confirmLoading={submitting} destroyOnHidden okText="追加" open={open} title="追加备用卷号" onCancel={onCancel} onOk={submit}>
      <Form className="mes-modal-form" form={form} layout="vertical" initialValues={sourceOptions.length === 1 ? { originalUuid: sourceOptions[0]?.value } : undefined}>
        <Form.Item name="originalUuid" label="来源母卷" rules={[{ required: true, message: '请选择来源母卷' }]}>
          <Select options={sourceOptions} placeholder="请选择备用号对应的母卷" />
        </Form.Item>
        <Form.Item name="count" label="追加数量" rules={[{ required: true, message: '请输入追加数量' }, { type: 'number', min: 1, max: 500, message: '数量范围为 1-500' }]}>
          <InputNumber min={1} max={500} />
        </Form.Item>
      </Form>
    </Modal>
  )
}
