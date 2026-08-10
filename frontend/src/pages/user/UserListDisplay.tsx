import type { ReactNode } from 'react'
import { Space, Tag } from 'antd'
import {
  getRoleModuleNames,
  getRoleProfile,
} from '../../constants/permissionMeta'
import type { UserRoleCode } from '../../types/user'
import { roleTag, roleText } from './userDisplay'

export function UserRoleSummary({
  roleCode,
}: {
  roleCode?: string
}): ReactNode {
  return (
    <Space size={6}>
      {roleTag(roleCode)}
      <Tag className="mes-data-tag">{roleText(roleCode)}</Tag>
    </Space>
  )
}

export function UserRoleCell({
  roleCode,
}: {
  roleCode?: UserRoleCode
}): ReactNode {
  const profile = getRoleProfile(roleCode)
  return (
    <div className="user-role-cell">
      {roleTag(roleCode)}
      <span>{profile?.summary ?? roleText(roleCode)}</span>
    </div>
  )
}

export function UserRoleScope({
  roleCode,
}: {
  roleCode?: UserRoleCode
}): ReactNode {
  const names = getRoleModuleNames(roleCode)
  if (!names.length) return '-'
  return (
    <div className="user-role-scope">
      {names.map((name) => (
        <Tag key={name}>{name}</Tag>
      ))}
    </div>
  )
}
