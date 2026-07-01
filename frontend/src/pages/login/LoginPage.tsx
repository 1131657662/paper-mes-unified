import { useState } from 'react'
import type { ReactNode } from 'react'
import { Button, Card, Form, Input } from 'antd'
import {
  AuditOutlined,
  ClusterOutlined,
  DeploymentUnitOutlined,
  FieldTimeOutlined,
  FileDoneOutlined,
  LockOutlined,
  SafetyCertificateOutlined,
  ToolOutlined,
  UserOutlined,
} from '@ant-design/icons'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { login } from '../../api/auth'
import { useAuthActions, useAuthUser } from '../../stores/authStore'
import './LoginPage.css'

interface LoginForm {
  password: string
  username: string
}

interface ProductionStat {
  icon: ReactNode
  label: string
  value: string
}

const productionStats: ProductionStat[] = [
  { icon: <FileDoneOutlined />, label: '待下发', value: '工单核对' },
  { icon: <ToolOutlined />, label: '加工中', value: '机台排产' },
  { icon: <AuditOutlined />, label: '待回录', value: '产量复核' },
]

const flowItems = ['开单', '下发', '回录', '出库', '结算']

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
        <section className="login-page__intro" aria-label="系统介绍">
          <div className="login-page__brand">
            <span className="login-page__brand-icon"><DeploymentUnitOutlined /></span>
            <div>
              <h1>纸品加工 MES</h1>
              <p>生产开单、复卷锯纸、出库结算一体化工作台</p>
            </div>
          </div>

          <div className="login-page__production-card">
            <div className="login-page__panel-head">
              <span><ClusterOutlined /> 生产加工看板</span>
              <em>Paper Processing</em>
            </div>
            <div className="login-page__stat-grid">
              {productionStats.map((item) => (
                <div className="login-page__stat" key={item.label}>
                  <span>{item.icon}</span>
                  <strong>{item.label}</strong>
                  <small>{item.value}</small>
                </div>
              ))}
            </div>
            <div className="login-page__flow">
              {flowItems.map((item) => (
                <span key={item}>{item}</span>
              ))}
            </div>
            <div className="login-page__hint">
              <FieldTimeOutlined />
              <span>按工单流转、机台产出、卷号追踪和加工应收组织日常生产。</span>
            </div>
          </div>
        </section>

        <Card className="login-card">
          <div className="login-card__title">
            <span className="login-card__badge"><SafetyCertificateOutlined /> 安全登录</span>
            <h2>登录生产系统</h2>
            <p>请使用系统账号进入工作台</p>
          </div>
          <Form<LoginForm> layout="vertical" onFinish={handleFinish}>
            <Form.Item name="username" label="用户名" rules={[{ required: true, message: '请输入用户名' }]}>
              <Input prefix={<UserOutlined />} placeholder="请输入用户名" size="large" />
            </Form.Item>
            <Form.Item name="password" label="密码" rules={[{ required: true, message: '请输入密码' }]}>
              <Input.Password prefix={<LockOutlined />} placeholder="请输入密码" size="large" />
            </Form.Item>
            <Button block type="primary" htmlType="submit" loading={submitting} size="large">
              登录
            </Button>
          </Form>
          <div className="login-card__tip">默认管理员账号：admin / admin123</div>
        </Card>
      </main>
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
