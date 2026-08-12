import { Form, Input, Modal } from 'antd'
import type { ProcessOrder } from '../../../types/processOrder'

interface FormValues { postProductionNote?: string }

interface Props {
  loading?: boolean
  open: boolean
  order?: ProcessOrder
  onCancel: () => void
  onSubmit: (values: FormValues) => Promise<void>
}

export default function PostProductionNoteModal({ loading, open, order, onCancel, onSubmit }: Props) {
  const [form] = Form.useForm<FormValues>()
  return (
    <Modal destroyOnHidden title="编辑后生产备注" open={open} confirmLoading={loading}
      onCancel={onCancel} onOk={() => form.submit()}>
      <Form form={form} layout="vertical" initialValues={{ postProductionNote: order?.postProductionNote }} onFinish={onSubmit}>
        <Form.Item name="postProductionNote" label="后生产备注" rules={[{ max: 2000, message: '后生产备注不能超过2000个字符' }]}>
          <Input.TextArea rows={5} placeholder="填写完工、出库或结算后的运营说明；不会修改已下发的生产指令" />
        </Form.Item>
      </Form>
    </Modal>
  )
}
