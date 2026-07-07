import { Form, Input, Modal } from 'antd'
import { strongPasswordRules } from '../../constants/passwordRules'
import type { UserPasswordDTO } from '../../types/user'

interface Props {
  open: boolean
  submitting: boolean
  userName?: string
  onCancel: () => void
  onSubmit: (values: UserPasswordDTO) => Promise<void>
}

export default function UserPasswordModal({ onCancel, onSubmit, open, submitting, userName }: Props) {
  const [form] = Form.useForm<UserPasswordDTO>()

  return (
    <Modal
      title="重置密码"
      open={open}
      confirmLoading={submitting}
      onCancel={onCancel}
      onOk={() => form.submit()}
      okText="确认重置"
      cancelText="取消"
      destroyOnHidden
      width={480}
      afterClose={() => form.resetFields()}
    >
      <p className="document-action-warning">
        将为 {userName || '该用户'} 设置新的登录密码。原密码不会显示，也不会被系统明文保存。
      </p>
      <Form className="mes-modal-form user-password-form" form={form} layout="vertical" onFinish={onSubmit}>
        <Form.Item
          name="password"
          label="新密码"
          rules={strongPasswordRules('请输入新密码')}
        >
          <Input.Password placeholder="请输入新密码" />
        </Form.Item>
      </Form>
    </Modal>
  )
}
