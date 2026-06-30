import { Spin } from 'antd'
import { useEffect, useState } from 'react'
import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { getCurrentUser } from '../api/auth'
import { useAuthActions, useAuthUser } from '../stores/authStore'

export default function AuthGuard() {
  const user = useAuthUser()
  const { signOut, syncCurrentUser } = useAuthActions()
  const location = useLocation()
  const [checking, setChecking] = useState(Boolean(user?.accessToken))

  useEffect(() => {
    if (!user?.accessToken) {
      setChecking(false)
      return
    }
    let alive = true
    getCurrentUser()
      .then((currentUser) => {
        if (alive) syncCurrentUser(currentUser)
      })
      .catch(() => {
        if (alive) signOut()
      })
      .finally(() => {
        if (alive) setChecking(false)
      })
    return () => {
      alive = false
    }
  }, [signOut, syncCurrentUser, user?.accessToken])

  if (checking) {
    return (
      <div className="app-shell__auth-loading">
        <Spin />
        <span className="app-shell__auth-loading-text">正在恢复登录状态</span>
      </div>
    )
  }

  if (!user) {
    return <Navigate to="/login" replace state={{ from: `${location.pathname}${location.search}` }} />
  }

  return <Outlet />
}
