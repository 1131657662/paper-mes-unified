import { Tag } from 'antd'
import type { UserRoleCode, UserStatus } from '../../types/user'
import { getRoleProfile } from '../../constants/permissionMeta'

export const userRoleOptions: Array<{ label: string; value: UserRoleCode }> = [
  { value: 'admin', label: '管理员' },
  { value: 'operator', label: '录单员' },
  { value: 'finance', label: '财务' },
  { value: 'warehouse', label: '仓库' },
]

export const userStatusOptions: Array<{ label: string; value: UserStatus }> = [
  { value: 1, label: '启用' },
  { value: 0, label: '停用' },
]

export function roleText(roleCode?: string) {
  return getRoleProfile(roleCode)?.label ?? '未知角色'
}

export function statusText(status?: number) {
  if (status === 1) return '启用'
  if (status === 0) return '停用'
  return '-'
}

export function roleTag(roleCode?: string) {
  const color = getRoleProfile(roleCode)?.tone ?? 'default'
  return <Tag className="mes-data-tag" color={color}>{roleText(roleCode)}</Tag>
}

export function statusTag(status?: number) {
  return (
    <Tag className="mes-data-tag" color={status === 1 ? 'green' : 'default'}>
      {statusText(status)}
    </Tag>
  )
}
