import { Button, Form, Input } from 'antd'
import { LockOutlined, LoginOutlined, SafetyCertificateOutlined, UserOutlined } from '@ant-design/icons'
import { APP_BRAND } from '../../config/brand'
import './LoginFormPanel.css'

export interface LoginFormValues {
  password: string
  username: string
}

interface LoginFormPanelProps {
  onFinish: (values: LoginFormValues) => Promise<void>
  submitting: boolean
}

export function LoginFormPanel({ onFinish, submitting }: LoginFormPanelProps) {
  return (
    <section className="login-page__form-panel" aria-label="登录表单">
      <div className="login-card" aria-busy={submitting}>
        <LoginFormHeader />
        <LoginFields onFinish={onFinish} submitting={submitting} />
        <div className="login-card__support">忘记密码请联系系统管理员</div>
      </div>
    </section>
  )
}

function LoginFormHeader() {
  return (
    <div className="login-card__heading">
      <span className="login-card__eyebrow">
        <SafetyCertificateOutlined />
        受保护入口
      </span>
      <h2>欢迎回来</h2>
      <p>登录 {APP_BRAND.name} 工作台</p>
    </div>
  )
}

function LoginFields({ onFinish, submitting }: LoginFormPanelProps) {
  return (
    <Form<LoginFormValues>
      className="login-card__form"
      disabled={submitting}
      layout="vertical"
      name="paper-mes-login"
      onFinish={onFinish}
      requiredMark={false}
      validateTrigger="onBlur"
    >
      <Form.Item name="username" label="用户名" rules={[{ required: true, whitespace: true, message: '请输入用户名' }]}>
        <Input
          allowClear
          autoComplete="username"
          autoFocus
          maxLength={64}
          prefix={<UserOutlined />}
          placeholder="请输入用户名"
          size="large"
        />
      </Form.Item>
      <Form.Item name="password" label="密码" rules={[{ required: true, message: '请输入密码' }]}>
        <Input.Password
          autoComplete="current-password"
          maxLength={64}
          prefix={<LockOutlined />}
          placeholder="请输入密码"
          size="large"
        />
      </Form.Item>
      <Button block type="primary" htmlType="submit" loading={submitting} size="large" icon={<LoginOutlined />}>
        {submitting ? '登录中' : '登录'}
      </Button>
    </Form>
  )
}
