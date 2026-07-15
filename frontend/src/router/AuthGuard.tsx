import { Spin } from 'antd'
import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useCurrentUser } from '../features/auth/hooks/useCurrentUser'
import { useAuthUser } from '../stores/authStore'

export default function AuthGuard() {
  const user = useAuthUser()
  const location = useLocation()
  const { isError: isSessionInvalid, isPending: isCheckingSession } = useCurrentUser(Boolean(user))

  if (user && isCheckingSession) {
    return (
      <div className="app-shell__auth-loading">
        <Spin />
        <span className="app-shell__auth-loading-text">正在恢复登录状态</span>
      </div>
    )
  }

  if (!user || isSessionInvalid) {
    return <Navigate to="/login" replace state={{ from: `${location.pathname}${location.search}` }} />
  }

  return <Outlet />
}
