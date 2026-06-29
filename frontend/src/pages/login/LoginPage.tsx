import { useState } from 'react'
import { Button, Card, Form, Input } from 'antd'
import { LockOutlined, UserOutlined } from '@ant-design/icons'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { login } from '../../api/auth'
import { useAuthActions, useAuthUser } from '../../stores/authStore'
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
      <div className="login-page__intro">
        <h1>纸品加工 MES</h1>
        <p>加工单、出库、结算与报表的一体化生产工作台。</p>
      </div>
      <Card className="login-card">
        <div className="login-card__title">
          <h2>登录系统</h2>
          <p>默认管理员账号 admin / admin123</p>
        </div>
        <Form<LoginForm> layout="vertical" onFinish={handleFinish}>
          <Form.Item name="username" label="用户名" rules={[{ required: true, message: '请输入用户名' }]}>
            <Input prefix={<UserOutlined />} placeholder="请输入用户名" />
          </Form.Item>
          <Form.Item name="password" label="密码" rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password prefix={<LockOutlined />} placeholder="请输入密码" />
          </Form.Item>
          <Button block type="primary" htmlType="submit" loading={submitting}>
            登录
          </Button>
        </Form>
      </Card>
    </div>
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
