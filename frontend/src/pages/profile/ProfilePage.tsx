import type { ReactNode } from 'react'
import { Button, Card, Form, Input, Tag, message } from 'antd'
import { IdcardOutlined, KeyOutlined, LockOutlined, SafetyCertificateOutlined, UserOutlined } from '@ant-design/icons'
import MesPageHeader from '../../components/layout/MesPageHeader'
import GeneratedUserAvatar from '../../components/user/GeneratedUserAvatar'
import { getRoleModuleNames, getRoleProfile } from '../../constants/permissionMeta'
import { strongPasswordRules } from '../../constants/passwordRules'
import { useChangePassword } from '../../features/auth/hooks/useChangePassword'
import { useAuthUser } from '../../stores/authStore'
import type { AuthUser } from '../../types/auth'
import '../documentModule.css'
import './ProfilePage.css'

interface ProfilePasswordForm {
  confirmPassword: string
  newPassword: string
  oldPassword: string
}

export default function ProfilePage() {
  const user = useAuthUser()

  return (
    <div className="document-module-page profile-page">
      <MesPageHeader title="个人中心" description="查看当前登录身份，维护自己的账号安全。" />
      <div className="profile-page__content">
        <AccountInfoCard user={user} />
        <PasswordCard />
      </div>
    </div>
  )
}

function AccountInfoCard({ user }: { user: AuthUser | null }) {
  const profile = getRoleProfile(user?.roleCode)
  const modules = getRoleModuleNames(user?.roleCode)

  return (
    <Card className="document-module-card profile-account-card">
      <div className="profile-account-card__head">
        <GeneratedUserAvatar className="profile-account-card__avatar" size={58} user={user} />
        <div className="profile-account-card__identity">
          <span>当前账号</span>
          <strong>{user?.realName || user?.username || '-'}</strong>
          <Tag color={profile?.tone ?? 'default'}>{profile?.label ?? '访客'}</Tag>
        </div>
      </div>

      <div className="profile-info-list">
        <InfoRow icon={<UserOutlined />} label="登录账号" value={user?.username ?? '-'} />
        <InfoRow icon={<IdcardOutlined />} label="姓名" value={user?.realName ?? '-'} />
        <InfoRow icon={<SafetyCertificateOutlined />} label="系统角色" value={profile?.summary ?? profile?.label ?? '未配置'} />
      </div>

      <div className="profile-scope">
        <span>可访问模块</span>
        <div>
          {modules.length ? modules.map((moduleName) => <Tag key={moduleName}>{moduleName}</Tag>) : <Tag>暂无模块</Tag>}
        </div>
      </div>
    </Card>
  )
}

function PasswordCard() {
  const [form] = Form.useForm<ProfilePasswordForm>()
  const { mutateAsync: changePassword, isPending: isChangingPassword } = useChangePassword()

  const handleSubmit = async (values: ProfilePasswordForm) => {
    await changePassword({ newPassword: values.newPassword, oldPassword: values.oldPassword })
    message.success('密码已修改')
    form.resetFields()
  }

  return (
    <Card className="document-module-card profile-password-card" title="修改密码">
      <p className="profile-password-card__note">密码修改成功后，其他设备上的登录会失效。</p>
      <Form className="profile-password-form" form={form} layout="vertical" onFinish={handleSubmit} requiredMark={false}>
        <PasswordFields />
        <div className="profile-password-form__actions">
          <Button type="primary" htmlType="submit" loading={isChangingPassword}>保存新密码</Button>
          <Button onClick={() => form.resetFields()}>重置</Button>
        </div>
      </Form>
    </Card>
  )
}

function PasswordFields() {
  return (
    <div className="profile-password-fields">
      <Form.Item name="oldPassword" label="原密码" rules={[{ required: true, message: '请输入原密码' }]}>
        <Input.Password autoComplete="current-password" maxLength={64} prefix={<LockOutlined />} placeholder="请输入原密码" />
      </Form.Item>
      <Form.Item name="newPassword" label="新密码" rules={newPasswordRules}>
        <Input.Password autoComplete="new-password" maxLength={32} prefix={<KeyOutlined />} placeholder="请输入新密码" />
      </Form.Item>
      <Form.Item name="confirmPassword" label="确认新密码" dependencies={['newPassword']} rules={confirmPasswordRules}>
        <Input.Password autoComplete="new-password" maxLength={32} prefix={<KeyOutlined />} placeholder="请再次输入新密码" />
      </Form.Item>
    </div>
  )
}

function InfoRow({ icon, label, value }: { icon: ReactNode; label: string; value: string }) {
  return (
    <div className="profile-info-row">
      <span className="profile-info-row__icon">{icon}</span>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  )
}

const newPasswordRules = [
  ...strongPasswordRules('请输入新密码'),
]

const confirmPasswordRules = [
  { required: true, message: '请再次输入新密码' },
  ({ getFieldValue }: { getFieldValue: (name: string) => string }) => ({
    validator(_: unknown, value: string) {
      if (!value || getFieldValue('newPassword') === value) {
        return Promise.resolve()
      }
      return Promise.reject(new Error('两次输入的新密码不一致'))
    },
  }),
]
