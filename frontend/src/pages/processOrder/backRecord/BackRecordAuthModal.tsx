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
        className="back-record-auth-alert"
        showIcon
        type="error"
        message="重量偏差超过 5%，必须由管理员验证身份后放行。"
      />
      <Form form={form} layout="vertical">
        <Form.Item name="releaseAdminUsername" label="管理员账号" rules={[{ required: true, message: '管理员账号必填' }]}>
          <Input autoComplete="username" placeholder="请输入管理员登录账号" />
        </Form.Item>
        <Form.Item name="releaseAdminPassword" label="管理员密码" rules={[{ required: true, message: '管理员密码必填' }]}>
          <Input.Password autoComplete="current-password" placeholder="请输入管理员登录密码" />
        </Form.Item>
        <Form.Item name="releaseReason" label="放行原因" rules={[{ required: true, message: '放行原因必填' }]}>
          <Input.TextArea rows={3} placeholder="请说明差异原因和放行依据" />
        </Form.Item>
      </Form>
    </Modal>
  )
}
