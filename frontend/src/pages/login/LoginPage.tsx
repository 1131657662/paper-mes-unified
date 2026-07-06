import { useState } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { login } from '../../api/auth'
import { useAuthActions, useAuthUser } from '../../stores/authStore'
import { useDocumentTitle } from '../../hooks/useDocumentTitle'
import { LoginFormPanel, type LoginFormValues } from './LoginFormPanel'
import { LoginHero } from './LoginHero'
import './LoginPage.css'

export default function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const user = useAuthUser()
  const { signIn } = useAuthActions()
  const [submitting, setSubmitting] = useState(false)
  const redirectPath = getRedirectPath(location.state, location.search)
  useDocumentTitle('登录')

  if (user) return <Navigate to={redirectPath} replace />

  const handleFinish = async (values: LoginFormValues) => {
    setSubmitting(true)
    try {
      const authUser = await login({
        password: values.password,
        username: values.username.trim(),
      })
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

function getRedirectPath(state: unknown, search: string): string {
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
