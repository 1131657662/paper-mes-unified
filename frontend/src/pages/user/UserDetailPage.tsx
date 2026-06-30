import { useState } from 'react'
import { Button, Card, Descriptions, Empty, Skeleton, Space, Tag, message } from 'antd'
import { EditOutlined, KeyOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import { useNavigate, useParams } from 'react-router-dom'
import MesPageHeader from '../../components/layout/MesPageHeader'
import { useResetUserPassword } from '../../features/user/hooks/useUserMutations'
import { useUserDetail } from '../../features/user/hooks/useUserDetail'
import { getRoleProfile } from '../../constants/permissionMeta'
import RolePermissionPreview from './RolePermissionPreview'
import { roleText, statusTag, statusText } from './userDisplay'
import UserPasswordModal from './UserPasswordModal'
import '../documentModule.css'
import './UserProfile.css'

export default function UserDetailPage() {
  const { uuid } = useParams()
  const navigate = useNavigate()
  const [passwordOpen, setPasswordOpen] = useState(false)
  const { data: user, isLoading: isLoadingUser } = useUserDetail(uuid)
  const { mutateAsync: resetPassword, isPending: isResettingPassword } = useResetUserPassword()

  if (isLoadingUser) {
    return (
      <div className="document-module-page user-profile-page">
        <Skeleton active paragraph={{ rows: 8 }} />
      </div>
    )
  }

  if (!user) {
    return (
      <div className="document-module-page user-profile-page">
        <Empty description="用户不存在" />
      </div>
    )
  }

  return (
    <div className="document-module-page user-profile-page">
      <MesPageHeader
        title={user.realName}
        eyebrow="用户权限"
        description={`登录账号：${user.username} · 角色：${roleText(user.roleCode)} · 状态：${statusText(user.status)}`}
        onBack={() => navigate('/users')}
        tags={statusTag(user.status)}
        actions={(
          <Space>
            <Button icon={<KeyOutlined />} onClick={() => setPasswordOpen(true)}>
              重置密码
            </Button>
            <Button icon={<EditOutlined />} type="primary" onClick={() => navigate(`/users/${user.uuid}/edit`)}>
              编辑用户
            </Button>
          </Space>
        )}
      />

      <section className="user-detail-overview">
        <MetricCard label="系统角色" value={roleText(user.roleCode)} helper={getRoleProfile(user.roleCode)?.summary ?? '未配置权限说明'} />
        <MetricCard label="账号状态" value={statusText(user.status)} helper="停用后不能登录系统" />
        <MetricCard label="最近登录" value={dateText(user.lastLoginTime)} helper="用于判断账号活跃度" />
        <MetricCard label="最近更新" value={dateText(user.updateTime)} helper="权限资料维护时间" />
      </section>

      <div className="user-detail-grid">
        <Card className="document-module-card" title="账号信息">
          <Descriptions column={2} size="small">
            <Descriptions.Item label="登录账号">{text(user.username)}</Descriptions.Item>
            <Descriptions.Item label="姓名">{text(user.realName)}</Descriptions.Item>
            <Descriptions.Item label="系统角色">
              <Tag className="mes-data-tag" color={user.roleCode === 'admin' ? 'blue' : 'default'}>
                {roleText(user.roleCode)}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="账号状态">{statusText(user.status)}</Descriptions.Item>
          </Descriptions>
        </Card>

        <Card className="document-module-card" title="维护信息">
          <Descriptions column={2} size="small">
            <Descriptions.Item label="创建人">{text(user.createBy)}</Descriptions.Item>
            <Descriptions.Item label="更新人">{text(user.updateBy)}</Descriptions.Item>
            <Descriptions.Item label="创建时间">{dateText(user.createTime)}</Descriptions.Item>
            <Descriptions.Item label="更新时间">{dateText(user.updateTime)}</Descriptions.Item>
            <Descriptions.Item label="备注" span={2}>{text(user.remark)}</Descriptions.Item>
          </Descriptions>
        </Card>
      </div>

      <RolePermissionPreview roleCode={user.roleCode} />

      <UserPasswordModal
        open={passwordOpen}
        submitting={isResettingPassword}
        userName={`${user.realName}（${user.username}）`}
        onCancel={() => setPasswordOpen(false)}
        onSubmit={async (values) => {
          await resetPassword({ uuid: user.uuid, data: values })
          message.success('密码已重置')
          setPasswordOpen(false)
        }}
      />
    </div>
  )
}

function MetricCard({ helper, label, value }: { helper: string; label: string; value: string }) {
  return (
    <div className="user-detail-metric">
      <span>{label}</span>
      <strong>{value}</strong>
      <em>{helper}</em>
    </div>
  )
}

function dateText(value?: string) {
  if (!value) return '-'
  const date = dayjs(value)
  return date.isValid() ? date.format('YYYY-MM-DD HH:mm:ss') : value
}

function text(value?: string) {
  return value || '-'
}
