import { Alert, Form, Input, Modal } from 'antd'
import type { FormInstance } from 'antd'
import type { BackRecordAuthorization } from './backRecordUtils'

interface Props {
  open: boolean
  form: FormInstance<BackRecordAuthorization>
  onCancel: () => void
  onSubmit: () => void
}

export default function BackRecordAuthModal({ open, form, onCancel, onSubmit }: Props) {
  return (
    <Modal
      title="超差授权放行"
      open={open}
      onCancel={onCancel}
      onOk={onSubmit}
      okText="确认超差放行"
      cancelText="取消"
      destroyOnHidden
      forceRender
    >
      <Alert
        showIcon
        type="error"
        message="重量偏差超过 5%，需授权放行并写入操作日志。"
        style={{ marginBottom: 16 }}
      />
      <Form form={form} layout="vertical">
        <Form.Item name="operator" label="放行人" rules={[{ required: true, message: '放行人必填' }]}>
          <Input placeholder="放行人姓名" />
        </Form.Item>
        <Form.Item name="releaseReason" label="放行原因" rules={[{ required: true, message: '放行原因必填' }]}>
          <Input.TextArea rows={3} placeholder="请说明差异原因和放行依据" />
        </Form.Item>
      </Form>
    </Modal>
  )
}
