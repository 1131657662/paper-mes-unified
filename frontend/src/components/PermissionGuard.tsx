import type { ReactNode } from 'react'
import { Button, Result, Space, Tag } from 'antd'
import { useNavigate } from 'react-router-dom'
import { getPermissionLabel, getRoleProfile } from '../constants/permissionMeta'
import { useAuthUser, useHasAnyPermission } from '../stores/authStore'

interface Props {
  children: ReactNode
  permissions: string[]
}

export default function PermissionGuard({ children, permissions }: Props) {
  const allowed = useHasAnyPermission(permissions)
  const navigate = useNavigate()
  const user = useAuthUser()
  const role = getRoleProfile(user?.roleCode)

  if (allowed) return <>{children}</>

  return (
    <Result
      status="403"
      title="无权限访问"
      subTitle="当前账号没有访问该页面的权限，请联系管理员调整角色或权限。"
      extra={(
        <Space direction="vertical" size={12}>
          <Space wrap>
            <Tag color={role?.tone ?? 'default'}>当前角色：{role?.label ?? '未知角色'}</Tag>
            {permissions.map((permission) => (
              <Tag key={permission}>需要：{getPermissionLabel(permission)}</Tag>
            ))}
          </Space>
          <Button type="primary" onClick={() => navigate('/dashboard')}>返回仪表盘</Button>
        </Space>
      )}
    />
  )
}
