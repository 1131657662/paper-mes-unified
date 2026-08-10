import { useEffect } from 'react'
import { Spin } from 'antd'
import { Navigate, Outlet, useLocation } from 'react-router'
import { useCurrentUser } from '../features/auth/hooks/useCurrentUser'
import { useAuthActions, useAuthUser } from '../stores/authStore'

export default function AuthGuard() {
  const user = useAuthUser()
  const { syncCurrentUser } = useAuthActions()
  const location = useLocation()
  const {
    data: currentUser,
    isError: isSessionInvalid,
    isPending: isCheckingSession,
  } = useCurrentUser(true)

  useEffect(() => {
    if (currentUser) syncCurrentUser(currentUser)
  }, [currentUser, syncCurrentUser])

  if (isCheckingSession) {
    return (
      <div className="app-shell__auth-loading">
        <Spin />
        <span className="app-shell__auth-loading-text">正在恢复登录状态</span>
      </div>
    )
  }

  if ((!user && !currentUser) || isSessionInvalid) {
    return <Navigate to="/login" replace state={{ from: `${location.pathname}${location.search}` }} />
  }

  return <Outlet />
}
