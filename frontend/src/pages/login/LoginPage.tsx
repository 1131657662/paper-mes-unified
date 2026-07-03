import { useState } from 'react'
import { Button, Form, Input } from 'antd'
import { CheckCircleOutlined, LockOutlined, LoginOutlined, SafetyCertificateOutlined, UserOutlined } from '@ant-design/icons'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { login } from '../../api/auth'
import { useAuthActions, useAuthUser } from '../../stores/authStore'
import { LoginHero } from './LoginHero'
import './LoginPage.css'

interface LoginForm {
  password: string
  username: string
}

export default function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const user = useAuthUser()
  const { signIn } = useAuthActions()
  const [submitting, setSubmitting] = useState(false)
  const redirectPath = getRedirectPath(location.state, location.search)

  if (user) return <Navigate to={redirectPath} replace />

  const handleFinish = async (values: LoginForm) => {
    setSubmitting(true)
    try {
      const authUser = await login(values)
      signIn(authUser, authUser.permissions)
      navigate(redirectPath, { replace: true })
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="login-page">
      <main className="login-page__shell">
        <LoginHero />
        <LoginFormPanel onFinish={handleFinish} submitting={submitting} />
      </main>
    </div>
  )
}

function LoginFormPanel({ onFinish, submitting }: { onFinish: (values: LoginForm) => Promise<void>; submitting: boolean }) {
  return (
    <section className="login-page__form-panel" aria-label="登录表单">
      <div className="login-card">
        <div className="login-card__title">
          <span className="login-card__badge">
            <SafetyCertificateOutlined />
            安全登录
          </span>
          <h2>登录生产系统</h2>
          <p>使用系统账号进入工作台</p>
        </div>

        <Form<LoginForm> layout="vertical" onFinish={onFinish}>
          <Form.Item name="username" label="用户名" rules={[{ required: true, message: '请输入用户名' }]}>
            <Input autoComplete="username" prefix={<UserOutlined />} placeholder="请输入用户名" size="large" />
          </Form.Item>
          <Form.Item name="password" label="密码" rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password autoComplete="current-password" prefix={<LockOutlined />} placeholder="请输入密码" size="large" />
          </Form.Item>
          <Button block type="primary" htmlType="submit" loading={submitting} size="large" icon={<LoginOutlined />}>
            登录
          </Button>
        </Form>

        <div className="login-card__tip">
          <CheckCircleOutlined />
          <span>默认管理员账号：admin / admin123</span>
        </div>
      </div>
    </section>
  )
}

function getRedirectPath(state: unknown, search: string) {
  const params = new URLSearchParams(search)
  const queryFrom = params.get('from')
  if (isSafeRedirect(queryFrom)) return queryFrom
  if (!state || typeof state !== 'object' || !('from' in state)) return '/process-orders'
  const from = state.from
  if (isSafeRedirect(from)) return from
  return '/process-orders'
}

function isSafeRedirect(from: unknown): from is string {
  return typeof from === 'string' && from.startsWith('/') && !from.startsWith('//') && from !== '/login'
}
