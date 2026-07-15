import { Button, Card, Form, Skeleton, Space, message } from 'antd'
import { useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import MesPageHeader from '../../components/layout/MesPageHeader'
import { useCreateUser, useUpdateUser } from '../../features/user/hooks/useUserMutations'
import { useUserDetail } from '../../features/user/hooks/useUserDetail'
import { useUnsavedChangesGuard } from '../../hooks/useUnsavedChangesGuard'
import type { SystemUser, UserSaveDTO } from '../../types/user'
import RolePermissionPreview from './RolePermissionPreview'
import UserProfileForm from './UserProfileForm'
import '../documentModule.css'
import './UserProfile.css'

interface Props {
  mode: 'create' | 'edit'
}

export default function UserFormPage({ mode }: Props) {
  const [form] = Form.useForm<UserSaveDTO>()
  const navigate = useNavigate()
  const { uuid } = useParams()
  const isEdit = mode === 'edit'
  const { data: user, isLoading: isLoadingUser } = useUserDetail(isEdit ? uuid : undefined)
  const { mutateAsync: createSystemUser, isPending: isCreatingUser } = useCreateUser()
  const { mutateAsync: updateSystemUser, isPending: isUpdatingUser } = useUpdateUser()
  const roleCode = Form.useWatch('roleCode', form)
  const { clearDirty, markDirty } = useUnsavedChangesGuard()

  useEffect(() => {
    if (user) form.setFieldsValue(toFormValues(user))
  }, [form, user])

  const submit = async (values: UserSaveDTO) => {
    const savedUuid = isEdit && uuid ? uuid : await createSystemUser(values)
    if (isEdit && uuid) await updateSystemUser({ uuid, data: values })
    clearDirty()
    message.success(isEdit ? '用户信息已保存' : '用户已新增')
    navigate(`/users/${savedUuid}`)
  }

  const backPath = isEdit && uuid ? `/users/${uuid}` : '/users'

  return (
    <div className="document-module-page user-profile-page">
      <MesPageHeader
        title={isEdit ? '编辑用户权限' : '新增系统用户'}
        onBack={() => navigate(backPath)}
        actions={(
          <Space>
            <Button onClick={() => navigate(backPath)}>取消</Button>
            <Button type="primary" loading={isCreatingUser || isUpdatingUser} onClick={() => form.submit()}>
              保存用户
            </Button>
          </Space>
        )}
      />

      <div className="user-form-layout">
        <Card className="document-module-card user-profile-card" title="用户资料">
          {isLoadingUser ? (
            <Skeleton active paragraph={{ rows: 6 }} />
          ) : (
            <UserProfileForm editing={isEdit} form={form} onFinish={submit} onValuesChange={markDirty} />
          )}
        </Card>
        <RolePermissionPreview compact roleCode={roleCode} />
      </div>
    </div>
  )
}

function toFormValues(user: SystemUser): UserSaveDTO {
  return {
    username: user.username,
    realName: user.realName,
    roleCode: user.roleCode,
    status: user.status,
    remark: user.remark,
  }
}
