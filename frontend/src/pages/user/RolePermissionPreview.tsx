import { CheckCircleFilled, MinusCircleOutlined } from '@ant-design/icons'
import { Card, Tag } from 'antd'
import { PERMISSION_GROUPS, PERMISSION_ITEMS, getRoleProfile, getRolePermissions, roleHasPermission } from '../../constants/permissionMeta'
import type { UserRoleCode } from '../../types/user'

interface Props {
  roleCode?: UserRoleCode
  compact?: boolean
}

export default function RolePermissionPreview({ compact, roleCode }: Props) {
  const profile = getRoleProfile(roleCode)
  const permissions = getRolePermissions(roleCode)

  if (!profile) {
    return (
      <Card className="document-module-card user-permission-preview" title="角色权限">
        <div className="user-permission-empty">请选择系统角色后查看权限范围</div>
      </Card>
    )
  }

  return (
    <Card
      className={compact ? 'document-module-card user-permission-preview user-permission-preview--compact' : 'document-module-card user-permission-preview'}
      title="角色权限"
      extra={<Tag color={profile.tone}>{profile.summary}</Tag>}
    >
      <div className="user-role-profile">
        <strong>{profile.label}</strong>
        <p>{profile.description}</p>
      </div>

      <div className="user-permission-groups">
        {PERMISSION_GROUPS.map((group) => {
          const enabledCount = group.permissions.filter((code) => roleHasPermission(roleCode, code)).length
          return (
            <div className="user-permission-group" key={group.key}>
              <div className="user-permission-group__head">
                <div>
                  <strong>{group.title}</strong>
                  <span>{group.description}</span>
                </div>
                <Tag color={enabledCount > 0 ? 'processing' : 'default'}>
                  {enabledCount}/{group.permissions.length}
                </Tag>
              </div>
              {!compact && (
                <div className="user-permission-items">
                  {group.permissions.map((code) => (
                    <PermissionPoint enabled={permissions.includes(code)} key={code} code={code} />
                  ))}
                </div>
              )}
            </div>
          )
        })}
      </div>
    </Card>
  )
}

function PermissionPoint({ code, enabled }: { code: string; enabled: boolean }) {
  const item = PERMISSION_ITEMS.find((permission) => permission.code === code)
  return (
    <div className={enabled ? 'user-permission-point is-enabled' : 'user-permission-point'}>
      {enabled ? <CheckCircleFilled /> : <MinusCircleOutlined />}
      <div>
        <strong>{item?.label ?? code}</strong>
        <span>{item?.description ?? code}</span>
      </div>
    </div>
  )
}
