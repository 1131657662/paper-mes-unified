import { Alert, Form, Input, Modal } from 'antd'
import type { ProcessOrder, ProcessOrderRemarkDTO } from '../../../types/processOrder'

interface Props {
  loading?: boolean
  open: boolean
  order?: ProcessOrder
  onCancel: () => void
  onSubmit: (values: ProcessOrderRemarkDTO) => Promise<void>
}

export default function OrderRemarkModal({ loading, open, order, onCancel, onSubmit }: Props) {
  const [form] = Form.useForm<ProcessOrderRemarkDTO>()

  return (
    <Modal
      destroyOnHidden
      title="编辑主单备注"
      open={open}
      confirmLoading={loading}
      onCancel={onCancel}
      onOk={() => form.submit()}
    >
      {order?.printStatus === 1 && (
        <Alert
          showIcon
          type="warning"
          className="order-detail-note-alert"
          message="本单已打印，修改会影响打印备注，请确认是否需要重新打印。"
        />
      )}
      <Form
        form={form}
        layout="vertical"
        initialValues={{ remark: order?.remark, remarkLong: order?.remarkLong }}
        onFinish={onSubmit}
      >
        <Form.Item name="remark" label="备注" rules={[{ max: 255, message: '备注不能超过255个字符' }]}>
          <Input.TextArea rows={3} placeholder="填写生产现场需要特别注意的说明" />
        </Form.Item>
        <Form.Item name="remarkLong" label="详细备注" rules={[{ max: 2000, message: '详细备注不能超过2000个字符' }]}>
          <Input.TextArea rows={5} placeholder="可补充更完整的客户要求、标签说明、交付注意事项" />
        </Form.Item>
      </Form>
    </Modal>
  )
}
